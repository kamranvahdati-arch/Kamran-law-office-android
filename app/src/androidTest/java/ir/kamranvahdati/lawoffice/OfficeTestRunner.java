package ir.kamranvahdati.lawoffice;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import androidx.test.runner.AndroidJUnitRunner;

/** Database fixture replacement must not race an OS alarm receiver in the test process. */
public final class OfficeTestRunner extends AndroidJUnitRunner {
    private ComponentName receiver;
    private int previousState;

    @Override public void onStart() {
        receiver=new ComponentName(getTargetContext(),ReminderReceiver.class);
        PackageManager manager=getTargetContext().getPackageManager();
        previousState=manager.getComponentEnabledSetting(receiver);
        manager.setComponentEnabledSetting(receiver,PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP);
        // ReminderDeliveryTest still calls onReceive directly to verify real notifications,
        // permission checks and deduplication. Alarm timing/reboot remains a separate QA gate.
        super.onStart();
    }

    @Override public void onDestroy() {
        if(receiver!=null)getTargetContext().getPackageManager().setComponentEnabledSetting(
            receiver,previousState,PackageManager.DONT_KILL_APP);
        super.onDestroy();
    }
}
