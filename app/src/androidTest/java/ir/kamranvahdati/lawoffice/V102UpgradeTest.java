package ir.kamranvahdati.lawoffice;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.ContentValues;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Iterator;

/** Same-package installed upgrade fixture. The seed method also compiles on immutable V10 and V10.1. */
@RunWith(AndroidJUnit4.class)
public class V102UpgradeTest {
 @Test public void structuredDataSurvivesInstalledUpgrade() throws Exception {
  Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
  String phase=InstrumentationRegistry.getArguments().getString("upgrade_phase","");
  Assume.assumeTrue("seed".equals(phase)||"verify".equals(phase));
  File expected=new File(c.getFilesDir(),"v102-upgrade-before.json");
  try(OfficeDb db=new OfficeDb(c)){
   if("seed".equals(phase)){
    assertTrue(BuildConfig.VERSION_NAME,BuildConfig.VERSION_NAME.equals("10.0")||BuildConfig.VERSION_NAME.equals("10.1"));
    assertFalse(BuildConfig.DEBUG);
    assertEquals(0,db.countClients());
    long person=db.addClient("شخص آزمون ارتقا","0013540831","پدر","1360/01/01","09120000001","نشانی حفظ شود","یادداشت حفظ شود");
    OfficeDb.CaseRecord record=new OfficeDb.CaseRecord();record.title="پرونده حفظ داده";record.clientId=person;record.province="فارس";record.judicialCity="شهر ثبت شده";long caseId=db.addCase(record);
    long colleague=db.addAndLinkCollaborator(caseId,"وکیل","آزمون","12345","کانون","09120000002","یادداشت","مجتمعاً",20);
    db.addRepresentationContract(caseId,"UPGRADE-102",JalaliDate.today().value(),"موضوع","اختیارات",10000,false,"طرف مقابل",true,"مجتمعاً","یادداشت",Collections.singletonList(person),Collections.singletonList(colleague));
    db.addLedger(caseId,"payment","حق‌الوکاله",1200,JalaliDate.today().value(),"موکل","دریافتی");
    db.addPaymentCheck(caseId,null,"UP-CHECK-102",JalaliDate.today().value(),3000,"بانک","شعبه","pending","آزمون");
    db.addTask("اقدام آزمون ارتقا","پرونده حفظ داده",JalaliDate.today().value(),"15:00","high","یادداشت اقدام");
    db.saveAppointment("جلسه دادگاه","شخص","",person,caseId,JalaliDate.today().value(),"13:00","14:00","محل","یادداشت","1","مرجع","شهر ثبت شده");
    File fixture=new File(c.getCacheDir(),"upgrade-102.pdf");try(FileOutputStream out=new FileOutputStream(fixture)){out.write("%PDF-1.4\nVOKANO immutable upgrade attachment".getBytes(StandardCharsets.UTF_8));}
    Uri uri=MediaStorage.copy(c,Uri.fromFile(fixture));db.addCaseAttachment(caseId,"upgrade.pdf","application/pdf",uri.toString());fixture.delete();
    int schema=db.getReadableDatabase().getVersion();
    if(schema==14){
     long legal=db.addClient("شرکت آزمون ارتقا","","","","09120000003","نشانی حقوقی","شخص حقوقی");
     ContentValues change=new ContentValues();change.put("person_type","legal");change.put("registration_number","123456");
     db.getWritableDatabase().update("clients",change,"id=?",new String[]{""+legal});
     ContentValues role=row();role.put("case_id",caseId);role.put("person_id",legal);role.put("role","خوانده");db.getWritableDatabase().insertOrThrow("person_roles",null,role);
     ContentValues party=row();party.put("contract_id",1);party.put("person_id",legal);party.put("role","طرف مقابل");db.getWritableDatabase().insertOrThrow("contract_parties",null,party);
     ContentValues account=row();account.put("name","حساب آزمون ارتقا");account.put("kind","bank");long accountId=db.getWritableDatabase().insertOrThrow("financial_accounts",null,account);
     ContentValues independent=row();independent.put("kind","expense");independent.put("category","هزینه شخصی");independent.put("amount",700);independent.put("entry_date",JalaliDate.today().value());independent.put("person_id",legal);independent.put("account_id",accountId);db.getWritableDatabase().insertOrThrow("independent_ledger",null,independent);
     ContentValues ledger=new ContentValues();ledger.put("person_id",person);ledger.put("account_id",accountId);db.getWritableDatabase().update("ledger",ledger,"case_id=?",new String[]{""+caseId});
     ContentValues request=row();request.put("kind","request");request.put("province","فارس");request.put("city","شیراز");request.put("title","همکاری ساختاریافته آزمون");db.getWritableDatabase().insertOrThrow("collaboration_requests",null,request);
    }
    c.getSharedPreferences("office_profile",0).edit().putString("name","وکیل ارتقا").putString("theme_id","dark").putBoolean("profile_complete",true).commit();
    JSONObject snapshot=new JSONObject(db.exportJson());snapshot.put("attachment_hash",hash(c,uri));snapshot.put("previous_schema",schema);snapshot.put("settings",new JSONObject(c.getSharedPreferences("office_profile",0).getAll()));
    try(FileOutputStream out=new FileOutputStream(expected)){out.write(snapshot.toString().getBytes(StandardCharsets.UTF_8));}
   }else{
    assertEquals("10.2",BuildConfig.VERSION_NAME);assertTrue(expected.isFile());
    JSONObject before=new JSONObject(read(expected)),actual=new JSONObject(db.exportJson());
    JSONObject oldTables=before.getJSONObject("tables"),newTables=actual.getJSONObject("tables");
    for(Iterator<String> tables=oldTables.keys();tables.hasNext();){String table=tables.next();JSONArray oldRows=oldTables.getJSONArray(table),newRows=newTables.getJSONArray(table);
     for(int i=0;i<oldRows.length();i++){JSONObject old=oldRows.getJSONObject(i),now=null;
      for(int j=0;j<newRows.length();j++)if(newRows.getJSONObject(j).getLong("id")==old.getLong("id"))now=newRows.getJSONObject(j);
      assertNotNull(table+" id "+old.getLong("id"),now);
      for(Iterator<String> keys=old.keys();keys.hasNext();){String key=keys.next();assertEquals(table+"."+key,String.valueOf(old.get(key)),String.valueOf(now.get(key)));}
     }
    }
    JSONArray attachments=newTables.getJSONArray("case_attachments");assertEquals(1,attachments.length());
    assertEquals(before.getString("attachment_hash"),hash(c,Uri.parse(attachments.getJSONObject(0).getString("content_uri"))));
    JSONObject settings=before.getJSONObject("settings"),current=new JSONObject(c.getSharedPreferences("office_profile",0).getAll());
    for(Iterator<String> keys=settings.keys();keys.hasNext();){String key=keys.next();assertEquals("setting "+key,String.valueOf(settings.get(key)),String.valueOf(current.get(key)));}
    assertEquals(0,db.countDemoRows());assertEquals(15,db.getReadableDatabase().getVersion());
    assertTrue(before.getInt("previous_schema")==13||before.getInt("previous_schema")==14);
   }
  }
 }
 private static ContentValues row(){ContentValues value=new ContentValues();value.put("uid",java.util.UUID.randomUUID().toString());String now=Long.toString(System.currentTimeMillis());value.put("created_at",now);value.put("updated_at",now);return value;}
 private static String read(File file)throws Exception{try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toString("UTF-8");}}
 private static String hash(Context c,Uri uri)throws Exception{MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream in=c.getContentResolver().openInputStream(uri)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)digest.update(b,0,n);}return android.util.Base64.encodeToString(digest.digest(),android.util.Base64.NO_WRAP);}
}
