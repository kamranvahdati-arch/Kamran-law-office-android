package ir.kamranvahdati.lawoffice;
import android.app.job.*;
public final class AutoBackupJob extends JobService {
 @Override public boolean onStartJob(JobParameters params){AutoBackup.start(this,()->jobFinished(params,false));return true;}
 @Override public boolean onStopJob(JobParameters params){return true;}
}
