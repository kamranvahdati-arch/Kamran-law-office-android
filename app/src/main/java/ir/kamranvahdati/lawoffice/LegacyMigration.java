package ir.kamranvahdati.lawoffice;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

final class LegacyMigration {
    private static final String[] TABLES={"clients","cases","tasks","ledger","worklogs","appointments","deadlines"};

    static void migrate(Context context,OfficeDb encrypted) throws Exception {
        File legacy=context.getDatabasePath(OfficeDb.LEGACY_NAME);
        if(!legacy.exists())return;
        SharedPreferences flag=context.getSharedPreferences("database_migration",Context.MODE_PRIVATE);
        if(!flag.getBoolean("verified",false)) {
            JSONObject root=new JSONObject();root.put("format","KLO-2");JSONObject tables=new JSONObject();int[] counts=new int[TABLES.length];
            try(SQLiteDatabase old=SQLiteDatabase.openDatabase(legacy.getPath(),null,SQLiteDatabase.OPEN_READONLY)) {
                for(int j=0;j<TABLES.length;j++){JSONArray rows=new JSONArray();if(exists(old,TABLES[j]))try(Cursor c=old.rawQuery("SELECT * FROM "+TABLES[j],null)){while(c.moveToNext()){JSONObject row=new JSONObject();for(int k=0;k<c.getColumnCount();k++){String name=c.getColumnName(k);if(c.isNull(k))row.put(name,JSONObject.NULL);else if(c.getType(k)==Cursor.FIELD_TYPE_INTEGER)row.put(name,c.getLong(k));else row.put(name,c.getString(k));}rows.put(row);}}tables.put(TABLES[j],rows);counts[j]=rows.length();}
            }
            root.put("tables",tables);
            encrypted.getWritableDatabase();
            encrypted.importJson(root.toString());
            for(int j=0;j<TABLES.length;j++)try(Cursor c=encrypted.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM "+TABLES[j],null)){if(!c.moveToFirst()||c.getInt(0)!=counts[j])throw new IllegalStateException("تعداد داده‌های مهاجرت‌شده با نسخه قبلی یکسان نیست: "+TABLES[j]);}
            if(!flag.edit().putBoolean("verified",true).commit())throw new IllegalStateException("تأیید مهاجرت ذخیره نشد");
        }
        if(!SQLiteDatabase.deleteDatabase(legacy))throw new IllegalStateException("پاک کردن دیتابیس قدیمی بی‌رمز انجام نشد");
    }

    private static boolean exists(SQLiteDatabase db,String table){try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{table})){return c.moveToFirst();}}
}
