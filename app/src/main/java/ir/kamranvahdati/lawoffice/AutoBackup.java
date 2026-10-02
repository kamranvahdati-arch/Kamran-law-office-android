package ir.kamranvahdati.lawoffice;

import android.app.job.*;
import android.content.*;
import android.security.keystore.*;
import android.util.Base64;
import org.json.JSONObject;
import java.io.*;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

/** Explicit opt-in only. Backup password is encrypted by a nonexportable device Keystore key. */
final class AutoBackup {
    private static final String PREF="vokano_auto_backup",KEY="VOKANO_AUTO_BACKUP_V1";
    static final int JOB=102071;
    private static boolean running;
    static boolean enabled(Context c){return c.getSharedPreferences(PREF,0).contains("credential");}
    static String status(Context c){return c.getSharedPreferences(PREF,0).getString("status","");}
    static void enable(Context c,String password)throws Exception{
        if(!new VokanoWorkspace(c).configured())throw new IOException("ابتدا فضای کاری را انتخاب کنید");
        if(password==null||password.length()<8)throw new IllegalArgumentException("رمز حداقل ۸ نویسه باشد");
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        if(!store.containsAlias(KEY)){KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");generator.init(new KeyGenParameterSpec.Builder(KEY,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());generator.generateKey();}
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,store.getKey(KEY,null));
        String encrypted=Base64.encodeToString(cipher.doFinal(password.getBytes("UTF-8")),Base64.NO_WRAP),iv=Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP);
        if(!c.getSharedPreferences(PREF,0).edit().putString("credential",encrypted).putString("iv",iv).commit())throw new IOException("Cannot save backup preference");
        schedule(c);start(c,null);
    }
    static void disable(Context c){c.getSharedPreferences(PREF,0).edit().clear().commit();JobScheduler scheduler=c.getSystemService(JobScheduler.class);if(scheduler!=null)scheduler.cancel(JOB);}
    static void schedule(Context c){if(!enabled(c))return;JobScheduler scheduler=c.getSystemService(JobScheduler.class);if(scheduler!=null)scheduler.schedule(new JobInfo.Builder(JOB,new ComponentName(c,AutoBackupJob.class)).setPersisted(true).setPeriodic(24L*60*60*1000).build());}
    static synchronized void start(Context c,Runnable finished){
        Context app=c.getApplicationContext();
        if(running||!enabled(app)||WorkspaceBackups.today().equals(app.getSharedPreferences(PREF,0).getString("last_day",""))){if(finished!=null)finished.run();return;}
        running=true;
        new Thread(()->{File complete=null;final String backupDay=WorkspaceBackups.today();try{
            android.content.SharedPreferences prefs=app.getSharedPreferences(PREF,0);
            KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,store.getKey(KEY,null),new GCMParameterSpec(128,Base64.decode(prefs.getString("iv",""),Base64.NO_WRAP)));
            String password=new String(cipher.doFinal(Base64.decode(prefs.getString("credential",""),Base64.NO_WRAP)),"UTF-8");
            complete=File.createTempFile("daily-", ".vkb",app.getCacheDir());
            try(OfficeDb db=new OfficeDb(app);OutputStream out=new FileOutputStream(complete)){FullBackup.write(app,bundle(app,db),out,password);}
            if(!enabled(app))return;
            boolean warning=WorkspaceBackups.save(app,complete,password,backupDay);
            prefs.edit().putString("last_day",backupDay).putString("status",warning?"پشتیبان ذخیره شد؛ حذف نسخه‌های قدیمی نیازمند اقدام دستی است":"پشتیبان روزانه ذخیره شد").commit();
        }catch(Exception failure){app.getSharedPreferences(PREF,0).edit().putString("status","پشتیبان خودکار انجام نشد؛ اتصال فضای کاری، فضای آزاد و تنظیمات رمز را بررسی کنید").commit();}
        finally{if(complete!=null)complete.delete();synchronized(AutoBackup.class){running=false;}if(finished!=null)finished.run();}},"vokano-daily-backup").start();
    }
    private static String bundle(Context c,OfficeDb db)throws Exception{
        JSONObject root=new JSONObject().put("format","KLO-BUNDLE-1").put("app_version",BuildConfig.VERSION_NAME).put("database",new JSONObject(db.exportJson()));
        JSONObject profile=new JSONObject();android.content.SharedPreferences prefs=c.getSharedPreferences("office_profile",0);
        for(String key:new String[]{"name","lawyer_level","professional_body","center","bar_branch","license","national_id","province","city","phone","office_phone","address","website","theme_id","categories","subjects","work_actions","personal_categories","deadline_types","sms_templates","instagram_url","telegram_url","photo","logo","font_scale","keep_screen_on","notifications_enabled","profile_complete"}){Object value=prefs.getAll().get(key);if(value!=null)profile.put(key,value);}
        return root.put("profile",profile).toString();
    }
}
