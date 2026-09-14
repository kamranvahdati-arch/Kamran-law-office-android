package ir.kamranvahdati.lawoffice;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

final class SecureBackup {
    private static final byte[] MAGIC = {'K', 'L', 'O', '2'};

    static byte[] encrypt(String json, String code) throws Exception {
        byte[] salt = new byte[16];
        byte[] iv = new byte[12];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        random.nextBytes(iv);
        SecretKey key = key(code, salt);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(json.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(MAGIC);
        output.write(salt);
        output.write(iv);
        output.write(encrypted);
        return output.toByteArray();
    }

    static String decrypt(byte[] payload, String code) throws Exception {
        if (payload.length < 48) throw new IllegalArgumentException("فایل پشتیبان ناقص است");
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        byte[] magic = new byte[4];
        buffer.get(magic);
        for (int i = 0; i < 4; i++) if (magic[i] != MAGIC[i])
            throw new IllegalArgumentException("فرمت فایل پشتیبان معتبر نیست");
        byte[] salt = new byte[16];
        byte[] iv = new byte[12];
        buffer.get(salt);
        buffer.get(iv);
        byte[] encrypted = new byte[buffer.remaining()];
        buffer.get(encrypted);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(code, salt), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    private static SecretKey key(String code, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(code.toCharArray(), salt, 120000, 256);
        byte[] raw = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).getEncoded();
        spec.clearPassword();
        return new SecretKeySpec(raw, "AES");
    }
}
