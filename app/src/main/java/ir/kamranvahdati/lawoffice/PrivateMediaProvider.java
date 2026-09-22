package ir.kamranvahdati.lawoffice;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
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
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort) { return null; }
    @Override public Uri insert(Uri uri,ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri,ContentValues values,String selection,String[] args) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri,String selection,String[] args) { throw new UnsupportedOperationException(); }
}
