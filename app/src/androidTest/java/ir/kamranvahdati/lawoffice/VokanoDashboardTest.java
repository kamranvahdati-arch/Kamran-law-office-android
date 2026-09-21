package ir.kamranvahdati.lawoffice;

import static org.junit.Assert.*;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class VokanoDashboardTest {
    @Test public void fourNewCountersUseSavedRecordsAndCardsOpenRealLists() {
        Instrumentation instrument=InstrumentationRegistry.getInstrumentation();
        Context context=instrument.getTargetContext();
        String today=JalaliDate.today().value(),tomorrow=JalaliDate.addDays(today,1);
        int hearings,consultations,deadlines,personal;
        OfficeDb db=new OfficeDb(context);try {
            hearings=db.countFutureHearings(today);consultations=db.countConsultations();
            deadlines=db.countOpenDeadlines();personal=db.countPersonalTasks();
            db.saveAppointment("جلسه دادگاه","موکل آزمایشی","",1L,1L,tomorrow,"13:25","14:10","شعبه آزمایشی","آزمون وکانو","۲","دادگاه","تهران");
            db.saveAppointment("مشاوره حضوری","موکل آزمایشی","",1L,1L,tomorrow,"15:05","15:50","دفتر","آزمون وکانو","","","تهران");
            db.saveDeadline(1,1L,"رفع نقص آزمایشی",today,tomorrow,1,"آزمون وکانو");
            db.addLinkedTask("مطالعه آزمایشی","برنامه شخصی","مطالعه",true,null,null,tomorrow,"17:25","18:10","خانه","عادی","آزمون وکانو");
            assertEquals(hearings+1,db.countFutureHearings(today));
            assertEquals(consultations+1,db.countConsultations());
            assertEquals(deadlines+1,db.countOpenDeadlines());
            assertEquals(personal+1,db.countPersonalTasks());
            assertTrue(db.personalTasks().stream().anyMatch(t->"مطالعه آزمایشی".equals(t.title)));
        } finally {db.close();}
        context.getSharedPreferences("office_profile",Context.MODE_PRIVATE).edit()
            .putBoolean("notification_permission_prompted",true).putString("theme_id",AppTheme.DARK)
            .putString("name","وکیل آزمایشی").putString("professional_body","کانون وکلای دادگستری")
            .putString("province","تهران").putString("city","تهران")
            .putString("national_id","0013540831").putString("phone","09120000001")
            .putBoolean("profile_complete",true).putBoolean("lock_enabled",false).commit();
        MainActivity activity=(MainActivity)instrument.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            instrument.waitForIdleSync();assertNotNull(activity.root);
            for(String label:new String[]{"وقت‌های رسیدگی","مهلت‌ها","کارهای شخصی","وقت‌های مشاوره"}){
                instrument.runOnMainSync(()->{
                    activity.dashboard();TextView card=find(activity.page,label);
                    assertNotNull("Missing dashboard card: "+label,card);assertTrue(card.getParent() instanceof View);
                    ((View)card.getParent()).performClick();
                    assertTrue("No destination for "+label,activity.page.getChildCount()>0);
                    assertNotEquals("وکانو | VOKANO",activity.title.getText().toString());
                });
            }
            instrument.runOnMainSync(()->{
                activity.dashboard();assertNotNull(find(activity.page,"برنامه واقعی امروز"));
                assertNotNull(find(activity.page,"تقویم کاری شمسی"));
                assertNotNull(find(activity.page,"مهلت‌های نزدیک"));
            });
        } finally {instrument.runOnMainSync(activity::finish);instrument.waitForIdleSync();}
    }

    @Test public void updatingPersonalTaskPreservesIdentityAndMovesExistingReminders() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String today=JalaliDate.today().value(),tomorrow=JalaliDate.addDays(today,1);
        OfficeDb db=new OfficeDb(context);try {
            db.addLinkedTask("خواندن","برنامه شخصی","مطالعه",true,null,null,today,"13:25","14:00","خانه","عادی","");
            OfficeDb.TaskRecord before=null;
            for(OfficeDb.TaskRecord task:db.personalTasks())if("خواندن".equals(task.title)){before=task;break;}
            assertNotNull(before);final long taskId=before.id;int count=db.remindersFor("task",taskId).size();
            long original=db.remindersFor("task",taskId).get(0).at;
            db.updateTaskDetails(taskId,"مطالعه",tomorrow,"13:25","14:00","دفتر","فوری","تغییر تاریخ");
            OfficeDb.TaskRecord after=null;
            for(OfficeDb.TaskRecord task:db.personalTasks())if(task.id==taskId){after=task;break;}
            assertNotNull(after);assertEquals("مطالعه",after.title);assertEquals(tomorrow,after.date);
            assertEquals(count,db.remindersFor("task",taskId).size());
            assertTrue(db.remindersFor("task",taskId).get(0).at>original);
            db.setTaskDone(taskId,true);
            assertTrue(db.personalTasks().stream().anyMatch(t->t.id==taskId&&t.done==1));
        } finally {db.close();}
    }

    @Test public void approvedVokanoAssetsAreBundled() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertNotNull(BitmapFactory.decodeResource(context.getResources(),R.drawable.vokano_icon_dark));
        assertNotNull(BitmapFactory.decodeResource(context.getResources(),R.drawable.vokano_icon_light));
    }

    private TextView find(View view,String text) {
        if(view instanceof TextView && text.contentEquals(((TextView)view).getText()))return (TextView)view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){
            TextView found=find(group.getChildAt(i),text);if(found!=null)return found;
        }}return null;
    }
}
