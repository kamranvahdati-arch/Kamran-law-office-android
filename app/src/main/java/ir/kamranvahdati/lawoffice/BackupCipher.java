package ir.kamranvahdati.lawoffice;

import java.io.*;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/** Authenticated, ordered 1 MiB frames. The authenticated empty final frame prevents truncation. */
final class BackupCipher {
    private static final int BLOCK=1024*1024;
    private static final byte[] MAGIC={'V','K','B','3'};
    static void encrypt(File source,OutputStream destination,String password) throws Exception {
        if(password==null||password.length()<12)throw new IOException("Backup password too short");
        byte[] salt=new byte[16],prefix=new byte[8];SecureRandom random=new SecureRandom();
        random.nextBytes(salt);random.nextBytes(prefix);
        SecretKeySpec key=key(password,salt);
        DataOutputStream out=new DataOutputStream(destination);
        out.write(MAGIC);out.write(salt);out.write(prefix);
        try(InputStream in=new FileInputStream(source)) {
            byte[] block=new byte[BLOCK];int index=0;
            while(true){int size=0,n;while(size<BLOCK&&(n=in.read(block,size,BLOCK-size))!=-1)size+=n;
                byte[] encrypted=frame(Cipher.ENCRYPT_MODE,key,prefix,index++,size,block,size);
                out.writeInt(encrypted.length);out.write(encrypted);
                if(size==0)break;
                if(index==Integer.MAX_VALUE)throw new IOException("Backup too large");
            }
        }
        out.flush();
    }
    static void decrypt(InputStream source,File destination,String password) throws Exception {
        DataInputStream in=new DataInputStream(source);byte[] magic=new byte[4],salt=new byte[16],prefix=new byte[8];
        in.readFully(magic);if(!Arrays.equals(magic,MAGIC))throw new IOException("Unsupported backup format");
        in.readFully(salt);in.readFully(prefix);SecretKeySpec key=key(password,salt);
        boolean complete=false;
        try(FileOutputStream out=new FileOutputStream(destination)) {
            int index=0;
            while(true){int length=in.readInt();if(length<16||length>BLOCK+16)throw new IOException("Invalid frame");
                byte[] encrypted=new byte[length];in.readFully(encrypted);
                byte[] plain=frame(Cipher.DECRYPT_MODE,key,prefix,index++,length-16,encrypted,length);
                if(plain.length==0){if(in.read()!=-1)throw new IOException("Trailing data");break;}
                out.write(plain);
                if(index==Integer.MAX_VALUE)throw new IOException("Backup too large");
            }
            out.getFD().sync();complete=true;
        }finally{if(!complete)destination.delete();}
    }
    private static byte[] frame(int mode,SecretKeySpec key,byte[] prefix,int index,int size,byte[] bytes,int count)throws Exception{
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv=ByteBuffer.allocate(12).put(prefix).putInt(index).array();
        cipher.init(mode,key,new GCMParameterSpec(128,iv));
        cipher.updateAAD(ByteBuffer.allocate(12).put(MAGIC).putInt(index).putInt(size).array());
        return cipher.doFinal(bytes,0,count);
    }
    private static SecretKeySpec key(String password,byte[] salt)throws Exception{
        PBEKeySpec spec=new PBEKeySpec(password.toCharArray(),salt,210000,256);
        try{return new SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(),"AES");}
        finally{spec.clearPassword();}
    }
}
