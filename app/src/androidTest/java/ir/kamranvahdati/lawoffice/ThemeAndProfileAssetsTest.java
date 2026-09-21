package ir.kamranvahdati.lawoffice;

import static org.junit.Assert.*;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ThemeAndProfileAssetsTest {
    @Test public void everyPresetHasReadableCoreContrast() {
        for (String id : AppTheme.ids()) {
            AppTheme theme=AppTheme.from(id);
            assertTrue(id+" text/background",contrast(theme.text,theme.background)>=4.5);
            assertTrue(id+" text/card",contrast(theme.text,theme.card)>=4.5);
            assertTrue(id+" muted/background",contrast(theme.muted,theme.background)>=4.5);
            assertTrue(id+" muted/card",contrast(theme.muted,theme.card)>=4.5);
            assertTrue(id+" on-primary",contrast(theme.onPrimary,theme.primary)>=4.5);
        }
    }

    @Test public void approvedLogosAreBundledAndProvinceListIsComplete() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertNotNull(BitmapFactory.decodeResource(context.getResources(),R.drawable.logo_bar_association));
        assertNotNull(BitmapFactory.decodeResource(context.getResources(),R.drawable.logo_judiciary_center));
        assertEquals(31,IranLocations.provinces().length);
        for(String province:IranLocations.provinces())assertTrue(province,IranLocations.cities(province).length>0);
    }

    private static double contrast(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
    private static double luminance(int color){return .2126*channel(Color.red(color))+.7152*channel(Color.green(color))+.0722*channel(Color.blue(color));}
    private static double channel(int value){double x=value/255d;return x<=.04045?x/12.92:Math.pow((x+.055)/1.055,2.4);}
}
