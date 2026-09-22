package ir.kamranvahdati.lawoffice;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import android.app.Instrumentation;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

/** A real 9.1 preview APK exports the encrypted bundle; a separate v10 package imports it. */
@RunWith(AndroidJUnit4.class)
public class CrossPackageBackupTransferTest {
    private static final String CODE = "CI-only-transfer-code-1405";
    private static final String CLIENT = "موکل انتقال آزمایشی";
    private static final String CASE = "پرونده انتقال میان دو بسته";
    private static final String DEADLINE = "مهلت انتقال آزمایشی";
    private static final byte[] PDF = "%PDF-1.4\nVOKANO-91-MEDIA\n%%EOF".getBytes(StandardCharsets.UTF_8);

    @Test public void exportFromActualVersion91Preview() throws Exception {
        assumeTrue("Run only with the installed v9.1 preview APK",
                "export_preview".equals(InstrumentationRegistry.getArguments().getString("transfer_mode")));
        Instrumentation instrument=InstrumentationRegistry.getInstrumentation();
        Context context=instrument.getTargetContext();
        assertTrue(context.getPackageName().endsWith(".preview"));
        context.deleteDatabase(OfficeDb.ENCRYPTED_NAME);
        SharedPreferences prefs=context.getSharedPreferences("office_profile",Context.MODE_PRIVATE);
        prefs.edit().putString("name","وکیل انتقال آزمایشی")
                .putString("professional_body","کانون وکلای دادگستری")
                .putString("province","تهران").putString("city","تهران")
                .putString("national_id","0013540831").putString("phone","09120000001")
                .putBoolean("lock_enabled",false).putBoolean("profile_complete",true).commit();
        OfficeDb db=new OfficeDb(context);
        try {
            db.clearAllOfficeData();
            long client=db.addClient(CLIENT,"0013540831","پدر","1360/01/01","09120000001","تهران","فقط آزمون انتقال");
            OfficeDb.CaseRecord record=new OfficeDb.CaseRecord();
            record.title=CASE;record.clientId=client;record.category="حقوقی";record.status="active";
            long caseId=db.addCase(record);
            File oldMedia=new File(context.getFilesDir(),"case-91.pdf");
            try(FileOutputStream out=new FileOutputStream(oldMedia)){out.write(PDF);}
            db.addCaseAttachment(caseId,"case-91.pdf","application/pdf",Uri.fromFile(oldMedia).toString());
            String today=JalaliDate.today().value(),due=JalaliDate.addDays(today,3);
            db.saveDeadline(caseId,client,DEADLINE,today,due,3,"داده ساختگی");
            assertEquals(1,db.countClients());
            assertEquals(1,db.deadlines(caseId,false).size());
        } finally {db.close();}
        MainActivity activity=start(instrument,context);
        try {
            String bundle=activity.exportBundle();
            byte[] encrypted=SecureBackup.encrypt(bundle,CODE);
            String encoded=Base64.encodeToString(encrypted,Base64.NO_WRAP);
            Bundle status=new Bundle();status.putString("klo_transfer_base64",encoded);
            instrument.sendStatus(0,status);
        } finally {finish(instrument,activity);}
    }

    @Test public void restoreVersion91PreviewBundleIntoVersion10Base() throws Exception {
        String encoded=InstrumentationRegistry.getArguments().getString("backup_payload","");
        assumeTrue("Run with a bundle exported by the v9.1 preview APK",!encoded.isEmpty());
        Instrumentation instrument=InstrumentationRegistry.getInstrumentation();
        Context context=instrument.getTargetContext();
        assertEquals("ir.kamranvahdati.lawoffice",context.getPackageName());
        byte[] encrypted=Base64.decode(encoded,Base64.NO_WRAP);
        try {SecureBackup.decrypt(encrypted,"wrong-code");fail("Wrong backup code was accepted");}
        catch(javax.crypto.AEADBadTagException expected) { /* Authentication must fail before import. */ }
        context.getSharedPreferences("office_profile",Context.MODE_PRIVATE).edit()
                .putBoolean("lock_enabled",false).putBoolean("profile_complete",true).commit();
        MainActivity activity=start(instrument,context);
        try {
            activity.importBundle(SecureBackup.decrypt(encrypted,CODE));
            assertEquals(13,activity.db.getReadableDatabase().getVersion());
            assertEquals(1,activity.db.countClients());
            assertEquals(CLIENT,activity.db.clients().get(0).name);
            OfficeDb.CaseRecord restored=activity.db.cases(null,"همه",null).get(0);
            assertEquals(CASE,restored.title);
            assertEquals(1,activity.db.deadlines(restored.id,false).size());
            assertEquals(DEADLINE,activity.db.deadlines(restored.id,false).get(0).title);
            OfficeDb.CaseAttachmentRecord attachment=activity.db.caseAttachments(restored.id).get(0);
            assertEquals("case-91.pdf",attachment.name);
            // The old backup contains only a URI owned by the old app, never the media bytes.
            assertFalse("Old package-private file unexpectedly became available to the new UID",
                    canRead(context,Uri.parse(attachment.uri)));
            // Simulate the user granting the same original document again via the system picker.
            File selected=new File(context.getCacheDir(),"selected-case-91.pdf");
            try(FileOutputStream out=new FileOutputStream(selected)){out.write(PDF);}
            Field caseField=MainActivity.class.getDeclaredField("pendingAttachmentCaseId");
            Field idField=MainActivity.class.getDeclaredField("pendingAttachmentId");
            caseField.setAccessible(true);idField.setAccessible(true);
            caseField.set(activity,restored.id);idField.set(activity,attachment.id);
            instrument.runOnMainSync(()->activity.onActivityResult(75,Activity.RESULT_OK,
                    new Intent().setData(Uri.fromFile(selected))));
            instrument.waitForIdleSync();
            Uri stored=Uri.parse(activity.db.caseAttachments(restored.id).get(0).uri);
            assertTrue(selected.delete());
            byte[] result=new byte[PDF.length];
            try(InputStream in=context.getContentResolver().openInputStream(stored)){
                assertNotNull(in);int n=0,k;while(n<result.length&&(k=in.read(result,n,result.length-n))!=-1)n+=k;
                assertEquals(PDF.length,n);assertEquals(-1,in.read());
            }
            assertArrayEquals("The private copy must remain open after the selected source disappears",PDF,result);
            assertEquals(stored.toString(),activity.db.caseAttachments(restored.id).get(0).uri);
            assertEquals("وکیل انتقال آزمایشی",activity.prefs.getString("name",""));
            assertTrue("The restored lawyer profile must be usable",activity.profileReady());
        } finally {finish(instrument,activity);}
    }

    private static boolean canRead(Context context,Uri uri) {
        try(InputStream in=context.getContentResolver().openInputStream(uri)){return in!=null&&in.read()!=-1;}
        catch(Exception e){return false;}
    }

    private static MainActivity start(Instrumentation instrument,Context context) {
        MainActivity activity=(MainActivity)instrument.startActivitySync(
                new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        instrument.waitForIdleSync();return activity;
    }

    private static void finish(Instrumentation instrument,MainActivity activity) {
        instrument.runOnMainSync(activity::finish);instrument.waitForIdleSync();
    }
}
