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
        AlarmManager alarms=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if(alarms==null)return;
        OfficeDb db=new OfficeDb(context);
        for(OfficeDb.ReminderRecord r:db.pendingReminders()) {
            Intent intent=new Intent(context,ReminderReceiver.class).setAction(ACTION_REMINDER).putExtra("id",r.id).putExtra("days",r.advanceDays);
            PendingIntent pending=PendingIntent.getBroadcast(context,(int)r.id,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,r.at,pending);
        }
        db.close();
    }

    @Override public void onReceive(Context context,Intent intent) {
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){schedule(context);return;}
        if(!ACTION_REMINDER.equals(intent.getAction()))return;
        long id=intent.getLongExtra("id",-1);if(id<0)return;
        OfficeDb db=new OfficeDb(context);boolean valid=db.fireReminder(id);db.close();if(!valid)return;
        if(Build.VERSION.SDK_INT>=33&&context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);if(manager==null)return;
        if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(new NotificationChannel(CHANNEL,"مهلت‌های قضایی",NotificationManager.IMPORTANCE_HIGH));
        int days=intent.getIntExtra("days",0);
        Intent open=new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent click=PendingIntent.getActivity(context,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,CHANNEL):new Notification.Builder(context);
        Notification n=builder.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("یادآوری مهلت پرونده").setContentText(days==0?"امروز تاریخ مهلت ثبت‌شده است؛ پرونده را بررسی کنید":"مهلت ثبت‌شده تا "+days+" روز دیگر نزدیک می‌شود؛ پرونده را بررسی کنید").setContentIntent(click).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build();
        manager.notify((int)id,n);
    }
}
