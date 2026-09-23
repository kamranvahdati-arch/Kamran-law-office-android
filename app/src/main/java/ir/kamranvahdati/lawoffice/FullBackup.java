package ir.kamranvahdati.lawoffice;

import android.content.Context;
import android.net.Uri;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;

/** Files are authenticated and checked before live records are changed. No missing media is skipped. */
final class FullBackup {
    private static final long MAX_MANIFEST=32L*1024*1024;
    static void write(Context context,String bundle,OutputStream destination,String password)throws Exception{
        File archive=File.createTempFile("backup-", ".zip",context.getCacheDir());
        try{
            JSONObject root=new JSONObject(bundle);root.put("format","VOKANO-FULL-1");root.put("schema",13);
            LinkedHashSet<String> uris=references(root);
            File media=new File(context.getFilesDir(),"office-media");File[] owned=media.listFiles();
            if(owned!=null)for(File file:owned)if(file.isFile()&&file.getName().matches("[0-9a-f]{32}"))uris.add(uri(context,file.getName()).toString());
            JSONArray files=new JSONArray();
            try(ZipOutputStream zip=new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(archive)))){
                for(String original:uris){String id=UUID.randomUUID().toString().replace("-","");
                    MessageDigest hash=MessageDigest.getInstance("SHA-256");long size;
                    zip.putNextEntry(new ZipEntry("media/"+id));
                    try(InputStream in=context.getContentResolver().openInputStream(Uri.parse(original))){
                        if(in==null)throw new IOException("Unreadable attachment");size=copy(in,zip,hash,Long.MAX_VALUE);
                    }
                    zip.closeEntry();if(size==0)throw new IOException("Empty attachment");
                    files.put(new JSONObject().put("uri",original).put("id",id).put("size",size).put("sha256",hex(hash.digest())));
                }
                root.put("media",files);byte[] json=root.toString().getBytes(StandardCharsets.UTF_8);
                if(json.length>MAX_MANIFEST)throw new IOException("Database backup exceeds supported size");
                zip.putNextEntry(new ZipEntry("bundle.json"));zip.write(json);zip.closeEntry();
            }
            BackupCipher.encrypt(archive,destination,password);
        }finally{archive.delete();}
    }
    static Prepared read(Context context,InputStream source,String password)throws Exception{
        File stage=new File(context.getCacheDir(),"restore-"+UUID.randomUUID());
        if(!stage.mkdirs())throw new IOException("Cannot stage restore");
        ArrayList<File> installed=new ArrayList<>();boolean prepared=false;
        try{
            File archive=new File(stage,"archive");BackupCipher.decrypt(source,archive,password);
            JSONObject root=null;Map<String,File> extracted=new HashMap<>();
            // ZipFile also checks the central directory (a truncated ZIP is rejected).
            try(ZipFile zip=new ZipFile(archive)){
                Set<String> seen=new HashSet<>();Enumeration<? extends ZipEntry> entries=zip.entries();
                while(entries.hasMoreElements()){
                    ZipEntry entry=entries.nextElement();String name=entry.getName();
                    if(!seen.add(name)||entry.isDirectory())throw new IOException("Duplicate/invalid archive entry");
                    if(name.equals("bundle.json")){
                        ByteArrayOutputStream json=new ByteArrayOutputStream();
                        try(InputStream in=zip.getInputStream(entry)){copy(in,json,null,MAX_MANIFEST);}
                        root=new JSONObject(json.toString("UTF-8"));
                    }else if(name.matches("media/[0-9a-f]{32}")){
                        File file=new File(stage,name.substring(6));
                        if(entry.getSize()<1||entry.getSize()>stage.getUsableSpace()-16*1024*1024)throw new IOException("Insufficient storage or invalid media");
                        try(InputStream in=zip.getInputStream(entry);FileOutputStream out=new FileOutputStream(file)){copy(in,out,null,entry.getSize());out.getFD().sync();}
                        extracted.put(name.substring(6),file);
                    }else throw new IOException("Unexpected archive entry");
                }
            }
            if(root==null||!"VOKANO-FULL-1".equals(root.optString("format"))||root.getInt("schema")!=13)throw new IOException("Unsupported backup schema");
            JSONArray files=root.getJSONArray("media");Map<String,String> mapped=new HashMap<>();Set<String> ids=new HashSet<>();
            if(files.length()!=extracted.size())throw new IOException("Missing media");
            for(int i=0;i<files.length();i++){
                JSONObject record=files.getJSONObject(i);String id=record.getString("id"),original=record.getString("uri");File file=extracted.get(id);
                if(file==null||!ids.add(id)||mapped.containsKey(original)||file.length()!=record.getLong("size"))throw new IOException("Invalid media manifest");
                MessageDigest hash=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){copy(in,new OutputStream(){public void write(int b){}public void write(byte[] b,int o,int n){}},hash,Long.MAX_VALUE);}
                if(!hex(hash.digest()).equals(record.getString("sha256")))throw new IOException("Attachment integrity failure");
                mapped.put(original,id);
            }
            for(String ref:references(root))if(!mapped.containsKey(ref))throw new IOException("Unresolved attachment reference");
            // Use fresh private names; existing files stay untouched even if DB validation later fails.
            Map<String,String> replacements=new HashMap<>();File dir=new File(context.getFilesDir(),"office-media");
            if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cannot create media directory");
            for(Map.Entry<String,String> mapping:mapped.entrySet()){
                String id=UUID.randomUUID().toString().replace("-","");File target=new File(dir,id);
                if(!extracted.get(mapping.getValue()).renameTo(target))throw new IOException("Cannot install restored media");
                installed.add(target);replacements.put(mapping.getKey(),uri(context,id).toString());
            }
            JSONArray attachments=root.getJSONObject("database").getJSONObject("tables").getJSONArray("case_attachments");
            for(int i=0;i<attachments.length();i++){JSONObject row=attachments.getJSONObject(i);String old=row.optString("content_uri","");if(!old.isEmpty())row.put("content_uri",replacements.get(old));}
            JSONObject profile=root.getJSONObject("profile");for(String key:new String[]{"photo","logo"}){String old=profile.optString(key,"");if(!old.isEmpty())profile.put(key,replacements.get(old));}
            root.put("format","KLO-BUNDLE-1");prepared=true;return new Prepared(root.toString(),installed);
        }finally{removeTree(stage);if(!prepared)for(File file:installed)file.delete();}
    }
    static final class Prepared implements AutoCloseable{
        final String bundle;private final List<File> files;private boolean committed;
        Prepared(String bundle,List<File> files){this.bundle=bundle;this.files=files;}
        void commit(){committed=true;}
        @Override public void close(){if(!committed)for(File file:files)file.delete();}
    }
    private static LinkedHashSet<String> references(JSONObject root)throws Exception{
        LinkedHashSet<String> values=new LinkedHashSet<>();
        JSONArray rows=root.getJSONObject("database").getJSONObject("tables").getJSONArray("case_attachments");
        for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);if(!row.isNull("content_uri")){String s=row.optString("content_uri","");if(!s.isEmpty())values.add(s);}}
        JSONObject profile=root.getJSONObject("profile");for(String key:new String[]{"photo","logo"}){String s=profile.optString(key,"");if(!s.isEmpty())values.add(s);}
        return values;
    }
    private static Uri uri(Context context,String id){return new Uri.Builder().scheme("content").authority(context.getPackageName()+".media").appendPath(id).build();}
    private static long copy(InputStream in,OutputStream out,MessageDigest hash,long limit)throws Exception{
        byte[] bytes=new byte[32768];long size=0;int n;
        while((n=in.read(bytes))!=-1){size+=n;if(size>limit)throw new IOException("Archive size limit exceeded");out.write(bytes,0,n);if(hash!=null)hash.update(bytes,0,n);}return size;
    }
    private static String hex(byte[] bytes){StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
    static void removeTree(File root){File[] children=root.listFiles();if(children!=null)for(File child:children)removeTree(child);root.delete();}
}
