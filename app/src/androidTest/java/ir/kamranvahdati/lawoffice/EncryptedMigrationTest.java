package ir.kamranvahdati.lawoffice;

import static org.junit.Assert.*;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileInputStream;
import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
public class EncryptedMigrationTest {
    @Test public void existingRowsSurviveEncryptionAndBackupRollback() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(OfficeDb.ENCRYPTED_NAME);
        context.deleteDatabase(OfficeDb.LEGACY_NAME);
        context.getSharedPreferences("database_secret",Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences("database_migration",Context.MODE_PRIVATE).edit().clear().commit();
        try(SQLiteDatabase old=SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(OfficeDb.LEGACY_NAME),null)) {
            old.execSQL("CREATE TABLE clients(id INTEGER PRIMARY KEY,name TEXT NOT NULL,created_at TEXT)");
            old.execSQL("CREATE TABLE cases(id INTEGER PRIMARY KEY,title TEXT NOT NULL,client_id INTEGER,fee_agreed INTEGER,created_at TEXT)");
            old.execSQL("CREATE TABLE tasks(id INTEGER PRIMARY KEY,title TEXT NOT NULL,priority TEXT NOT NULL,due_date TEXT,case_name TEXT)");
            old.execSQL("CREATE TABLE ledger(id INTEGER PRIMARY KEY,case_id INTEGER,kind TEXT NOT NULL,category TEXT NOT NULL,amount INTEGER NOT NULL,paid_by TEXT)");
            old.execSQL("CREATE TABLE worklogs(id INTEGER PRIMARY KEY,case_id INTEGER,action_type TEXT NOT NULL)");
            old.execSQL("INSERT INTO clients VALUES(1,'موکل فرضی','2026-01-01')");
            old.execSQL("INSERT INTO cases VALUES(1,'پرونده فرضی',1,1000,'2026-01-01')");
            old.execSQL("INSERT INTO tasks VALUES(1,'اقدام فرضی','فوری','1405/06/24','پرونده فرضی')");
            old.execSQL("INSERT INTO ledger VALUES(1,1,'expense','تمبر',100,'وکیل')");
        }
        OfficeDb db=new OfficeDb(context);
        LegacyMigration.migrate(context,db);
        assertFalse(context.getDatabasePath(OfficeDb.LEGACY_NAME).exists());
        assertEquals(1,db.countClients());
        assertEquals(1,db.cases(null,"همه",null).size());
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM clients WHERE uid IS NULL",null)){assertTrue(c.moveToFirst());assertEquals(0,c.getInt(0));}
        byte[] header=new byte[16];try(FileInputStream in=new FileInputStream(context.getDatabasePath(OfficeDb.ENCRYPTED_NAME))){assertEquals(16,in.read(header));}
        assertFalse(Arrays.equals("SQLite format 3\u0000".getBytes("UTF-8"),header));
        long installment=db.addInstallment(1,"قسط نمونه",1000,"1405/07/01");
        db.payInstallment(installment,500,"1405/06/25","موکل","رسید آزمایشی");
        assertEquals(500,db.installments(1).get(0).paid);
        JSONObject missing=new JSONObject(db.exportJson());missing.getJSONObject("tables").remove("clients");
        try{db.importJson(missing.toString());fail("Incomplete backup must be rejected");}catch(Exception expected){assertEquals(1,db.countClients());}
        db.importJson(db.exportJson());
        assertEquals(500,db.installments(1).get(0).paid);
        db.close();
        OfficeDb reopened=new OfficeDb(context);assertEquals(1,reopened.countClients());assertEquals(500,reopened.installments(1).get(0).paid);reopened.close();
    }
}
