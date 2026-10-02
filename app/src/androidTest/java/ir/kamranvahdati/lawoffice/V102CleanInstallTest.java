package ir.kamranvahdati.lawoffice;

import static org.junit.Assert.*;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Run only after the candidate's first install on an empty emulator. */
@RunWith(AndroidJUnit4.class)
public class V102CleanInstallTest {
 @Test public void releaseStartsWithoutSyntheticOrOfficeRecords(){
  Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
  assertFalse(BuildConfig.DEBUG);assertEquals("10.2",BuildConfig.VERSION_NAME);
  assertEquals("ir.kamranvahdati.lawoffice",c.getPackageName());
  try(OfficeDb db=new OfficeDb(c)){
   assertEquals(15,db.getReadableDatabase().getVersion());
   assertEquals(0,db.countDemoRows());
   assertEquals(0,db.countCases(null));
   assertEquals(0,db.countClients());
   assertEquals(0,db.countPersons());
   try(android.database.Cursor tasks=db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM tasks",null)){
    assertTrue(tasks.moveToFirst());assertEquals(0,tasks.getInt(0));
   }
  }
 }
}
