package ir.kamranvahdati.lawoffice;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.security.MessageDigest;

/** Validate the persisted replacement before any deletion. Failed deletes are a nonfatal warning. */
final class WorkspaceBackups {
    static synchronized boolean save(Context c,File complete,String password,String day)throws Exception{
        if(day==null||!day.matches("\\d{4}-\\d{2}-\\d{2}"))throw new IOException("Invalid backup date");
        SimpleDateFormat dateCheck=new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT);dateCheck.setLenient(false);dateCheck.parse(day);
        Uri folder=new VokanoWorkspace(c).resolve("Backup",true);
        String name="VOKANO-"+day+"-"+UUID.randomUUID()+".vkb";
        Uri target=DocumentsContract.createDocument(c.getContentResolver(),folder,"application/octet-stream",name);
        if(target==null)throw new IOException("Cannot create backup");
        boolean valid=false;
        try{
            try(InputStream in=new FileInputStream(complete);OutputStream out=c.getContentResolver().openOutputStream(target,"w")){
                if(out==null)throw new IOException("Cannot write backup");byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)out.write(b,0,n);
            }
            byte[] expected;try(InputStream in=new FileInputStream(complete)){expected=digest(in);}
            try(InputStream in=c.getContentResolver().openInputStream(target)){if(!MessageDigest.isEqual(expected,digest(in)))throw new IOException("Persisted backup integrity failure");}
            try(InputStream in=c.getContentResolver().openInputStream(target);FullBackup.Prepared p=FullBackup.read(c,in,password)){/* Validated in an isolated database; never restore live data. */}
            valid=true;
        }finally{if(!valid)try{DocumentsContract.deleteDocument(c.getContentResolver(),target);}catch(Exception ignored){}}
        boolean warning=false;
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(folder,DocumentsContract.getDocumentId(folder));
        List<Uri> obsolete=new ArrayList<>();
        try(Cursor cursor=c.getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){
            if(cursor==null)return true;
            while(cursor.moveToNext()){
                String filename=cursor.getString(1);
                if(!obsolete(filename,day)||filename.equals(name))continue;
                obsolete.add(DocumentsContract.buildDocumentUriUsingTree(folder,cursor.getString(0)));
            }
        }catch(Exception unavailable){return true;}
        for(Uri uri:obsolete)try{if(!DocumentsContract.deleteDocument(c.getContentResolver(),uri))warning=true;}catch(Exception e){warning=true;}
        return warning;
    }
    private static byte[] digest(InputStream in)throws Exception{if(in==null)throw new IOException("Backup unavailable");MessageDigest hash=MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[32768];int n;while((n=in.read(buffer))!=-1)hash.update(buffer,0,n);return hash.digest();}
    static String today(){return new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(new Date());}
    static boolean obsolete(String filename,String day){
        if(filename==null||!filename.matches("VOKANO-\\d{4}-\\d{2}-\\d{2}-[0-9a-f-]{36}\\.vkb"))return false;
        try{SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT);format.setLenient(false);format.setTimeZone(TimeZone.getTimeZone("UTC"));
            long now=format.parse(day).getTime(),date=format.parse(filename.substring(7,17)).getTime();return date==now||date<now-29L*86400000L;
        }catch(Exception e){return false;}
    }
}
