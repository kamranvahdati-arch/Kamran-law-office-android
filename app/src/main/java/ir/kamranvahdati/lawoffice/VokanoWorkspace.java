package ir.kamranvahdati.lawoffice;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/** User-owned SAF tree. The database stores portable provider references, never tree/document IDs. */
final class VokanoWorkspace {
    private final Context context;
    VokanoWorkspace(Context c){context=c.getApplicationContext();}
    boolean configured(){return !context.getSharedPreferences("workspace",0).getString("tree","").isEmpty();}
    void select(Uri tree,int flags)throws Exception{
        context.getContentResolver().takePersistableUriPermission(tree,flags&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
        Uri root=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
        for(String name:new String[]{"Backup","Cases","Pleadings","Accounting","Exports"})child(root,name,true);
        context.getSharedPreferences("workspace",0).edit().putString("tree",tree.toString()).commit();
    }
    Uri root()throws IOException{
        String value=context.getSharedPreferences("workspace",0).getString("tree","");
        if(value.isEmpty())throw new IOException("ابتدا پوشه VOKANO را انتخاب کنید");
        Uri tree=Uri.parse(value);return DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
    }
    Uri resolve(String relative,boolean directories)throws Exception{
        if(relative==null||!relative.matches("[A-Za-z0-9._/-]+")||relative.endsWith("/"))throw new IOException("Invalid workspace path");
        Uri node=root();for(String part:relative.split("/")){if(part.isEmpty()||part.equals(".")||part.equals(".."))throw new IOException("Invalid path");node=child(node,part,directories);if(node==null)throw new FileNotFoundException("فایل در فضای کاری یافت نشد: "+relative);}return node;
    }
    private Uri child(Uri parent,String name,boolean create)throws Exception{
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(parent,DocumentsContract.getDocumentId(parent));
        try(Cursor c=context.getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){
            if(c==null)throw new IOException("Workspace provider unavailable");
            while(c.moveToNext())if(name.equals(c.getString(1)))return DocumentsContract.buildDocumentUriUsingTree(parent,c.getString(0));
        }
        if(!create)return null;
        Uri result=DocumentsContract.createDocument(context.getContentResolver(),parent,DocumentsContract.Document.MIME_TYPE_DIR,name);
        if(result==null)throw new IOException("Cannot create workspace folder");return result;
    }
    void ensureCase(String uid)throws Exception{if(uid==null||!uid.matches("[A-Za-z0-9-]+"))throw new IOException("Invalid case identity");resolve("Cases/"+uid,true);}
    Uri copy(Uri source,String directory)throws Exception{
        Uri parent=resolve(directory,true);String name=UUID.randomUUID().toString().replace("-","");
        Uri target=DocumentsContract.createDocument(context.getContentResolver(),parent,"application/octet-stream",name);
        if(target==null)throw new IOException("Cannot create workspace document");
        try{
            MessageDigest written=MessageDigest.getInstance("SHA-256");long size=0;
            try(InputStream in=context.getContentResolver().openInputStream(source);OutputStream out=context.getContentResolver().openOutputStream(target,"w")){
                if(in==null||out==null)throw new IOException("Document unavailable");byte[] b=new byte[32768];int n;
                while((n=in.read(b))!=-1){out.write(b,0,n);written.update(b,0,n);size+=n;}
            }
            if(size==0)throw new IOException("Empty document");MessageDigest read=MessageDigest.getInstance("SHA-256");
            try(InputStream in=context.getContentResolver().openInputStream(target)){if(in==null)throw new IOException("Cannot validate document");byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)read.update(b,0,n);}
            if(!MessageDigest.isEqual(written.digest(),read.digest()))throw new IOException("Workspace copy integrity failure");
            return reference(directory+"/"+name);
        }catch(Exception e){try{DocumentsContract.deleteDocument(context.getContentResolver(),target);}catch(Exception ignored){}throw e;}
    }
    private static boolean validPath(String path){
        if(path==null||!path.matches("[A-Za-z0-9._/-]+"))return false;
        String[] parts=path.split("/",-1);
        if(parts.length<2||!Arrays.asList("Cases","Pleadings","Accounting","Exports").contains(parts[0]))return false;
        for(String part:parts)if(part.isEmpty()||part.equals(".")||part.equals(".."))return false;
        return true;
    }
    Uri reference(String path){if(!validPath(path))throw new IllegalArgumentException("Invalid workspace reference");return canonical(path);}
    private static Uri canonical(String path){return new Uri.Builder().scheme("content").authority(BuildConfig.APPLICATION_ID+".media").appendPath("workspace").appendPath(path).build();}
    static boolean isReference(Uri uri){
        if(uri==null||!"content".equals(uri.getScheme())||!(BuildConfig.APPLICATION_ID+".media").equals(uri.getAuthority())||uri.getPathSegments().size()!=2||!"workspace".equals(uri.getPathSegments().get(0)))return false;
        String path=uri.getPathSegments().get(1);return validPath(path)&&canonical(path).equals(uri);
    }
    Uri resolveReference(Uri uri)throws Exception{if(!isReference(uri))throw new IOException("Invalid workspace reference");return resolve(uri.getPathSegments().get(1),false);}
    void migrateProfileMedia()throws Exception{
        android.content.SharedPreferences prefs=context.getSharedPreferences("office_profile",0);
        for(String key:new String[]{"photo","logo"}){
            String value=prefs.getString(key,"");if(value.isEmpty()||isReference(Uri.parse(value)))continue;
            Uri copied=copy(Uri.parse(value),"Exports/Profile");
            if(!prefs.edit().putString(key,copied.toString()).commit())throw new IOException("Cannot save profile workspace reference");
        }
    }
    void migrateAttachments(OfficeDb db)throws Exception{
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT a.id,a.content_uri,c.uid FROM case_attachments a JOIN cases c ON c.id=a.case_id",null)){
            while(c.moveToNext()){ensureCase(c.getString(2));Uri old=Uri.parse(c.getString(1));if(isReference(old))continue;
                Uri copied=copy(old,"Cases/"+c.getString(2));db.updateCaseAttachmentUri(c.getLong(0),copied.toString());
            }
        }
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT uid FROM cases",null)){while(c.moveToNext())ensureCase(c.getString(0));}
    }
}
