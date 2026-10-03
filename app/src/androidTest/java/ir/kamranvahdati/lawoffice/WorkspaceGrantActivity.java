package ir.kamranvahdati.lawoffice;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.provider.DocumentsContract;
/** Test APK owns the provider and can offer the same persistable tree grant as a picker result. */
public final class WorkspaceGrantActivity extends Activity {
 @Override public void onCreate(Bundle saved){super.onCreate(saved);
  if(getIntent().getBooleanExtra("reset_fixture",false)){removeFixture(new java.io.File(getFilesDir(),"workspace-fixture"));}
  grantUriPermission("ir.kamranvahdati.lawoffice",DocumentsContract.buildTreeDocumentUri(WorkspaceTestProvider.AUTHORITY,"root"),Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
  android.os.ResultReceiver ready=getIntent().getParcelableExtra("fixture_ready");
  if(ready!=null)ready.send(RESULT_OK,Bundle.EMPTY);
  finish();
 }
 private static void removeFixture(java.io.File file){java.io.File[] children=file.listFiles();if(children!=null)for(java.io.File child:children)removeFixture(child);file.delete();}
}
