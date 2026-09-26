package ir.kamranvahdati.lawoffice;

import static org.junit.Assert.*;
import android.content.Context;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/** Verifies fixtures created by the ORIGINAL permanent-signed V10 instrumentation. */
@RunWith(AndroidJUnit4.class)
public class V101ProductionUpgradeTest {
 @Test public void originalProductionDataSurvivesSignedUpgrade()throws Exception{
  Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
  assertFalse(BuildConfig.DEBUG);assertEquals("10.1",BuildConfig.VERSION_NAME);
  File expectedFile=new File(c.getExternalFilesDir("production-proof"),"expected.json");assertTrue(expectedFile.isFile());
  JSONObject expected=new JSONObject(V101UpgradeTest.read(new FileInputStream(expectedFile)));
  try(OfficeDb db=new OfficeDb(c)){
   JSONObject before=expected.getJSONObject("database").getJSONObject("tables"),after=new JSONObject(db.exportJson()).getJSONObject("tables");
   for(Iterator<String> tables=before.keys();tables.hasNext();){String table=tables.next();JSONArray oldRows=before.getJSONArray(table),newRows=after.getJSONArray(table);assertEquals(table,oldRows.length(),newRows.length());for(int i=0;i<oldRows.length();i++){JSONObject old=oldRows.getJSONObject(i),now=null;for(int j=0;j<newRows.length();j++)if(newRows.getJSONObject(j).getLong("id")==old.getLong("id"))now=newRows.getJSONObject(j);assertNotNull(now);for(Iterator<String> keys=old.keys();keys.hasNext();){String key=keys.next();assertEquals(table+"."+key,String.valueOf(old.get(key)),String.valueOf(now.get(key)));}}}
   for(OfficeDb.CaseAttachmentRecord item:db.caseAttachments(1)){Uri uri=Uri.parse(item.uri);assertEquals(expected.getJSONObject("hashes").getString(item.name),hash(c,uri));if(item.name.endsWith("pdf"))try(android.os.ParcelFileDescriptor fd=c.getContentResolver().openFileDescriptor(uri,"r");android.graphics.pdf.PdfRenderer renderer=new android.graphics.pdf.PdfRenderer(fd)){assertEquals(1,renderer.getPageCount());}if(item.name.endsWith("png"))try(InputStream in=c.getContentResolver().openInputStream(uri)){android.graphics.Bitmap image=android.graphics.BitmapFactory.decodeStream(in);assertNotNull(image);assertEquals(32,image.getWidth());image.recycle();}}
   assertEquals(expected.getJSONObject("hashes").getString("image.png"),hash(c,Uri.parse(c.getSharedPreferences("office_profile",0).getString("photo",""))));
   assertEquals("وکیل آزمون",c.getSharedPreferences("office_profile",0).getString("name",""));assertEquals(0,db.countDemoRows());assertEquals(14,db.getReadableDatabase().getVersion());
  }
 }
 static String hash(Context c,Uri uri)throws Exception{MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream in=c.getContentResolver().openInputStream(uri)){byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)digest.update(b,0,n);}StringBuilder text=new StringBuilder();for(byte b:digest.digest())text.append(String.format(Locale.ROOT,"%02x",b&255));return text.toString();}
}
