package ir.kamranvahdati.lawoffice;

import static org.junit.Assert.*;
import android.app.Instrumentation;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@RunWith(AndroidJUnit4.class)
public class ProductionBaselineTest {
 private static final String CODE="fixture-only-backup-2026";
 @Test public void emptyReleaseAndCompleteBackupAcrossApplicationDataReset()throws Exception{
  String mode=InstrumentationRegistry.getArguments().getString("production_mode","");Assume.assumeTrue(mode.equals("seed")||mode.equals("restore"));
  assertFalse("Production must not be debuggable",BuildConfig.DEBUG);
  Instrumentation ins=InstrumentationRegistry.getInstrumentation();Context c=ins.getTargetContext();
  assertEquals("ir.kamranvahdati.lawoffice",c.getPackageName());
  MainActivity a=(MainActivity)ins.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));ins.waitForIdleSync();
  File folder=c.getExternalFilesDir("production-proof");assertTrue(folder.isDirectory()||folder.mkdirs());
  File backup=new File(folder,"full.klo"),expectedFile=new File(folder,"expected.json");
  try{
   JSONObject empty=new JSONObject(a.db.exportJson()).getJSONObject("tables");
   for(Iterator<String> it=empty.keys();it.hasNext();){String table=it.next();assertEquals("Nonempty clean release: "+table,0,empty.getJSONArray(table).length());}
   assertEquals(0,a.db.countDemoRows());assertFalse(a.prefs.contains("photo"));
   if(mode.equals("seed")){
    String today=JalaliDate.today().value(),tomorrow=JalaliDate.addDays(today,1);
    long client=a.db.addClient("موکل آزمون پشتیبان","0013540831","پدر","1360/01/01","09120000001","تهران","صرفاً داده تست");
    OfficeDb.CaseRecord record=new OfficeDb.CaseRecord();record.title="پرونده آزمون پشتیبان";record.clientId=client;long caseId=a.db.addCase(record);
    a.db.addRepresentationContract(caseId,"EC-10",today,"دفاع","دادگاه",10000,true,"",true,"منفرد","آزمون",Collections.singletonList(client),Collections.emptyList());
    a.db.addFinancialContract(caseId,today,10000,"دو قسط","آزمون");long installment=a.db.addInstallment(caseId,"قسط اول",5000,tomorrow);a.db.payInstallment(installment,2000,today,"موکل","آزمون");
    a.db.addPaymentCheck(caseId,installment,"CHK-10",tomorrow,3000,"بانک","شعبه","pending","آزمون");
    a.db.saveDeadline(caseId,client,"رفع نقص",today,tomorrow,1,"مهلت فردا");
    a.db.saveAppointment("جلسه دادگاه","موکل","",client,caseId,tomorrow,"13:25","14:10","دادگاه","آزمون","۲","دادگاه","تهران");
    a.db.saveAppointment("مشاوره حضوری","موکل","",client,caseId,today,"15:25","16:10","دفتر","آزمون","","","تهران");
    a.db.addLinkedTask("مطالعه","برنامه شخصی","مطالعه",true,null,null,today,"17:25","18:10","دفتر","عادی","آزمون");
    File photo=new File(c.getCacheDir(),"fixture.png");Bitmap bitmap=Bitmap.createBitmap(32,32,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.BLUE);
    try(OutputStream out=new FileOutputStream(photo)){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}bitmap.recycle();
    Uri photoUri=MediaStorage.copy(c,Uri.fromFile(photo));photo.delete();
    a.prefs.edit().putString("photo",photoUri.toString()).putString("name","وکیل آزمون").putString("professional_body","کانون وکلای دادگستری").putString("province","تهران").putString("city","تهران").putString("national_id","0013540831").putString("phone","09120000001").putBoolean("profile_complete",true).putBoolean("notification_permission_prompted",true).commit();
    a.db.addCaseAttachment(caseId,"image.png","image/png",photoUri.toString());
    File pdf=new File(c.getCacheDir(),"fixture.pdf");PdfDocument document=new PdfDocument();try{
     PdfDocument.Page page=document.startPage(new PdfDocument.PageInfo.Builder(300,400,1).create());page.getCanvas().drawText("VOKANO backup test",20,40,new Paint());document.finishPage(page);try(OutputStream out=new FileOutputStream(pdf)){document.writeTo(out);}
    }finally{document.close();}
    Uri pdfUri=MediaStorage.copy(c,Uri.fromFile(pdf));pdf.delete();a.db.addCaseAttachment(caseId,"document.pdf","application/pdf",pdfUri.toString());
    File big=new File(c.getCacheDir(),"fixture-large.bin");Random random=new Random(310);try(OutputStream out=new FileOutputStream(big)){byte[] block=new byte[32768];for(int i=0;i<896;i++){random.nextBytes(block);out.write(block);}}
    Uri large=MediaStorage.copy(c,Uri.fromFile(big));big.delete();a.db.addCaseAttachment(caseId,"large.bin","application/octet-stream",large.toString());
    JSONObject expected=new JSONObject(a.exportBundle());JSONObject hashes=new JSONObject();
    for(OfficeDb.CaseAttachmentRecord item:a.db.caseAttachments(caseId))hashes.put(item.name,hash(c,Uri.parse(item.uri)));
    expected.put("hashes",hashes);write(expectedFile,expected.toString().getBytes(StandardCharsets.UTF_8));
    try(OutputStream out=new FileOutputStream(backup)){FullBackup.write(c,a.exportBundle(),out,CODE);}assertTrue("Large backup must exceed old 25 MiB limit",backup.length()>25*1024*1024);
    // An unreadable reference must never yield a successful backup.
    JSONObject missing=new JSONObject(a.exportBundle());missing.getJSONObject("profile").put("photo","content://missing/no-file");boolean failed=false;
    try{FullBackup.write(c,missing.toString(),new ByteArrayOutputStream(),CODE);}catch(Exception e){failed=true;}assertTrue(failed);
   }else{
    assertTrue("External backup must survive host-side data reset",backup.isFile());JSONObject expected=new JSONObject(new String(read(expectedFile),StandardCharsets.UTF_8));
    try(InputStream in=new FileInputStream(backup);FullBackup.Prepared result=FullBackup.read(c,in,CODE)){a.importBundle(result.bundle);result.commit();}
    JSONObject actual=new JSONObject(a.db.exportJson()).getJSONObject("tables"),before=expected.getJSONObject("database").getJSONObject("tables");
    for(Iterator<String> it=before.keys();it.hasNext();){String table=it.next();JSONArray rows=before.getJSONArray(table),current=actual.getJSONArray(table);assertEquals(table,rows.length(),current.length());
     for(int i=0;i<rows.length();i++){JSONObject old=rows.getJSONObject(i),now=current.getJSONObject(i);for(Iterator<String> keys=old.keys();keys.hasNext();){String key=keys.next();if(!key.equals("content_uri"))assertEquals(table+"."+key,String.valueOf(old.get(key)),String.valueOf(now.get(key)));}}
    }
    assertEquals(1,a.db.countClients());assertEquals(1,a.db.cases(null,"همه",null).size());assertEquals(0,a.db.countDemoRows());
    for(OfficeDb.CaseAttachmentRecord item:a.db.caseAttachments(1)){
     Uri uri=Uri.parse(item.uri);assertEquals(expected.getJSONObject("hashes").getString(item.name),hash(c,uri));
     if(item.name.endsWith("pdf"))try(ParcelFileDescriptor fd=c.getContentResolver().openFileDescriptor(uri,"r");PdfRenderer renderer=new PdfRenderer(fd)){assertEquals(1,renderer.getPageCount());try(PdfRenderer.Page page=renderer.openPage(0)){assertTrue(page.getWidth()>0);}}
     if(item.name.endsWith("png"))try(InputStream in=c.getContentResolver().openInputStream(uri)){Bitmap image=BitmapFactory.decodeStream(in);assertNotNull(image);assertEquals(32,image.getWidth());image.recycle();}
    }
    assertEquals(expected.getJSONObject("hashes").getString("image.png"),hash(c,Uri.parse(a.prefs.getString("photo",""))));
    // Wrong key and invalid DB relationships leave live data unchanged.
    boolean failed=false;try(InputStream in=new FileInputStream(backup);FullBackup.Prepared ignored=FullBackup.read(c,in,"incorrect-password")){}catch(Exception e){failed=true;}assertTrue(failed);assertEquals(1,a.db.countClients());
    JSONObject bad=new JSONObject(a.exportBundle());bad.getJSONObject("database").getJSONObject("tables").getJSONArray("cases").getJSONObject(0).put("client_id",99999);
    failed=false;try{a.importBundle(bad.toString());}catch(Exception e){failed=true;}assertTrue(failed);assertEquals(1,a.db.countClients());assertEquals(3,a.db.caseAttachments(1).size());
    ins.runOnMainSync(a::dashboard);ins.waitForIdleSync();assertNotNull(a.root);assertEquals("وکانو | VOKANO",a.title.getText().toString());
   }
  }finally{ins.runOnMainSync(a::finish);ins.waitForIdleSync();}
 }
 private static String hash(Context c,Uri uri)throws Exception{MessageDigest hash=MessageDigest.getInstance("SHA-256");try(InputStream in=c.getContentResolver().openInputStream(uri)){byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)hash.update(b,0,n);}StringBuilder s=new StringBuilder();for(byte b:hash.digest())s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
 private static void write(File file,byte[] bytes)throws Exception{try(OutputStream out=new FileOutputStream(file)){out.write(bytes);}}
 private static byte[] read(File file)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=new FileInputStream(file)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}return out.toByteArray();}
}
