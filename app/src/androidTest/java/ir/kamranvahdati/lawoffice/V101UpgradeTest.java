package ir.kamranvahdati.lawoffice;

import static org.junit.Assert.*;
import android.content.*;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Runs once against baseline APK, then again after adb install -r of 10.1. Never resets application data. */
@RunWith(AndroidJUnit4.class)
public class V101UpgradeTest {
 @Test public void preservesBaselineAcrossPackageUpgrade()throws Exception{
  Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
  String phase=InstrumentationRegistry.getArguments().getString("upgrade_phase","");
  Assume.assumeTrue(phase.equals("seed")||phase.equals("verify"));
  assertFalse(BuildConfig.DEBUG);
  try(OfficeDb db=new OfficeDb(c)){
   File expected=new File(c.getFilesDir(),"v101-upgrade-before.json");
   if(phase.equals("seed")){
    assertEquals("10.0",BuildConfig.VERSION_NAME);
    assertEquals(0,db.countClients());
    long person=db.addClient("شخص آزمون ارتقا","0013540831","پدر","1360/01/01","09120000001","نشانی قدیمی","نام قدیمی نباید بازنویسی شود");
    OfficeDb.CaseRecord record=new OfficeDb.CaseRecord();record.title="پرونده حفظ داده";record.clientId=person;record.province="فارس";record.judicialCity="شهر قدیمی خارج از فهرست";long caseId=db.addCase(record);
    long colleague=db.addAndLinkCollaborator(caseId,"وکیل","آزمون","12345","کانون","09120000002","یادداشت","مجتمعاً",20);
    db.addRepresentationContract(caseId,"UPGRADE-101",JalaliDate.today().value(),"موضوع","اختیارات",10000,false,"طرف مقابل قدیمی",true,"مجتمعاً","یادداشت",Collections.singletonList(person),Collections.singletonList(colleague));
    db.addLedger(caseId,"payment","حق‌الوکاله",1200,JalaliDate.today().value(),"موکل","دریافتی قدیمی");
    db.addPaymentCheck(caseId,null,"UP-CHECK",JalaliDate.today().value(),3000,"بانک","شعبه","pending","آزمون");
    db.saveAppointment("جلسه دادگاه","شخص","",person,caseId,JalaliDate.today().value(),"13:00","14:00","محل","یادداشت","1","مرجع","شهر قدیمی");
    File file=new File(c.getCacheDir(),"upgrade-fixture.pdf");try(FileOutputStream out=new FileOutputStream(file)){out.write("%PDF-1.4\nVOKANO immutable upgrade attachment".getBytes(StandardCharsets.UTF_8));}
    Uri uri=MediaStorage.copy(c,Uri.fromFile(file));db.addCaseAttachment(caseId,"upgrade.pdf","application/pdf",uri.toString());file.delete();
    c.getSharedPreferences("office_profile",0).edit().putString("name","وکیل ارتقا").putString("theme_id","dark").commit();
    JSONObject snapshot=new JSONObject(db.exportJson());snapshot.put("attachment_hash",hash(c,uri));
    try(FileOutputStream out=new FileOutputStream(expected)){out.write(snapshot.toString().getBytes(StandardCharsets.UTF_8));}
   }else{
    assertEquals("10.1",BuildConfig.VERSION_NAME);assertTrue(expected.isFile());
    JSONObject before=new JSONObject(read(new FileInputStream(expected))),actual=new JSONObject(db.exportJson());
    JSONObject oldTables=before.getJSONObject("tables"),newTables=actual.getJSONObject("tables");
    for(Iterator<String> tables=oldTables.keys();tables.hasNext();){String table=tables.next();JSONArray oldRows=oldTables.getJSONArray(table),newRows=newTables.getJSONArray(table);assertEquals(table,oldRows.length(),newRows.length());for(int i=0;i<oldRows.length();i++){JSONObject old=oldRows.getJSONObject(i),now=null;for(int j=0;j<newRows.length();j++)if(newRows.getJSONObject(j).getLong("id")==old.getLong("id"))now=newRows.getJSONObject(j);assertNotNull(now);for(Iterator<String> keys=old.keys();keys.hasNext();){String key=keys.next();assertEquals(table+"."+key,String.valueOf(old.get(key)),String.valueOf(now.get(key)));}}}
    assertEquals(before.getString("attachment_hash"),hash(c,Uri.parse(db.caseAttachments(1).get(0).uri)));
    assertEquals("وکیل ارتقا",c.getSharedPreferences("office_profile",0).getString("name",""));assertEquals(0,db.countDemoRows());assertEquals(14,db.getReadableDatabase().getVersion());
   }
  }
 }
 static String read(InputStream in)throws Exception{try(InputStream stream=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=stream.read(b))!=-1)out.write(b,0,n);return out.toString("UTF-8");}}
 static String hash(Context c,Uri uri)throws Exception{java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");try(InputStream in=c.getContentResolver().openInputStream(uri)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)digest.update(b,0,n);}return android.util.Base64.encodeToString(digest.digest(),android.util.Base64.NO_WRAP);}
}
