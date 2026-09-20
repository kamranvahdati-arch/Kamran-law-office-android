package ir.kamranvahdati.lawoffice;
import static org.junit.Assert.*;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class FinancialFilterTest {
    @Test public void reportTotalsUseOnlyMatchingTransactions(){
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(OfficeDb.ENCRYPTED_NAME);
        OfficeDb db=new OfficeDb(context);db.getWritableDatabase();
        db.addLedger(1,"expense","هتل",12345,"1405/06/20","وکیل","آزمایشی");
        db.addLedger(1,"expense","هتل",99999,"1405/05/20","وکیل","خارج بازه");
        db.addLedger(1,"expense","کارشناسی",88888,"1405/06/20","وکیل","خارج دسته");
        String output=db.filteredFinancialText("1405/06/19","1405/06/21",null,1L,null,"همه",false,false,"expense","هتل");
        assertTrue(output.contains("جمع هزینه در نتیجه فیلتر: 12345"));
        assertFalse(output.contains("99999"));assertFalse(output.contains("88888"));
        try{db.filteredFinancialText("1405/07/01","1405/06/01",null,null,null,"همه",false,false,null,null);fail("invalid range");}catch(IllegalArgumentException expected){}
        db.close();
    }
}
