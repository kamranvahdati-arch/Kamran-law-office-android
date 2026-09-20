package ir.kamranvahdati.lawoffice;
import static org.junit.Assert.*;
import android.content.Context;
import java.util.Arrays;
import java.util.Collections;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TrashRestoreTest {
    @Test public void contractCheckAndAttachmentReturnFromTrash(){
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();context.deleteDatabase(OfficeDb.ENCRYPTED_NAME);
        OfficeDb db=new OfficeDb(context);db.getWritableDatabase();
        long contract=db.addRepresentationContract(1L,"TEST","1405/06/28","دفاع","آزمایش",0,false,"",false,"انفرادی","",Arrays.asList(1L),Collections.emptyList());
        db.deleteRepresentationContract(contract);assertTrue(db.representationContracts(1L,null).isEmpty());db.restoreRecord("contract",contract);assertEquals(1,db.representationContracts(null,1L).size());
        long check=db.addPaymentCheck(1,null,"TEST","1405/10/01",100,"بانک","شعبه","pending","");db.deletePaymentCheck(check);db.restoreRecord("check",check);assertEquals(1,db.paymentChecks(1L,null).size());
        long attachment=db.addCaseAttachment(1,"آزمایش.pdf","application/pdf","content://test/document");db.deleteCaseAttachment(attachment);db.restoreRecord("attachment",attachment);assertEquals(1,db.caseAttachments(1).size());
        db.close();
    }
}
