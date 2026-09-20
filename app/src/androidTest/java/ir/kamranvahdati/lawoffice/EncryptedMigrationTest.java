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
import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public class EncryptedMigrationTest {
    @Test public void version10DatabaseUpgradesAdditivelyTo11() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(OfficeDb.ENCRYPTED_NAME);
        context.deleteDatabase(OfficeDb.LEGACY_NAME);
        context.getSharedPreferences("database_secret",Context.MODE_PRIVATE).edit().clear().commit();
        OfficeDb previous=new OfficeDb(context);previous.getWritableDatabase();
        long realClient=previous.addClient("موکل ارتقا","0013540831","پدر","1360/01/01","09128402768","تهران","داده واقعی آزمون ارتقا");
        OfficeDb.CaseRecord realCase=new OfficeDb.CaseRecord();realCase.title="پرونده حفظ‌شونده";realCase.clientId=realClient;realCase.category="حقوقی";realCase.status="active";long realCaseId=previous.addCase(realCase);
        previous.getWritableDatabase().execSQL("DROP TABLE case_attachments");
        previous.getWritableDatabase().execSQL("ALTER TABLE cases DROP COLUMN judgment_number");
        previous.getWritableDatabase().execSQL("ALTER TABLE cases DROP COLUMN order_number");
        previous.getWritableDatabase().setVersion(10);previous.close();
        OfficeDb upgraded=new OfficeDb(context);assertEquals(13,upgraded.getReadableDatabase().getVersion());
        boolean judgment=false,order=false;try(Cursor c=upgraded.getReadableDatabase().rawQuery("PRAGMA table_info(cases)",null)){while(c.moveToNext()){judgment|="judgment_number".equals(c.getString(1));order|="order_number".equals(c.getString(1));}}
        assertTrue(judgment);assertTrue(order);assertEquals(1,upgraded.cases(null,"همه",realClient).size());assertEquals(realCaseId,upgraded.cases(null,"همه",realClient).get(0).id);
        assertTrue(upgraded.addCaseAttachment(realCaseId,"نمونه.pdf","application/pdf","content://test/upgrade")>0);upgraded.close();
    }

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
        assertEquals(1,db.caseClients(1).size());
        try(Cursor c=db.getReadableDatabase().rawQuery("PRAGMA user_version",null)){assertTrue(c.moveToFirst());assertEquals(13,c.getInt(0));}
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM clients WHERE uid IS NULL",null)){assertTrue(c.moveToFirst());assertEquals(0,c.getInt(0));}
        byte[] header=new byte[16];try(FileInputStream in=new FileInputStream(context.getDatabasePath(OfficeDb.ENCRYPTED_NAME))){assertEquals(16,in.read(header));}
        assertFalse(Arrays.equals("SQLite format 3\u0000".getBytes("UTF-8"),header));
        long installment=db.addInstallment(1,"قسط نمونه",1000,"1405/07/01");
        db.payInstallment(installment,500,"1405/06/25","موکل","رسید آزمایشی");
        assertEquals(500,db.installments(1).get(0).paid);
        long deadline=db.addDeadline(1,"مهلت نمونه","1405/06/24","1405/07/25",31,"فقط آزمایش");
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM reminders WHERE target_type='deadline' AND target_id=?",new String[]{String.valueOf(deadline)})){assertTrue(c.moveToFirst());assertEquals(5,c.getInt(0));}
        JSONObject missing=new JSONObject(db.exportJson());missing.getJSONObject("tables").remove("clients");
        try{db.importJson(missing.toString());fail("Incomplete backup must be rejected");}catch(Exception expected){assertEquals(1,db.countClients());}
        long secondClient=db.addClient("موکل دوم فرضی","0013540831","پدر نمونه","1360/01/01","09120000001","تهران","فقط آزمون");
        db.addClientToCase(1,secondClient,"موکل مشترک",false);
        assertEquals(2,db.caseClients(1).size());
        long collaborator=db.addCollaborator("وکیل","همکار","12345","کانون وکلای دادگستری","09120000002","فقط آزمون");
        db.linkCollaborator(1,collaborator,"مشترک / مجتمع",25,"توافق فرضی");
        assertEquals(1,db.caseCollaborators(1).size());
        long contract=db.addRepresentationContract(1L,"E-100","1405/06/28","دفاع","تمام مراحل",1000,false,"طرف فرضی",true,"مشترک / مجتمع","آزمون",Arrays.asList(1L,secondClient),Collections.singletonList(collaborator));
        assertTrue(contract>0);assertEquals(1,db.representationContracts(1L,null).size());assertEquals(1,db.representationContracts(null,secondClient).size());
        long removableContract=db.addRepresentationContract(1L,"E-DELETE","1405/06/28","دفاع","آزمون حذف نرم",0,false,"",false,"انفرادی","آزمون",Collections.singletonList(1L),Collections.emptyList());
        assertEquals(2,db.representationContracts(1L,null).size());db.deleteRepresentationContract(removableContract);assertEquals(1,db.representationContracts(1L,null).size());
        assertTrue(db.addFinancialContract(1,"1405/06/28",1000,"دو قسط","آزمون")>0);
        assertEquals(1,db.financialContracts(1).size());
        long check=db.addPaymentCheck(1,installment,"CHK-1","1405/07/01",500,"بانک نمونه","شعبه نمونه","pending","آزمون");
        assertTrue(check>0);assertEquals(1,db.paymentChecks(1L,null).size());
        db.updatePaymentCheckStatus(check,"bounced");assertEquals("bounced",db.paymentChecks(1L,null).get(0).status);
        long removableCheck=db.addPaymentCheck(1,null,"CHK-DELETE","1405/07/02",500,"بانک","شعبه","pending","آزمون حذف نرم");
        assertEquals(2,db.paymentChecks(1L,null).size());db.deletePaymentCheck(removableCheck);assertEquals(1,db.paymentChecks(1L,null).size());
        OfficeDb.CaseRecord upgraded=db.cases(null,"همه",null).get(0);upgraded.judgmentNumber="R-1405";upgraded.orderNumber="O-1405";db.updateCase(upgraded);
        assertEquals("R-1405",db.cases(null,"همه",null).get(0).judgmentNumber);
        long attachment=db.addCaseAttachment(1,"رأی نمونه.pdf","application/pdf","content://test/ruling");
        assertTrue(attachment>0);assertEquals(1,db.caseAttachments(1).size());
        db.getWritableDatabase().execSQL("INSERT INTO tasks(uid,title,priority,status,created_at,updated_at,is_demo) VALUES('demo-v091','دمو حذف‌شونده','عادی','open','x','x',1)");
        int beforeDemo=db.countDemoRows();assertTrue(beforeDemo>0);db.softDeleteDemoData();assertEquals(0,db.countDemoRows());assertEquals(2,db.countClients());assertEquals(1,db.cases(null,"همه",null).size());
        String fullBackup=db.exportJson();
        db.importJson(fullBackup);
        assertEquals(500,db.installments(1).get(0).paid);
        assertEquals(1,db.deadlines(1L,true).size());
        assertEquals(2,db.caseClients(1).size());
        assertEquals(1,db.caseCollaborators(1).size());
        assertEquals(1,db.representationContracts(1L,null).size());
        assertEquals(1,db.financialContracts(1).size());
        assertEquals(1,db.paymentChecks(1L,null).size());
        assertEquals("bounced",db.paymentChecks(1L,null).get(0).status);
        assertEquals(1,db.caseAttachments(1).size());
        assertEquals("O-1405",db.cases(null,"همه",null).get(0).orderNumber);
        db.close();
        OfficeDb reopened=new OfficeDb(context);assertEquals(2,reopened.countClients());assertEquals(500,reopened.installments(1).get(0).paid);reopened.close();
    }
}
