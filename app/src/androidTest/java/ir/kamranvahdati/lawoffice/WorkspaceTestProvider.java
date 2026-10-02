package ir.kamranvahdati.lawoffice;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsProvider;
import java.io.*;

/** Instrumentation-only documents provider; fixtures never contain real project records. */
public final class WorkspaceTestProvider extends DocumentsProvider {
    public static final String AUTHORITY="ir.kamranvahdati.lawoffice.storagefixture";
    private File base(){File base=new File(getContext().getFilesDir(),"workspace-fixture");base.mkdirs();return base;}
    private File file(String id)throws FileNotFoundException{
        if(!id.matches("root(/[A-Za-z0-9._-]+)*"))throw new FileNotFoundException();
        File file=id.equals("root")?base():new File(base(),id.substring(5));
        try{if(!file.getCanonicalPath().startsWith(base().getCanonicalPath()+"/")&&!file.equals(base()))throw new FileNotFoundException();}
        catch(IOException e){throw new FileNotFoundException();}return file;
    }
    private static final String[] COLUMNS={Document.COLUMN_DOCUMENT_ID,Document.COLUMN_DISPLAY_NAME,Document.COLUMN_MIME_TYPE,Document.COLUMN_FLAGS,Document.COLUMN_SIZE};
    @Override public boolean onCreate(){return true;}
    private void remove(File f){File[] children=f.listFiles();if(children!=null)for(File child:children)remove(child);f.delete();}
    @Override public Cursor queryRoots(String[] projection){return new MatrixCursor(new String[]{"root_id","document_id","title","flags"});}
    private void row(MatrixCursor c,String id,File f){MatrixCursor.RowBuilder row=c.newRow();for(String column:c.getColumnNames()){
        Object value=null;
        if(Document.COLUMN_DOCUMENT_ID.equals(column))value=id;
        if(Document.COLUMN_DISPLAY_NAME.equals(column))value=f.getName();
        if(Document.COLUMN_MIME_TYPE.equals(column))value=f.isDirectory()?Document.MIME_TYPE_DIR:"application/octet-stream";
        if(Document.COLUMN_FLAGS.equals(column))value=Document.FLAG_SUPPORTS_WRITE|Document.FLAG_SUPPORTS_DELETE|(f.isDirectory()?Document.FLAG_DIR_SUPPORTS_CREATE:0);
        if(Document.COLUMN_SIZE.equals(column))value=f.length();row.add(value);
    }}
    @Override public Cursor queryDocument(String id,String[] projection)throws FileNotFoundException{MatrixCursor c=new MatrixCursor(projection==null?COLUMNS:projection);File f=file(id);if(f.exists())row(c,id,f);return c;}
    @Override public Cursor queryChildDocuments(String parent,String[] projection,String sort)throws FileNotFoundException{
        MatrixCursor c=new MatrixCursor(projection==null?COLUMNS:projection);File[] children=file(parent).listFiles();if(children!=null)for(File f:children)row(c,parent+"/"+f.getName(),f);return c;
    }
    @Override public String createDocument(String parent,String mime,String name)throws FileNotFoundException{
        if(!name.matches("[A-Za-z0-9._-]+"))throw new FileNotFoundException();File f=new File(file(parent),name);
        try{if(Document.MIME_TYPE_DIR.equals(mime)){if(!f.mkdir()&&!f.isDirectory())throw new IOException();}else if(!f.createNewFile())throw new IOException();}
        catch(IOException e){throw new FileNotFoundException();}return parent+"/"+name;
    }
    @Override public ParcelFileDescriptor openDocument(String id,String mode,CancellationSignal signal)throws FileNotFoundException{return ParcelFileDescriptor.open(file(id),ParcelFileDescriptor.parseMode(mode));}
    @Override public void deleteDocument(String id)throws FileNotFoundException{
        if("root".equals(id)){remove(base());base();return;}
        if(new File(base(),"deny-delete").exists())throw new FileNotFoundException("Test provider refuses deletion");
        if(!file(id).delete())throw new FileNotFoundException();
    }
    @Override public boolean isChildDocument(String parent,String child){return child.startsWith(parent+"/");}
}
