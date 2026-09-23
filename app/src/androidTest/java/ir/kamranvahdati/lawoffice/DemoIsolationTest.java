package ir.kamranvahdati.lawoffice;
import static org.junit.Assert.*;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DemoIsolationTest {
    @Test public void deletingSamplesKeepsUserTransactionsAndTheirParents(){
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(OfficeDb.ENCRYPTED_NAME);
        OfficeDb db=new OfficeDb(context);assertEquals(100,db.countClients());
        db.addLedger(1,"expense","کارشناسی",12345,"1405/06/28","وکیل","داده ساختگی برای شبیه‌سازی ثبت کاربر");
        db.softDeleteDemoData();
        assertEquals(1,db.countCases(null));assertEquals(1,db.countClients());
        assertEquals(1,db.caseClients(1).size());assertEquals(1,db.ledger(1).size());
        assertEquals(12345,db.ledger(1).get(0).amount);
        assertEquals(0,db.countDemoRows());db.close();
    }
}
