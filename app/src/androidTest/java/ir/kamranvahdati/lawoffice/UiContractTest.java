package ir.kamranvahdati.lawoffice;

import static org.junit.Assert.*;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.WindowInsets;
import android.graphics.Insets;
import android.widget.EditText;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UiContractTest {
    @Test public void darkShellKeepsSystemInsetsAndDateTimePickersDiscoverable() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("office_profile",Context.MODE_PRIVATE).edit()
                .putBoolean("notification_permission_prompted",true)
                .putString("theme_id",AppTheme.DARK).putBoolean("dark",true)
                .putString("name","وکیل آزمایشی").putString("professional_body","کانون وکلای دادگستری")
                .putString("province","تهران").putString("city","تهران")
                .putString("national_id","0013540831").putString("phone","09128402768")
                .putBoolean("profile_complete",true).putBoolean("lock_enabled",false).commit();
        Intent intent=new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity raw=InstrumentationRegistry.getInstrumentation().startActivitySync(intent);
        MainActivity activity=(MainActivity)raw;InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertNotNull(activity.root);assertTrue("top inset",activity.root.getPaddingTop()>0);assertTrue("bottom inset",activity.root.getPaddingBottom()>0);
        int flags=activity.getWindow().getDecorView().getSystemUiVisibility();assertEquals(0,flags&View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);assertEquals(0,flags&View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            assertEquals("وکانو | VOKANO",activity.title.getText().toString());
            assertEquals("سامانه هوشمند وکلای نوین",activity.subtitle.getText().toString());
            assertTrue("brand is above product subtitle",activity.title.getTop()<activity.subtitle.getTop());
            View insetTarget=new View(activity);activity.safeArea(insetTarget);
            insetTarget.dispatchApplyWindowInsets(new WindowInsets.Builder().setSystemWindowInsets(Insets.of(31,42,27,48)).build());
            assertEquals(31,insetTarget.getPaddingLeft());assertEquals(27,insetTarget.getPaddingRight());
            assertEquals(42+activity.dp(5),insetTarget.getPaddingTop());assertEquals(48+activity.dp(5),insetTarget.getPaddingBottom());
            EditText date=activity.input("تاریخ"),time=activity.input("ساعت");activity.bindJalaliPicker(date);activity.bindTimePicker(time);assertNotNull(date.getCompoundDrawables()[2]);assertNotNull(time.getCompoundDrawables()[2]);
        });
        ActivityInfo info=context.getPackageManager().getActivityInfo(activity.getComponentName(),PackageManager.GET_META_DATA);assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,info.screenOrientation);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(activity::finish);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
}
