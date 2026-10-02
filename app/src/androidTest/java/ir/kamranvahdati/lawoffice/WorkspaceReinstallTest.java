package ir.kamranvahdati.lawoffice;
import static org.junit.Assert.*;
import android.app.Instrumentation;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;
import org.json.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicReference;

/** Separate provider APK retains synthetic user-owned documents across TARGET-ONLY uninstall. */
@RunWith(AndroidJUnit4.class)
public class WorkspaceReinstallTest {
 private static final String PASSWORD="isolated-reinstall-password";
 private static final String CONTENT="synthetic uninstall fixture";
 @Test public void externalDocumentsSurviveReinstallAndLightRestore()throws Exception{
  Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();Context context=instrumentation.getTargetContext();
  String phase=InstrumentationRegistry.getArguments().getString("upgrade_phase","");Assume.assumeTrue("seed".equals(phase)||"verify".equals(phase));
  assertFalse("Must be isolated release fixture",BuildConfig.DEBUG);
  if("verify".equals(phase)){
   assertFalse(new VokanoWorkspace(context).configured());assertTrue(context.getContentResolver().getPersistedUriPermissions().isEmpty());
   try(OfficeDb db=new OfficeDb(context)){assertEquals(0,db.countPersons());}
  }
  Intent grant=new Intent().setComponent(new ComponentName(instrumentation.getContext().getPackageName(),WorkspaceGrantActivity.class.getName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  context.startActivity(grant);Uri tree=DocumentsContract.buildTreeDocumentUri(WorkspaceTestProvider.AUTHORITY,"root");VokanoWorkspace workspace=new VokanoWorkspace(context);
  Exception pending=null;for(int attempt=0;attempt<50;attempt++){try{workspace.select(tree,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);pending=null;break;}catch(SecurityException awaiting){pending=awaiting;Thread.sleep(100);}}
  if(pending!=null)throw pending;assertFalse(context.getContentResolver().getPersistedUriPermissions().isEmpty());
  if("seed".equals(phase)){
   try(OfficeDb db=new OfficeDb(context)){
    db.getWritableDatabase().execSQL("INSERT INTO clients(name,uid,is_client) VALUES('Synthetic reinstall person','reinstall-person-102',1)");
    workspace.migrateAttachments(db);workspace.migrateProfileMedia();
    Uri source=DocumentsContract.createDocument(context.getContentResolver(),workspace.resolve("Exports",true),"application/octet-stream","reinstall-fixture");
    try(OutputStream out=context.getContentResolver().openOutputStream(source)){out.write(CONTENT.getBytes("UTF-8"));}
    Uri portable=workspace.copy(source,"Exports/Profile");
    JSONObject profile=new JSONObject().put("name","آزمون بازیابی نصب مجدد").put("photo",portable.toString()).put("theme_id","dark");
    JSONObject root=new JSONObject().put("database",new JSONObject(db.exportJson())).put("profile",profile);
    File complete=File.createTempFile("reinstall-",".vkb",context.getCacheDir());try{
     try(OutputStream out=new FileOutputStream(complete)){FullBackup.write(context,root.toString(),out,PASSWORD);}
     assertFalse(WorkspaceBackups.save(context,complete,PASSWORD,WorkspaceBackups.today()));
    }finally{complete.delete();}
   }
  }else{
   Uri backup=null;Uri folder=workspace.resolve("Backup",false);
   try(Cursor c=context.getContentResolver().query(DocumentsContract.buildChildDocumentsUriUsingTree(folder,DocumentsContract.getDocumentId(folder)),new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){
    while(c.moveToNext())if(c.getString(1).endsWith(".vkb")){assertNull("Expected one final snapshot",backup);backup=DocumentsContract.buildDocumentUriUsingTree(folder,c.getString(0));}
   }
   assertNotNull(backup);
   try(InputStream in=context.getContentResolver().openInputStream(backup);FullBackup.Prepared restored=FullBackup.read(context,in,PASSWORD)){
    JSONObject expected=new JSONObject(restored.bundle);MainActivity activity=(MainActivity)instrumentation.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    AtomicReference<Exception> error=new AtomicReference<>();instrumentation.runOnMainSync(()->{try{activity.importBundle(restored.bundle);}catch(Exception e){error.set(e);}});
    if(error.get()!=null)throw error.get();restored.commit();instrumentation.runOnMainSync(activity::finish);
    try(OfficeDb db=new OfficeDb(context)){assertEquals(1,db.countPersons());assertEquals(expected.getJSONObject("database").getJSONObject("tables").toString(),new JSONObject(db.exportJson()).getJSONObject("tables").toString());}
    assertEquals("آزمون بازیابی نصب مجدد",context.getSharedPreferences("office_profile",0).getString("name",""));
    Uri photo=Uri.parse(context.getSharedPreferences("office_profile",0).getString("photo",""));
    try(InputStream media=context.getContentResolver().openInputStream(workspace.resolveReference(photo));ByteArrayOutputStream out=new ByteArrayOutputStream()){int n;while((n=media.read())!=-1)out.write(n);assertEquals(CONTENT,out.toString("UTF-8"));}
   }
  }
 }
}
