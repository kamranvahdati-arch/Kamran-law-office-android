package ir.kamranvahdati.lawoffice;
import static org.junit.Assert.*;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.json.JSONObject;
import java.io.*;


@RunWith(AndroidJUnit4.class)
public class V102StorageTest {
 @Test public void lightBackupRoundTripPreservesMetadataWithoutMedia()throws Exception{
  Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
  try(OfficeDb db=new OfficeDb(c)){
   JSONObject root=new JSONObject().put("database",new JSONObject(db.exportJson())).put("profile",new JSONObject());
   // Use empty attachment list: no actual project/user files enter test artifacts.
   root.getJSONObject("database").getJSONObject("tables").put("case_attachments",new org.json.JSONArray());
   ByteArrayOutputStream bytes=new ByteArrayOutputStream();FullBackup.write(c,root.toString(),bytes,"storage-test-password");
   try(FullBackup.Prepared restored=FullBackup.read(c,new ByteArrayInputStream(bytes.toByteArray()),"storage-test-password")){
    JSONObject result=new JSONObject(restored.bundle);assertEquals("KLO-BUNDLE-1",result.getString("format"));assertEquals(15,result.getInt("schema"));assertFalse(result.has("media"));
   }
  }
 }
 @Test public void retentionOwnFilesOnlyAndInclusiveThirtyDays(){
  String today="2026-10-02";String suffix="-00000000-0000-0000-0000-000000000000.vkb";
  assertTrue(WorkspaceBackups.obsolete("VOKANO-2026-10-02"+suffix,today));
  assertFalse(WorkspaceBackups.obsolete("VOKANO-2026-09-03"+suffix,today));
  assertTrue(WorkspaceBackups.obsolete("VOKANO-2026-09-02"+suffix,today));
  assertFalse(WorkspaceBackups.obsolete("private-document.pdf",today));
  assertFalse(WorkspaceBackups.obsolete("VOKANO-2026-10-03"+suffix,today));
 }
 @Test public void clientMembershipDoesNotErasePerson(){
  Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();try(OfficeDb db=new OfficeDb(c)){
   db.getWritableDatabase().beginTransaction();try{
    int people=db.countPersons(),clients=db.countClients();long id=db.addClient("آزمون ساختاریافته","","","","","","");
    db.setClientMembership(id,false);assertEquals(people+1,db.countPersons());assertEquals(clients,db.countClients());
    db.setClientMembership(id,true);assertEquals(clients+1,db.countClients());
   }finally{db.getWritableDatabase().endTransaction();}
  }
 }
}
