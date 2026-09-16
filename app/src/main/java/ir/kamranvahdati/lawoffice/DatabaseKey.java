package ir.kamranvahdati.lawoffice;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.security.KeyStore;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class DatabaseKey {
    private static final String ALIAS="law_office_database_wrap_v1";
    private static final String PREF="database_secret";

    static byte[] read(Context context) {
        try {
            SharedPreferences prefs=context.getSharedPreferences(PREF,Context.MODE_PRIVATE);
            String wrapped=prefs.getString("wrapped_key",null);
            KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
            SecretKey key=(SecretKey)store.getKey(ALIAS,null);
            if(wrapped==null) {
                if(context.getDatabasePath(OfficeDb.ENCRYPTED_NAME).exists())throw new IllegalStateException("کلید دیتابیس یافت نشد؛ از فایل پشتیبان بازیابی کنید");
                if(key==null){KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");generator.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());key=generator.generateKey();}
                byte[] secret=new byte[32];new SecureRandom().nextBytes(secret);
                Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key);
                byte[] iv=cipher.getIV(),body=cipher.doFinal(secret),packed=new byte[iv.length+body.length];System.arraycopy(iv,0,packed,0,iv.length);System.arraycopy(body,0,packed,iv.length,body.length);
                if(!prefs.edit().putString("wrapped_key",Base64.encodeToString(packed,Base64.NO_WRAP)).commit())throw new IllegalStateException("ذخیره کلید دیتابیس ناموفق بود");
                return secret;
            }
            if(key==null)throw new IllegalStateException("کلید امن گوشی یافت نشد؛ از پشتیبان بازیابی کنید");
            byte[] packed=Base64.decode(wrapped,Base64.NO_WRAP);if(packed.length<29)throw new IllegalStateException("کلید دیتابیس خراب است");
            byte[] iv=new byte[12],body=new byte[packed.length-12];System.arraycopy(packed,0,iv,0,12);System.arraycopy(packed,12,body,0,body.length);
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,iv));byte[] secret=cipher.doFinal(body);
            if(secret.length!=32)throw new IllegalStateException("طول کلید دیتابیس معتبر نیست");return secret;
        }catch(Exception e){throw new IllegalStateException("باز کردن دیتابیس رمزگذاری‌شده ممکن نیست",e);}
    }
}
