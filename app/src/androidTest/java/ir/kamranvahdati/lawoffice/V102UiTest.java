package ir.kamranvahdati.lawoffice;

import static org.junit.Assert.*;
import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class V102UiTest {
 @Test public void scopedScreensAndSmsHandoff() throws Exception {
  Instrumentation ins=InstrumentationRegistry.getInstrumentation();Context c=ins.getTargetContext();
  assertEquals(android.content.pm.PackageManager.PERMISSION_DENIED,c.checkSelfPermission("android.permission.SEND_SMS"));
  for(String theme:new String[]{AppTheme.LIGHT,AppTheme.DARK}){
   c.getSharedPreferences("office_profile",0).edit().putString("theme_id",theme).putBoolean("lock_enabled",false).commit();
   MainActivity a=(MainActivity)ins.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));ins.waitForIdleSync();
   try{
    for(int route:new int[]{MainActivity.HOME,MainActivity.SMS,MainActivity.BACKUP,MainActivity.LETTER,MainActivity.ABOUT,MainActivity.CONTACT}){
     ins.runOnMainSync(()->{a.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);a.go(route,false);});ins.waitForIdleSync();
     assertEquals(View.LAYOUT_DIRECTION_RTL,a.root.getLayoutDirection());assertTrue(a.page.getChildCount()>0);
     Bitmap bitmap=ins.getUiAutomation().takeScreenshot();assertNotNull(bitmap);File folder=new File(c.getExternalFilesDir(null),"qa/v102");assertTrue(folder.isDirectory()||folder.mkdirs());
     try(FileOutputStream out=new FileOutputStream(new File(folder,theme+"-route-"+route+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}finally{bitmap.recycle();}
    }
    final Intent[] captured={null};
    Instrumentation.ActivityMonitor monitor=new Instrumentation.ActivityMonitor(){@Override public Instrumentation.ActivityResult onStartActivity(Intent intent){if(Intent.ACTION_SENDTO.equals(intent.getAction())){captured[0]=new Intent(intent);return new Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null);}return null;}};
    ins.addMonitor(monitor);
    try{
     ins.runOnMainSync(()->a.composeSms("09120000001","پیام آزمایشی؛ ارسال واقعی ممنوع"));ins.waitForIdleSync();
     AccessibilityNodeInfo root=ins.getUiAutomation().getRootInActiveWindow();assertNotNull(root);
     List<AccessibilityNodeInfo> buttons=root.findAccessibilityNodeInfosByText("باز کردن برنامه پیامک");assertFalse(buttons.isEmpty());assertTrue(buttons.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK));
     long until=android.os.SystemClock.uptimeMillis()+3000;while(captured[0]==null&&android.os.SystemClock.uptimeMillis()<until)Thread.sleep(50);
     assertNotNull("SMS app handoff intercepted; nothing sent",captured[0]);assertEquals("smsto",captured[0].getData().getScheme());assertEquals("پیام آزمایشی؛ ارسال واقعی ممنوع",captured[0].getStringExtra("sms_body"));
     root.recycle();for(AccessibilityNodeInfo node:buttons)node.recycle();
    }finally{ins.removeMonitor(monitor);}
   }finally{ins.runOnMainSync(a::finish);ins.waitForIdleSync();}
  }
 }
}
