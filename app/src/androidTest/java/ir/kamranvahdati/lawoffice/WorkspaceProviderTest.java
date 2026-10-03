package ir.kamranvahdati.lawoffice;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;
import org.json.*;
import java.io.*;
import java.util.*;

@RunWith(AndroidJUnit4.class)
public class WorkspaceProviderTest {
    private Context context;private String previousTree;
    @Before public void prepare()throws Exception{
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        previousTree=context.getSharedPreferences("workspace",0).getString("tree",null);
        // The test APK owns a protected DocumentsProvider. Obtain a real persisted tree grant.
        Intent grant=new Intent().setComponent(new ComponentName(
                InstrumentationRegistry.getInstrumentation().getContext().getPackageName(),
                WorkspaceGrantActivity.class.getName())).putExtra("reset_fixture",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(grant);
        Uri tree=DocumentsContract.buildTreeDocumentUri(WorkspaceTestProvider.AUTHORITY,"root");
        SecurityException pending=null;
        for(int attempt=0;attempt<50;attempt++){
            try{context.getContentResolver().takePersistableUriPermission(tree,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);pending=null;break;}
            catch(SecurityException awaiting){pending=awaiting;Thread.sleep(100);}
        }
        if(pending!=null)throw pending;
        new VokanoWorkspace(context).select(tree,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }
    @After public void cleanup()throws Exception{
        if(context==null)return;
        context.getSharedPreferences("workspace",0).edit().putString("tree",previousTree).commit();
    }
    // Reset occurs inside the test-provider APK before issuing the tree grant.
    // Deleting the granted root through DocumentsContract revokes that grant by design.
    private JSONObject bundle()throws Exception{try(OfficeDb db=new OfficeDb(context)){JSONObject data=new JSONObject(db.exportJson());data.getJSONObject("tables").put("case_attachments",new JSONArray());return new JSONObject().put("database",data).put("profile",new JSONObject());}}
    private File backup(JSONObject json)throws Exception{File f=File.createTempFile("fixture-",".vkb",context.getCacheDir());try(OutputStream out=new FileOutputStream(f)){FullBackup.write(context,json.toString(),out,"fixture-password");}return f;}
    private List<String> names()throws Exception{
        Uri root=new VokanoWorkspace(context).resolve("Backup",true);List<String> names=new ArrayList<>();
        try(Cursor c=context.getContentResolver().query(DocumentsContract.buildChildDocumentsUriUsingTree(root,DocumentsContract.getDocumentId(root)),new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){while(c.moveToNext())names.add(c.getString(0));}return names;
    }
    @Test public void replacementRetentionAndNoDeleteProvider()throws Exception{
        File f=backup(bundle());try{
            assertFalse(WorkspaceBackups.save(context,f,"fixture-password","2026-09-02"));
            assertFalse(WorkspaceBackups.save(context,f,"fixture-password","2026-09-03"));
            assertFalse(WorkspaceBackups.save(context,f,"fixture-password","2026-10-02"));
            assertEquals(2,names().size());List<String> before=names();
            assertFalse(WorkspaceBackups.save(context,f,"fixture-password","2026-10-02"));assertEquals(2,names().size());assertFalse(before.equals(names()));
            Uri root=new VokanoWorkspace(context).root();DocumentsContract.createDocument(context.getContentResolver(),root,"application/octet-stream","deny-delete");
            assertTrue(WorkspaceBackups.save(context,f,"fixture-password","2026-10-02"));assertEquals(3,names().size());
        }finally{f.delete();}
    }
    @Test public void invalidRelationsDoNotDeleteLastGoodOrMutateLiveDb()throws Exception{
        File good=backup(bundle());File invalid=null;String before;
        try(OfficeDb db=new OfficeDb(context)){before=db.exportJson();}
        try{
            WorkspaceBackups.save(context,good,"fixture-password","2026-10-02");List<String> saved=names();
            JSONObject broken=bundle();broken.getJSONObject("database").getJSONObject("tables").getJSONArray("ledger").put(new JSONObject().put("id",999999).put("case_id",999999).put("kind","income").put("category","fixture"));
            invalid=backup(broken);boolean failed=false;try{WorkspaceBackups.save(context,invalid,"fixture-password","2026-10-02");}catch(Exception expected){failed=true;}
            assertTrue(failed);assertEquals(saved,names());
            try(OfficeDb db=new OfficeDb(context)){assertEquals(new JSONObject(before).getJSONObject("tables").toString(),new JSONObject(db.exportJson()).getJSONObject("tables").toString());}
        }finally{good.delete();if(invalid!=null)invalid.delete();}
    }
    @Test public void copiedMediaRelinksAndBackupContainsOnlyMetadata()throws Exception{
        VokanoWorkspace workspace=new VokanoWorkspace(context);workspace.ensureCase("fixture-case");
        Uri original=DocumentsContract.createDocument(context.getContentResolver(),workspace.resolve("Exports",true),"application/octet-stream","fixture-source");
        byte[] content="synthetic attachment fixture".getBytes("UTF-8");
        try(OutputStream out=context.getContentResolver().openOutputStream(original)){out.write(content);}
        Uri portable=workspace.copy(original,"Cases/fixture-case");
        String tree=context.getSharedPreferences("workspace",0).getString("tree","");context.getSharedPreferences("workspace",0).edit().remove("tree").commit();
        boolean detached=false;try{workspace.resolveReference(portable);}catch(IOException expected){detached=true;}assertTrue(detached);
        context.getSharedPreferences("workspace",0).edit().putString("tree",tree).commit();
        try(InputStream in=context.getContentResolver().openInputStream(workspace.resolveReference(portable))){ByteArrayOutputStream bytes=new ByteArrayOutputStream();int n;while((n=in.read())!=-1)bytes.write(n);assertArrayEquals(content,bytes.toByteArray());}
        JSONObject json=bundle();json.getJSONObject("profile").put("photo",portable.toString());File complete=backup(json);File zip=File.createTempFile("inspect-",".zip",context.getCacheDir());
        try{
            try(InputStream in=new FileInputStream(complete)){BackupCipher.decrypt(in,zip,"fixture-password");}
            try(java.util.zip.ZipFile archive=new java.util.zip.ZipFile(zip)){assertEquals(1,archive.size());assertNotNull(archive.getEntry("bundle.json"));}
        }finally{complete.delete();zip.delete();}
    }
    @Test public void automaticBackupStoresOneEncryptedSnapshotPerDay()throws Exception{
        AutoBackup.disable(context);
        try{
            VokanoWorkspace workspace=new VokanoWorkspace(context);
            try(OfficeDb db=new OfficeDb(context)){workspace.migrateAttachments(db);}
            workspace.migrateProfileMedia();
            AutoBackup.enable(context,"automatic-fixture-password");
            long until=android.os.SystemClock.uptimeMillis()+60000;
            while(!WorkspaceBackups.today().equals(context.getSharedPreferences("vokano_auto_backup",0).getString("last_day",""))&&android.os.SystemClock.uptimeMillis()<until)Thread.sleep(100);
            assertEquals(AutoBackup.status(context),WorkspaceBackups.today(),context.getSharedPreferences("vokano_auto_backup",0).getString("last_day",""));
            List<String> saved=names();assertEquals(1,saved.size());
            assertNotEquals("automatic-fixture-password",context.getSharedPreferences("vokano_auto_backup",0).getString("credential",""));
            java.util.concurrent.CountDownLatch finished=new java.util.concurrent.CountDownLatch(1);
            AutoBackup.start(context,finished::countDown);assertTrue(finished.await(5,java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(saved,names());
        }finally{AutoBackup.disable(context);}
    }
    @Test public void referencesRejectForeignAuthorityAndTraversalAndMissingIsExplicit()throws Exception{
        VokanoWorkspace workspace=new VokanoWorkspace(context);Uri valid=workspace.reference("Cases/fixture/file");assertTrue(VokanoWorkspace.isReference(valid));
        assertFalse(VokanoWorkspace.isReference(valid.buildUpon().authority("attacker.media").build()));
        assertFalse(VokanoWorkspace.isReference(valid.buildUpon().appendQueryParameter("x","y").build()));
        Uri traversal=new Uri.Builder().scheme("content").authority(BuildConfig.APPLICATION_ID+".media").appendPath("workspace").appendPath("Cases/../file").build();assertFalse(VokanoWorkspace.isReference(traversal));
        boolean missing=false;try{workspace.resolveReference(valid);}catch(FileNotFoundException expected){missing=true;}assertTrue(missing);
    }
}
