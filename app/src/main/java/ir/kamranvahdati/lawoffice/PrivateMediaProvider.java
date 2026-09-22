package ir.kamranvahdati.lawoffice;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

/** Read-only access to media copied into this installation, with per-intent URI grants. */
public final class PrivateMediaProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return "application/octet-stream"; }
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {
        if(!"r".equals(mode) || !uri.getAuthority().equals(getContext().getPackageName()+".media"))throw new FileNotFoundException();
        String id=uri.getLastPathSegment();
        if(id==null || !id.matches("[0-9a-f]{32}"))throw new FileNotFoundException();
        File file=new File(new File(getContext().getFilesDir(),"office-media"),id);
        return ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort) {
        String id=uri.getLastPathSegment();
        if(id==null||!id.matches("[0-9a-f]{32}"))return null;
        File file=new File(new File(getContext().getFilesDir(),"office-media"),id);
        if(!file.isFile())return null;
        String[] columns=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;
        Object[] values=new Object[columns.length];
        for(int i=0;i<columns.length;i++){
            if(OpenableColumns.DISPLAY_NAME.equals(columns[i]))values[i]=id;
            else if(OpenableColumns.SIZE.equals(columns[i]))values[i]=file.length();
        }
        MatrixCursor result=new MatrixCursor(columns);result.addRow(values);return result;
    }
    @Override public Uri insert(Uri uri,ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri,ContentValues values,String selection,String[] args) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri,String selection,String[] args) { throw new UnsupportedOperationException(); }
}
