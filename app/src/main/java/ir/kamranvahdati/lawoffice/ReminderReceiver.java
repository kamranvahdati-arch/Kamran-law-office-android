package ir.kamranvahdati.lawoffice;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

final public class ReminderReceiver extends BroadcastReceiver {
    private static final String ACTION_REMINDER="ir.kamranvahdati.lawoffice.REMINDER";
    private static final String CHANNEL="case_deadlines";

    static void schedule(Context context) {
        if(!context.getSharedPreferences("office_profile",Context.MODE_PRIVATE).getBoolean("notifications_enabled",true))return;
        AlarmManager alarms=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if(alarms==null)return;
        OfficeDb db=new OfficeDb(context);
        db.ensureReminderRules();
        for(OfficeDb.ReminderRecord r:db.pendingReminders()) {
            Intent intent=new Intent(context,ReminderReceiver.class).setAction(ACTION_REMINDER).putExtra("id",r.id).putExtra("days",r.advanceDays).putExtra("kind",r.kind);
            PendingIntent pending=PendingIntent.getBroadcast(context,(int)r.id,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,r.at,pending);
        }
        db.close();
    }

    @Override public void onReceive(Context context,Intent intent) {
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){schedule(context);return;}
        if(!ACTION_REMINDER.equals(intent.getAction()))return;
        if(!context.getSharedPreferences("office_profile",Context.MODE_PRIVATE).getBoolean("notifications_enabled",true))return;
        long id=intent.getLongExtra("id",-1);if(id<0)return;
        if(Build.VERSION.SDK_INT>=33&&context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);if(manager==null)return;
        if(!manager.areNotificationsEnabled())return;
        if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(new NotificationChannel(CHANNEL,"یادآوری‌های دفتر وکالت",NotificationManager.IMPORTANCE_HIGH));
        if(Build.VERSION.SDK_INT>=26&&manager.getNotificationChannel(CHANNEL).getImportance()==NotificationManager.IMPORTANCE_NONE)return;
        OfficeDb db=new OfficeDb(context);boolean valid;String reminderLabel;try{reminderLabel=db.reminderLabel(id);valid=db.fireReminder(id);}finally{db.close();}if(!valid)return;
        int days=intent.getIntExtra("days",0);String kind=intent.getStringExtra("kind");String label="deadline".equals(kind)?"مهلت پرونده":"appointment".equals(kind)?"قرار یا جلسه":"installment".equals(kind)?"سررسید قسط":"check".equals(kind)?"سررسید چک":"کار برنامه‌ریزی‌شده";
        Intent open=new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent click=PendingIntent.getActivity(context,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,CHANNEL):new Notification.Builder(context);
        Notification n=builder.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(reminderLabel==null||reminderLabel.isEmpty()?"یادآوری "+label:reminderLabel).setContentText(days==0?"زمان یادآوری ثبت‌شده فرا رسیده؛ برنامه را بررسی کنید":"زمان ثبت‌شده تا "+days+" روز دیگر نزدیک می‌شود؛ برنامه را بررسی کنید").setContentIntent(click).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build();
        manager.notify((int)id,n);
    }
}
