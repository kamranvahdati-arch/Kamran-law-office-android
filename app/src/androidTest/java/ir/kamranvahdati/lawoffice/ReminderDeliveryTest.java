package ir.kamranvahdati.lawoffice;

import static org.junit.Assert.*;
import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.database.Cursor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ReminderDeliveryTest {
    @Test public void receiverHonorsSettingAndPostsOnceAndCatchupSkipsOlderStages() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(OfficeDb.ENCRYPTED_NAME);
        if(Build.VERSION.SDK_INT>=33)InstrumentationRegistry.getInstrumentation().getUiAutomation()
            .grantRuntimePermission(context.getPackageName(),Manifest.permission.POST_NOTIFICATIONS);
        NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancelAll();
        OfficeDb db=new OfficeDb(context);String today=JalaliDate.today().value();
        long id=db.addDeadline(1,"مهلت اعلان آزمایشی",today,JalaliDate.addDays(today,10),10,"ساختگی");
        java.util.List<OfficeDb.ReminderRecord> reminders=db.remindersFor("deadline",id);
        long first=reminders.get(0).id,second=reminders.get(1).id;
        long now=System.currentTimeMillis();
        db.getWritableDatabase().execSQL("UPDATE reminders SET trigger_at=? WHERE id=?",new Object[]{now-7200000,first});
        db.getWritableDatabase().execSQL("UPDATE reminders SET trigger_at=? WHERE id=?",new Object[]{now-3600000,second});
        boolean older=false,latest=false;
        for(OfficeDb.ReminderRecord r:db.pendingReminders()){older|=r.id==first;latest|=r.id==second;}
        assertFalse(older);assertTrue(latest);
        Intent intent=new Intent(context,ReminderReceiver.class).setAction("ir.kamranvahdati.lawoffice.REMINDER").putExtra("id",second).putExtra("kind","deadline");
        context.getSharedPreferences("office_profile",Context.MODE_PRIVATE).edit().putBoolean("notifications_enabled",false).commit();
        new ReminderReceiver().onReceive(context,intent);
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT fired_at FROM reminders WHERE id=?",new String[]{String.valueOf(second)})){assertTrue(c.moveToFirst());assertTrue(c.isNull(0));}
        context.getSharedPreferences("office_profile",Context.MODE_PRIVATE).edit().putBoolean("notifications_enabled",true).commit();
        new ReminderReceiver().onReceive(context,intent);
        new ReminderReceiver().onReceive(context,intent);
        for(int attempt=0;attempt<20&&manager.getActiveNotifications().length==0;attempt++)android.os.SystemClock.sleep(100);assertEquals(1,manager.getActiveNotifications().length);
        assertEquals((int)second,manager.getActiveNotifications()[0].getId());
        for(OfficeDb.ReminderRecord r:db.pendingReminders()){assertNotEquals(first,r.id);assertNotEquals(second,r.id);}
        manager.cancelAll();db.close();
    }
}
