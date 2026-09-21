package ir.kamranvahdati.lawoffice;
import static org.junit.Assert.*;
import android.content.Context;
import java.util.Arrays;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ContractPeopleTest {
    @Test public void explicitPeopleShareOneContractAndColleagueEditKeepsIdentity(){
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();context.deleteDatabase(OfficeDb.ENCRYPTED_NAME);
        OfficeDb db=new OfficeDb(context);db.getWritableDatabase();
        long second=db.addClient("موکل فرضی دوم","","","","","","آزمایش");
        long firstLawyer=db.addAndLinkCollaborator(1,"وکیل","اول","","کانون وکلای دادگستری","","","مشترک",20);
        long secondLawyer=db.addAndLinkCollaborator(1,"وکیل","دوم","","کانون وکلای دادگستری","","","مشترک",15);
        long relation=db.caseCollaborators(1).get(0).id;
        db.linkCollaborator(1,db.caseCollaborators(1).get(0).collaboratorId,"مشترک",30,"اصلاح توافق");
        assertEquals(relation,db.caseCollaborators(1).get(0).id);assertEquals(2,db.caseCollaborators(1).size());
        long contract=db.addRepresentationContract(1L,"TEST-PEOPLE","1405/06/28","دفاع","تجدیدنظرخواهی",1000,true,"",true,"مشترک","",Arrays.asList(1L,second),Arrays.asList(firstLawyer,secondLawyer));
        assertTrue(db.isClientLinked(1,second));assertEquals(contract,db.representationContracts(null,second).get(0).id);
        assertEquals(contract,db.representationContracts(1L,1L).get(0).id);
        assertTrue(db.contractCollaboratorNames(contract).contains("اول"));assertTrue(db.contractCollaboratorNames(contract).contains("دوم"));
        int count=db.collaborators().size();try{db.addAndLinkCollaborator(1,"نام","فرضی","","","","","مشترک",Double.NaN);fail("invalid percentage");}catch(IllegalArgumentException expected){}
        assertEquals(count,db.collaborators().size());db.close();
    }
}
