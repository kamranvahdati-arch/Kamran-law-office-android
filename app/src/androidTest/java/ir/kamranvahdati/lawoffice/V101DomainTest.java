package ir.kamranvahdati.lawoffice;

import static org.junit.Assert.*;
import android.app.Instrumentation;
import android.content.*;
import android.view.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.*;

@RunWith(AndroidJUnit4.class)
public class V101DomainTest {
 @Test public void personRolesContractsAndIndependentMoneyRemainConsistent()throws Exception{
  Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();try(OfficeDb db=new OfficeDb(c)){
   OfficeV101 d=new OfficeV101(db);d.db().beginTransaction();try{
    ContentValues p=new ContentValues();p.put("name","شرکت آزمون");p.put("person_type","legal");p.put("national_id","12345678901");p.put("province","تهران");p.put("city","تهران");long id=d.savePerson(null,p);
    boolean duplicate=false;try{d.savePerson(null,p);}catch(IllegalArgumentException expected){duplicate=true;}assertTrue(duplicate);
    d.role(1,id,"خوانده");d.role(1,id,"خوانده");assertEquals(1,d.rows("SELECT id FROM person_roles WHERE person_id=?",Long.toString(id)).size());
    d.contractParty(1,id,"طرف مقابل");d.contractParty(1,id,"طرف مقابل");assertEquals("شرکت آزمون",d.opponents(1));
    ContentValues account=new ContentValues();account.put("name","حساب آزمون");account.put("kind","بانکی");long bank=d.account(account);
    ContentValues receipt=new ContentValues();receipt.put("kind","payment");receipt.put("category","مشاوره");receipt.put("amount",500);receipt.put("entry_date",JalaliDate.today().value());receipt.put("person_id",id);receipt.put("account_id",bank);d.transaction(null,receipt);
    assertEquals(1,d.transactions(null,id,bank,"payment",null,null).size());
    ContentValues expense=new ContentValues(receipt);expense.put("kind","expense");expense.put("amount",100);d.transaction(1L,expense);assertEquals(2,d.transactions(null,id,bank,null,null,null).size());
    receipt.put("contract_id",1L);boolean invalid=false;try{d.transaction(null,receipt);}catch(IllegalArgumentException expected){invalid=true;}assertTrue(invalid);
    ContentValues offer=new ContentValues();offer.put("kind","offer");offer.put("title","اعلام آمادگی");offer.put("province","فارس");offer.put("city","شیراز");offer.put("sync_status","remote");long request=d.collaboration(offer);assertEquals("local",OfficeV101.text(d.rows("SELECT * FROM collaboration_requests WHERE id=?",Long.toString(request)).get(0),"sync_status"));
    // Existing backup format must include the added records without losing relationships.
    String snapshot=db.exportJson();db.importJson(snapshot);assertEquals(2,d.transactions(null,id,bank,null,null,null).size());assertEquals("شرکت آزمون",d.opponents(1));
   }finally{d.db().endTransaction();}
  }
 }
 @Test public void lightAndDarkScreensRespectBarsAndExposeScopedRoutes()throws Exception{
  Instrumentation ins=InstrumentationRegistry.getInstrumentation();Context c=ins.getTargetContext();
  for(String theme:new String[]{AppTheme.LIGHT,AppTheme.DARK}){
   c.getSharedPreferences("office_profile",0).edit().putString("theme_id",theme).commit();
   MainActivity a=(MainActivity)ins.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));ins.waitForIdleSync();
   try{assertNotNull(a.db);ins.runOnMainSync(()->a.go(MainActivity.PEOPLE,false));ins.waitForIdleSync();assertEquals("اشخاص حقیقی و حقوقی",a.title.getText().toString());
    ins.runOnMainSync(()->a.go(MainActivity.ACCOUNTING,false));ins.waitForIdleSync();assertEquals("حسابداری",a.title.getText().toString());
    ins.runOnMainSync(()->a.go(MainActivity.COOPERATION,false));ins.waitForIdleSync();assertEquals("همکاری وکلا",a.title.getText().toString());
    ins.runOnMainSync(()->a.go(MainActivity.LEGAL,false));ins.waitForIdleSync();assertEquals(0,a.db.countLegalDocuments());
    ins.runOnMainSync(()->{a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);a.dashboard();});ins.waitForIdleSync();
    android.graphics.Bitmap screenshot=ins.getUiAutomation().takeScreenshot();assertNotNull(screenshot);java.io.File proof=new java.io.File(c.getExternalFilesDir(null),"v101-proof");assertTrue(proof.isDirectory()||proof.mkdirs());try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(proof,"bars-"+theme+".png"))){assertTrue(screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));}screenshot.recycle();
    ins.runOnMainSync(()->{WindowInsets insets=a.root.getRootWindowInsets();assertNotNull(insets);android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());assertTrue(a.root.getPaddingTop()>=bars.top);assertTrue(a.root.getPaddingBottom()>=bars.bottom);boolean light=android.graphics.Color.luminance(a.theme.background)>0.5;int appearance=a.getWindow().getInsetsController().getSystemBarsAppearance();assertEquals(light,(appearance&WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)!=0);});
   }finally{ins.runOnMainSync(a::finish);ins.waitForIdleSync();}
  }
 }
}
