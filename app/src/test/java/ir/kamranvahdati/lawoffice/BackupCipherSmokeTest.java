package ir.kamranvahdati.lawoffice;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
public class BackupCipherSmokeTest {
 public static void main(String[] args)throws Exception{
  File plain=File.createTempFile("backup-test", ".bin"),restored=File.createTempFile("restore-test", ".bin");
  String password="test-only-backup-password";byte[] bytes=new byte[3*1024*1024+17];new Random(23).nextBytes(bytes);Files.write(plain.toPath(),bytes);
  ByteArrayOutputStream output=new ByteArrayOutputStream();BackupCipher.encrypt(plain,output,password);byte[] encrypted=output.toByteArray();
  BackupCipher.decrypt(new ByteArrayInputStream(encrypted),restored,password);if(!Arrays.equals(bytes,Files.readAllBytes(restored.toPath())))throw new AssertionError("roundtrip");
  for(int mode=0;mode<4;mode++){byte[] bad=encrypted.clone();String code=password;
   if(mode==0)bad[bad.length/2]^=1;if(mode==1)bad=Arrays.copyOf(bad,bad.length-20);if(mode==2)code="wrong-password";if(mode==3)bad=Arrays.copyOf(bad,bad.length+1);
   boolean rejected=false;try{BackupCipher.decrypt(new ByteArrayInputStream(bad),restored,code);}catch(Exception e){rejected=true;}
   if(!rejected)throw new AssertionError("accepted corrupt backup "+mode);
  }
  plain.delete();restored.delete();System.out.println("Backup cipher: multi-frame roundtrip, tamper, truncation, wrong password, trailing bytes PASS");
 }
}
