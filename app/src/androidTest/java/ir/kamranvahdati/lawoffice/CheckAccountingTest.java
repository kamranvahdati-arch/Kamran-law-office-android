package ir.kamranvahdati.lawoffice;
import static org.junit.Assert.*;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CheckAccountingTest {
    @Test public void collectionAndReversalAreAtomicAndIdempotent() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(OfficeDb.ENCRYPTED_NAME);
        OfficeDb db=new OfficeDb(context);db.getWritableDatabase();
        long installment=db.addInstallment(1,"قسط آزمایشی",1000,"1405/10/01");
        long check=db.addPaymentCheck(1,installment,"TEST-1","1405/10/01",600,"بانک فرضی","شعبه فرضی","pending","تست");
        db.updatePaymentCheckStatus(check,"collected","1405/09/01","موکل");
        long paid=0;for(OfficeDb.InstallmentRecord i:db.installments(1))if(i.id==installment)paid=i.paid;
        assertEquals(600,paid);
        db.updatePaymentCheckStatus(check,"collected","1405/09/01","موکل");
        for(OfficeDb.InstallmentRecord i:db.installments(1))if(i.id==installment)assertEquals(600,i.paid);
        db.updatePaymentCheckStatus(check,"bounced","1405/09/02","موکل");
        for(OfficeDb.InstallmentRecord i:db.installments(1))if(i.id==installment)assertEquals(0,i.paid);
        db.updatePaymentCheckStatus(check,"collected","1405/09/03","موکل");
        long excessive=db.addPaymentCheck(1,installment,"TEST-2","1405/10/01",500,"بانک","شعبه","pending","");
        try{db.updatePaymentCheckStatus(excessive,"collected");fail("overpayment accepted");}catch(IllegalArgumentException expected){}
        for(OfficeDb.PaymentCheckRecord c:db.paymentChecks(1L,null))if(c.id==excessive)assertEquals("pending",c.status);
        for(OfficeDb.InstallmentRecord i:db.installments(1))if(i.id==installment)assertEquals(600,i.paid);
        db.close();
    }
}
