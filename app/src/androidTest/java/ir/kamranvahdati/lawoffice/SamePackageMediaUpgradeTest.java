package ir.kamranvahdati.lawoffice;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import android.content.Context;
import android.net.Uri;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Installed v9.1 and v10 share a package only when the APK signatures match. */
public class SamePackageMediaUpgradeTest {
    private static final byte[] BYTES="%PDF-1.4\nupgrade-preserves-bytes\n%%EOF".getBytes(StandardCharsets.UTF_8);
    @Test public void checkAttachmentAndProfileImageAcrossSamePackageUpgrade() throws Exception {
        String mode=InstrumentationRegistry.getArguments().getString("media_upgrade","");
        assumeTrue("seed".equals(mode)||"verify".equals(mode));
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        File file=new File(context.getFilesDir(),"v91-upgrade-document.pdf");
        if("seed".equals(mode)){
            try(FileOutputStream out=new FileOutputStream(file)){out.write(BYTES);}
            OfficeDb db=new OfficeDb(context);
            try{
                long client=db.addClient("موکل آزمون رسانه","0013540831","پدر","1360/01/01","09120000001","تهران","fixture");
                OfficeDb.CaseRecord record=new OfficeDb.CaseRecord();record.title="پرونده آزمون رسانه";record.clientId=client;
                record.category="حقوقی";record.status="active";
                db.addCaseAttachment(db.addCase(record),"v91-upgrade-document.pdf","application/pdf",Uri.fromFile(file).toString());
                context.getSharedPreferences("office_profile",Context.MODE_PRIVATE).edit().putString("photo",Uri.fromFile(file).toString()).commit();
            }finally{db.close();}
        }else{
            OfficeDb db=new OfficeDb(context);
            try{
                assertEquals(13,db.getReadableDatabase().getVersion());
                long caseId=-1;for(OfficeDb.CaseRecord record:db.cases(null,"همه",null))if("پرونده آزمون رسانه".equals(record.title))caseId=record.id;
                assertTrue("Case was lost",caseId>0);
                OfficeDb.CaseAttachmentRecord attachment=db.caseAttachments(caseId).get(0);
                assertEquals("v91-upgrade-document.pdf",attachment.name);
                assertBytes(context,Uri.parse(attachment.uri));
                assertBytes(context,Uri.parse(context.getSharedPreferences("office_profile",Context.MODE_PRIVATE).getString("photo","")));
                assertTrue("Original document was removed",file.isFile());
            }finally{db.close();}
        }
    }
    private static void assertBytes(Context context,Uri uri) throws Exception {
        try(InputStream in=context.getContentResolver().openInputStream(uri)){
            assertNotNull(in);byte[] data=new byte[BYTES.length];int at=0,k;
            while(at<data.length&&(k=in.read(data,at,data.length-at))!=-1)at+=k;
            assertEquals(BYTES.length,at);assertArrayEquals(BYTES,data);assertEquals(-1,in.read());
        }
    }
}
