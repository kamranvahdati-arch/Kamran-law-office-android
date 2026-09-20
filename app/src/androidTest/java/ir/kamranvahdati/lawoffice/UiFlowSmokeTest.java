package ir.kamranvahdati.lawoffice;

import static org.junit.Assert.*;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UiFlowSmokeTest {
    @Test public void actualScreensAndDialogsOpenInEveryTheme() throws Exception {
        Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        Context context=instrumentation.getTargetContext();
        for(String theme:AppTheme.ids()){
            context.getSharedPreferences("office_profile",Context.MODE_PRIVATE).edit()
              .putString("theme_id",theme).putBoolean("profile_complete",true)
              .putString("name","وکیل فرضی").putString("professional_body","کانون وکلای دادگستری")
              .putString("province","تهران").putString("city","تهران")
              .putString("national_id","0013540831").putString("phone","09120000001")
              .putBoolean("notification_permission_prompted",true).putBoolean("lock_enabled",false).commit();
            MainActivity activity=(MainActivity)instrumentation.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            try {
                instrumentation.waitForIdleSync();
                assertNotNull(activity.page);assertEquals(View.LAYOUT_DIRECTION_RTL,activity.root.getLayoutDirection());
                capture(instrumentation,activity,theme+"-dashboard");
                for(Runnable screen:new Runnable[]{activity::settings,activity::casesHub,activity::reports,activity::contact,()->activity.appointmentList(null),()->activity.deadlineList(null)}){
                    instrumentation.runOnMainSync(screen);instrumentation.waitForIdleSync();assertTrue(activity.page.getChildCount()>0);
                }
                instrumentation.runOnMainSync(()->activity.addAppointment(JalaliDate.today().value()));
                awaitActiveWindow(instrumentation,"appointment dialog: "+theme);
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
                instrumentation.runOnMainSync(()->activity.showTimePicker(activity.input("زمان آزمون")));
                awaitActiveWindow(instrumentation,"time picker: "+theme);
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
                instrumentation.runOnMainSync(()->activity.showJalaliPicker(activity.input("تاریخ آزمون")));
                awaitActiveWindow(instrumentation,"Jalali picker: "+theme);
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
                instrumentation.runOnMainSync(activity::settings);instrumentation.waitForIdleSync();
                capture(instrumentation,activity,theme+"-settings");
            } finally {instrumentation.runOnMainSync(activity::finish);instrumentation.waitForIdleSync();}
        }
    }
    private void awaitActiveWindow(Instrumentation instrumentation,String label) throws Exception {
        instrumentation.waitForIdleSync();
        long deadline=android.os.SystemClock.uptimeMillis()+5000;
        do {
            android.view.accessibility.AccessibilityNodeInfo root=instrumentation.getUiAutomation().getRootInActiveWindow();
            if(root!=null){root.recycle();return;}
            Thread.sleep(100);
        } while(android.os.SystemClock.uptimeMillis()<deadline);
        fail("No accessible active window after 5 seconds: "+label);
    }
    private void capture(Instrumentation instrumentation,MainActivity activity,String name) throws Exception {
        Bitmap[] bitmap=new Bitmap[1];
        instrumentation.runOnMainSync(()->{assertTrue(activity.root.getWidth()>0);bitmap[0]=Bitmap.createBitmap(activity.root.getWidth(),activity.root.getHeight(),Bitmap.Config.ARGB_8888);activity.root.draw(new Canvas(bitmap[0]));});
        File folder=new File(activity.getExternalFilesDir(null),"qa");assertTrue(folder.isDirectory()||folder.mkdirs());
        File file=new File(folder,name+"-"+bitmap[0].getWidth()+".png");
        try(FileOutputStream stream=new FileOutputStream(file)){assertTrue(bitmap[0].compress(Bitmap.CompressFormat.PNG,100,stream));}finally{bitmap[0].recycle();}
    }
}
