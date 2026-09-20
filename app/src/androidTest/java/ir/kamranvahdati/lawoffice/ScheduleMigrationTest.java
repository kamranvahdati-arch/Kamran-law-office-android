package ir.kamranvahdati.lawoffice;

import static org.junit.Assert.*;
import android.content.Context;
import android.database.Cursor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ScheduleMigrationTest {
    @Test public void additiveUpgradePreservesDatesAndCustomRemindersDoNotDuplicate() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(OfficeDb.ENCRYPTED_NAME);
        OfficeDb before=new OfficeDb(context);
        String today=JalaliDate.today().value(),due=JalaliDate.addDays(today,10);
        long deadline=before.addDeadline(1,"ده روز آزمایشی",today,due,10,"فقط داده ساختگی");
        long appointment=before.addAppointment("جلسه دادگاه","موکل فرضی","",1L,1L,JalaliDate.addDays(today,1),"13:25","","آدرس فرضی","آزمون");
        long originalReminder=before.remindersFor("deadline",deadline).get(0).at;
        for(String column:new String[]{"branch","authority","city","attendance_status","completed_at"})
            before.getWritableDatabase().execSQL("ALTER TABLE appointments DROP COLUMN "+column);
        before.getWritableDatabase().execSQL("ALTER TABLE deadlines DROP COLUMN client_id");
        before.getWritableDatabase().execSQL("ALTER TABLE deadlines DROP COLUMN completed_at");
        before.getWritableDatabase().execSQL("ALTER TABLE reminders DROP COLUMN label");
        before.getWritableDatabase().setVersion(11);before.close();
        OfficeDb db=new OfficeDb(context);
        assertEquals(13,db.getReadableDatabase().getVersion());
        OfficeDb.DeadlineRecord record=null;for(OfficeDb.DeadlineRecord d:db.deadlines(null,false))if(d.id==deadline)record=d;
        assertNotNull(record);assertEquals(due,record.dueDate);
        assertEquals(originalReminder,db.remindersFor("deadline",deadline).get(0).at);
        db.setAppointmentLocation(appointment,"۲","مجتمع آزمایشی","تهران");
        OfficeDb.AppointmentRecord visit=null;for(OfficeDb.AppointmentRecord a:db.appointments(null))if(a.id==appointment)visit=a;
        assertNotNull(visit);assertEquals("13:25",visit.start);assertEquals("۲",visit.branch);assertEquals("planned",visit.attendance);
        assertEquals(5,db.remindersFor("deadline",deadline).size());
        long custom=db.addCustomReminder("appointment",appointment,JalaliDate.addDays(today,1),"08:25","مرور دفاعیات");
        try {db.addCustomReminder("appointment",appointment,JalaliDate.addDays(today,1),"08:25","تکراری");fail("duplicate reminder accepted");}catch(IllegalArgumentException expected){}
        assertTrue(db.fireReminder(custom));assertFalse(db.fireReminder(custom));
        db.completeDeadline(deadline);
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT completed,completed_at FROM deadlines WHERE id=?",new String[]{String.valueOf(deadline)})){
            assertTrue(c.moveToFirst());assertEquals(1,c.getInt(0));assertNotNull(c.getString(1));
        }
        assertFalse(db.fireReminder(db.remindersFor("deadline",deadline).get(0).id));
        db.setScheduleStatus("deadline",deadline,"planned");
        String changed=JalaliDate.addDays(today,12);
        db.changeScheduleDate("deadline",deadline,changed,"09:00","");
        assertEquals(5,db.remindersFor("deadline",deadline).size());
        db.setScheduleStatus("appointment",appointment,"cancelled");
        assertFalse(db.fireReminder(db.remindersFor("appointment",appointment).get(0).id));
        for(OfficeDb.ReminderRecord r:db.remindersFor("deadline",deadline))db.removeReminder(r.id);
        db.ensureReminderRules();assertTrue(db.remindersFor("deadline",deadline).isEmpty());
        db.close();
    }
}
