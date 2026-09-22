package ir.kamranvahdati.lawoffice;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;

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
            assertEquals("وکیل انتقال آزمایشی",activity.prefs.getString("name",""));
            assertTrue("The restored lawyer profile must be usable",activity.profileReady());
        } finally {finish(instrument,activity);}
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
