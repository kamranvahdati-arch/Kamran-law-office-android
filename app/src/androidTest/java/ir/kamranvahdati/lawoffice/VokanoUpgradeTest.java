package ir.kamranvahdati.lawoffice;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Run explicitly after replacing a populated 9.1 APK with the version 10 APK on one emulator. */
@RunWith(AndroidJUnit4.class)
public class VokanoUpgradeTest {
    @Test public void samePackageUpgradeRetainsExistingAppointmentAndDatabaseVersion() {
        assumeTrue("Requires seeded 9.1 installed before 10, see build-apk.yml",
                "true".equalsIgnoreCase(InstrumentationRegistry.getArguments().getString("upgrade_mode","")));
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        OfficeDb db=new OfficeDb(context);
        try {
            assertEquals(13,db.getReadableDatabase().getVersion());
            boolean persisted=false;
            for(OfficeDb.AppointmentRecord appointment:db.appointments(null))
                if("مراجعه به دادگاه".equals(appointment.kind)&&"کرج".equals(appointment.city)
                        &&"13:25".equals(appointment.start)&&"۲".equals(appointment.branch))
                    persisted=true;
            assertTrue("9.1 appointment lost during APK upgrade",persisted);
        } finally {db.close();}
    }
}
