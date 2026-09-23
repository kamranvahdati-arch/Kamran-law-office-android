package ir.kamranvahdati.lawoffice;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

/** Copy source bytes before changing a database URI; never remove the source document. */
final class MediaStorage {
    static Uri copy(Context context,Uri source) throws Exception {
        File dir=new File(context.getFilesDir(),"office-media");
        if(!dir.isDirectory()&&!dir.mkdirs())throw new java.io.IOException("Cannot create media directory");
        String id=UUID.randomUUID().toString().replace("-","");
        File target=new File(dir,id),tmp=new File(dir,id+".tmp");
        long length=0;
        try(InputStream in=context.getContentResolver().openInputStream(source);
            FileOutputStream out=new FileOutputStream(tmp)) {
            if(in==null)throw new java.io.FileNotFoundException(source.toString());
            byte[] buf=new byte[32768];int n;
            while((n=in.read(buf))!=-1){length+=n;out.write(buf,0,n);}
            out.getFD().sync();
        }catch(Exception e){tmp.delete();throw e;}
        if(length==0){tmp.delete();throw new java.io.IOException("Empty source document");}
        if(!tmp.renameTo(target)){tmp.delete();throw new java.io.IOException("Cannot finish media copy");}
        return new Uri.Builder().scheme("content").authority(context.getPackageName()+".media").appendPath(id).build();
    }
    static boolean readable(Context context,String value) {
        if(value==null||value.isEmpty())return true;
        try(InputStream in=context.getContentResolver().openInputStream(Uri.parse(value))){return in!=null&&in.read()!=-1;}
        catch(Exception e){return false;}
    }
}
