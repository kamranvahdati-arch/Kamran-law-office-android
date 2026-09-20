package ir.kamranvahdati.lawoffice;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.HashSet;
import java.util.TimeZone;

final class OfficeDb extends SQLiteOpenHelper {
    static final String LEGACY_NAME = "law_office_demo_v7.db";
    static final String ENCRYPTED_NAME = "law_office_encrypted_v9.db";
    static final String DATABASE_NAME = ENCRYPTED_NAME;
    private static final int VERSION = 12;

    OfficeDb(Context context) {
        super(context, DATABASE_NAME, DatabaseKey.read(context), null, VERSION, 0, null, null, false);
        System.loadLibrary("sqlcipher");
    }

    @Override public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.execSQL("PRAGMA foreign_keys=ON");
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tasks(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL," +
                "case_name TEXT,due_date TEXT,due_time TEXT,priority TEXT NOT NULL," +
                "done INTEGER NOT NULL DEFAULT 0,status TEXT NOT NULL DEFAULT 'open',notes TEXT," +
                "created_at TEXT,updated_at TEXT)");
        db.execSQL("CREATE TABLE clients(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL," +
                "national_id TEXT,father_name TEXT,birth_date TEXT,phone TEXT,address TEXT,notes TEXT," +
                "created_at TEXT,updated_at TEXT)");
        db.execSQL("CREATE TABLE cases(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL," +
                "reference TEXT,stage TEXT,client_id INTEGER,fee_agreed INTEGER NOT NULL DEFAULT 0," +
                "agreement_notes TEXT,status TEXT NOT NULL DEFAULT 'active',category TEXT," +
                "case_number TEXT,archive_number TEXT,branch TEXT,subject TEXT,claim_text TEXT," +
                "authority_type TEXT,province TEXT,judicial_city TEXT," +
                "evidence TEXT,summary TEXT,financial_notes TEXT,contract_notes TEXT," +
                "created_at TEXT,updated_at TEXT," +
                "FOREIGN KEY(client_id) REFERENCES clients(id) ON DELETE SET NULL)");
        db.execSQL("CREATE TABLE ledger(id INTEGER PRIMARY KEY AUTOINCREMENT,case_id INTEGER NOT NULL," +
                "kind TEXT NOT NULL,category TEXT NOT NULL,amount INTEGER NOT NULL DEFAULT 0," +
                "entry_date TEXT,paid_by TEXT,description TEXT,created_at TEXT," +
                "FOREIGN KEY(case_id) REFERENCES cases(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE worklogs(id INTEGER PRIMARY KEY AUTOINCREMENT,case_id INTEGER NOT NULL," +
                "client_id INTEGER,action_type TEXT NOT NULL,action_date TEXT,description TEXT,created_at TEXT," +
                "FOREIGN KEY(case_id) REFERENCES cases(id) ON DELETE CASCADE," +
                "FOREIGN KEY(client_id) REFERENCES clients(id) ON DELETE SET NULL)");
        createScheduleTables(db);
        migrateV9(db);
        migrateV10(db);
        migrateV11(db);
        migrateV12(db);
        seed(db);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 5) {
            addColumn(db, "tasks", "due_time TEXT");
            addColumn(db, "tasks", "notes TEXT");
            addColumn(db, "cases", "status TEXT NOT NULL DEFAULT 'active'");
            addColumn(db, "cases", "category TEXT");
            addColumn(db, "cases", "case_number TEXT");
            addColumn(db, "cases", "archive_number TEXT");
            addColumn(db, "cases", "branch TEXT");
            addColumn(db, "cases", "subject TEXT");
            addColumn(db, "cases", "claim_text TEXT");
            addColumn(db, "cases", "evidence TEXT");
            addColumn(db, "cases", "summary TEXT");
            addColumn(db, "cases", "financial_notes TEXT");
            addColumn(db, "cases", "contract_notes TEXT");
            db.execSQL("UPDATE cases SET status='active' WHERE status IS NULL");
            db.execSQL("UPDATE cases SET category='حقوقی' WHERE category IS NULL");
        }
        if (oldVersion < 7) {
            addColumn(db, "cases", "authority_type TEXT");
            addColumn(db, "cases", "province TEXT");
            addColumn(db, "cases", "judicial_city TEXT");
            db.execSQL("CREATE TABLE IF NOT EXISTS worklogs(id INTEGER PRIMARY KEY AUTOINCREMENT,case_id INTEGER NOT NULL," +
                    "client_id INTEGER,action_type TEXT NOT NULL,action_date TEXT,description TEXT,created_at TEXT," +
                    "FOREIGN KEY(case_id) REFERENCES cases(id) ON DELETE CASCADE," +
                    "FOREIGN KEY(client_id) REFERENCES clients(id) ON DELETE SET NULL)");
        }
        if (oldVersion < 8) createScheduleTables(db);
        if (oldVersion < 9) migrateV9(db);
        if (oldVersion < 10) migrateV10(db);
        if (oldVersion < 11) migrateV11(db);
        if (oldVersion < 12) migrateV12(db);
    }

    /** Additive only: preserve existing dates, reminder epochs, IDs and completion flags. */
    private void migrateV12(SQLiteDatabase db) {
        addColumn(db,"appointments","branch TEXT");
        addColumn(db,"appointments","authority TEXT");
        addColumn(db,"appointments","city TEXT");
        addColumn(db,"appointments","attendance_status TEXT NOT NULL DEFAULT 'planned'");
        addColumn(db,"appointments","completed_at TEXT");
        addColumn(db,"deadlines","client_id INTEGER REFERENCES clients(id)");
        addColumn(db,"deadlines","completed_at TEXT");
        addColumn(db,"reminders","label TEXT NOT NULL DEFAULT ''");
    }

    private void migrateV9(SQLiteDatabase db) {
        for (String table : new String[]{"clients","cases","tasks","ledger","worklogs","appointments","deadlines"}) {
            addColumn(db, table, "uid TEXT");
            addColumn(db, table, "deleted_at TEXT");
            if (!"clients".equals(table) && !"cases".equals(table) && !"tasks".equals(table))
                addColumn(db, table, "updated_at TEXT");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_"+table+"_uid ON "+table+"(uid)");
            try (Cursor rows=db.rawQuery("SELECT id,created_at FROM "+table+" WHERE uid IS NULL",null)) {
                while (rows.moveToNext()) {
                    ContentValues v=new ContentValues(); v.put("uid",UUID.randomUUID().toString());
                    v.put("updated_at",rows.isNull(1)?now():rows.getString(1));
                    if(rows.isNull(1))v.put("created_at",now());
                    db.update(table,v,"id=?",new String[]{String.valueOf(rows.getLong(0))});
                }
            }
        }
        addColumn(db,"tasks","case_id INTEGER REFERENCES cases(id) ON DELETE SET NULL");
        addColumn(db,"tasks","client_id INTEGER REFERENCES clients(id) ON DELETE SET NULL");
        addColumn(db,"tasks","kind TEXT");
        addColumn(db,"tasks","end_time TEXT");
        addColumn(db,"tasks","place TEXT");
        addColumn(db,"cases","contract_number TEXT");
        addColumn(db,"cases","contract_date TEXT");
        db.execSQL("CREATE TABLE IF NOT EXISTS installments(id INTEGER PRIMARY KEY AUTOINCREMENT,uid TEXT UNIQUE NOT NULL,case_id INTEGER NOT NULL REFERENCES cases(id),title TEXT NOT NULL,amount INTEGER NOT NULL CHECK(amount>0),due_date TEXT NOT NULL,created_at TEXT NOT NULL,updated_at TEXT NOT NULL,deleted_at TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS installment_payments(id INTEGER PRIMARY KEY AUTOINCREMENT,uid TEXT UNIQUE NOT NULL,installment_id INTEGER NOT NULL REFERENCES installments(id),ledger_id INTEGER NOT NULL REFERENCES ledger(id),amount INTEGER NOT NULL CHECK(amount>0),created_at TEXT NOT NULL,updated_at TEXT NOT NULL,deleted_at TEXT)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_installment_case ON installments(case_id,due_date)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_tasks_case_date ON tasks(case_id,due_date)");
        db.execSQL("CREATE TABLE IF NOT EXISTS reminders(id INTEGER PRIMARY KEY AUTOINCREMENT,uid TEXT UNIQUE NOT NULL,target_type TEXT NOT NULL,target_id INTEGER NOT NULL,advance_days INTEGER NOT NULL,trigger_at INTEGER NOT NULL,fired_at TEXT,created_at TEXT NOT NULL,updated_at TEXT NOT NULL,deleted_at TEXT)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_reminders_trigger ON reminders(trigger_at,fired_at)");
    }

    /** Additive v10 migration. The v9 database file name and all existing columns stay intact. */
    private void migrateV10(SQLiteDatabase db) {
        for (String table : new String[]{"clients","cases","tasks","ledger","worklogs","appointments","deadlines","installments","installment_payments"})
            addColumn(db, table, "is_demo INTEGER NOT NULL DEFAULT 0");
        addColumn(db,"tasks","category TEXT");
        addColumn(db,"tasks","is_personal INTEGER NOT NULL DEFAULT 0");
        addColumn(db,"installment_payments","payment_method TEXT NOT NULL DEFAULT 'transfer'");
        addColumn(db,"installment_payments","check_id INTEGER REFERENCES payment_checks(id) ON DELETE SET NULL");

        db.execSQL("CREATE TABLE IF NOT EXISTS case_clients(id INTEGER PRIMARY KEY AUTOINCREMENT,uid TEXT UNIQUE NOT NULL,"+
                "case_id INTEGER NOT NULL REFERENCES cases(id),client_id INTEGER NOT NULL REFERENCES clients(id),"+
                "role TEXT NOT NULL DEFAULT 'موکل',is_primary INTEGER NOT NULL DEFAULT 0,"+
                "created_at TEXT NOT NULL,updated_at TEXT NOT NULL,deleted_at TEXT,is_demo INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_case_clients_live ON case_clients(case_id,client_id) WHERE deleted_at IS NULL");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_case_clients_client ON case_clients(client_id,deleted_at)");

        db.execSQL("CREATE TABLE IF NOT EXISTS collaborators(id INTEGER PRIMARY KEY AUTOINCREMENT,uid TEXT UNIQUE NOT NULL,"+
                "first_name TEXT NOT NULL,last_name TEXT NOT NULL,license_number TEXT,professional_body TEXT,phone TEXT,notes TEXT,"+
                "created_at TEXT NOT NULL,updated_at TEXT NOT NULL,deleted_at TEXT,is_demo INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS case_collaborators(id INTEGER PRIMARY KEY AUTOINCREMENT,uid TEXT UNIQUE NOT NULL,"+
                "case_id INTEGER NOT NULL REFERENCES cases(id),collaborator_id INTEGER NOT NULL REFERENCES collaborators(id),"+
                "collaboration_mode TEXT NOT NULL,fee_share_percent REAL,financial_notes TEXT,"+
                "created_at TEXT NOT NULL,updated_at TEXT NOT NULL,deleted_at TEXT,is_demo INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_case_collaborator_live ON case_collaborators(case_id,collaborator_id) WHERE deleted_at IS NULL");

        db.execSQL("CREATE TABLE IF NOT EXISTS representation_contracts(id INTEGER PRIMARY KEY AUTOINCREMENT,uid TEXT UNIQUE NOT NULL,"+
                "case_id INTEGER REFERENCES cases(id),contract_number TEXT NOT NULL,contract_date TEXT NOT NULL,subject TEXT,"+
                "authority_scope TEXT,fee_amount INTEGER,opponents_any INTEGER NOT NULL DEFAULT 0,opponents_text TEXT,"+
                "delegation_allowed INTEGER NOT NULL DEFAULT 0,collaboration_mode TEXT,notes TEXT,"+
                "created_at TEXT NOT NULL,updated_at TEXT NOT NULL,deleted_at TEXT,is_demo INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS contract_clients(id INTEGER PRIMARY KEY AUTOINCREMENT,uid TEXT UNIQUE NOT NULL,"+
                "contract_id INTEGER NOT NULL REFERENCES representation_contracts(id),client_id INTEGER NOT NULL REFERENCES clients(id),"+
                "created_at TEXT NOT NULL,updated_at TEXT NOT NULL,deleted_at TEXT,is_demo INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_contract_clients_live ON contract_clients(contract_id,client_id) WHERE deleted_at IS NULL");
        db.execSQL("CREATE TABLE IF NOT EXISTS contract_collaborators(id INTEGER PRIMARY KEY AUTOINCREMENT,uid TEXT UNIQUE NOT NULL,"+
                "contract_id INTEGER NOT NULL REFERENCES representation_contracts(id),collaborator_id INTEGER NOT NULL REFERENCES collaborators(id),"+
                "collaboration_mode TEXT,fee_share_percent REAL,notes TEXT,"+
                "created_at TEXT NOT NULL,updated_at TEXT NOT NULL,deleted_at TEXT,is_demo INTEGER NOT NULL DEFAULT 0)");

        db.execSQL("CREATE TABLE IF NOT EXISTS financial_contracts(id INTEGER PRIMARY KEY AUTOINCREMENT,uid TEXT UNIQUE NOT NULL,"+
                "case_id INTEGER NOT NULL REFERENCES cases(id),contract_date TEXT NOT NULL,total_amount INTEGER NOT NULL CHECK(total_amount>=0),"+
                "payment_terms TEXT,notes TEXT,created_at TEXT NOT NULL,updated_at TEXT NOT NULL,deleted_at TEXT,is_demo INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS payment_checks(id INTEGER PRIMARY KEY AUTOINCREMENT,uid TEXT UNIQUE NOT NULL,"+
                "case_id INTEGER NOT NULL REFERENCES cases(id),installment_id INTEGER REFERENCES installments(id),check_number TEXT NOT NULL,"+
                "due_date TEXT NOT NULL,amount INTEGER NOT NULL CHECK(amount>0),bank TEXT,branch TEXT,"+
                "collection_status TEXT NOT NULL DEFAULT 'pending',notes TEXT,"+
                "created_at TEXT NOT NULL,updated_at TEXT NOT NULL,deleted_at TEXT,is_demo INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_checks_due ON payment_checks(collection_status,due_date)");

        db.execSQL("CREATE TABLE IF NOT EXISTS legal_taxonomy(id INTEGER PRIMARY KEY AUTOINCREMENT,uid TEXT UNIQUE NOT NULL,"+
                "remote_id TEXT UNIQUE,title TEXT NOT NULL,parent_remote_id TEXT,content_version TEXT,"+
                "created_at TEXT NOT NULL,updated_at TEXT NOT NULL,deleted_at TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS legal_documents(id INTEGER PRIMARY KEY AUTOINCREMENT,uid TEXT UNIQUE NOT NULL,"+
                "remote_id TEXT UNIQUE,kind TEXT NOT NULL,title TEXT NOT NULL,number TEXT,subject TEXT,taxonomy_remote_id TEXT,"+
                "body TEXT NOT NULL,source_url TEXT,content_version TEXT,downloaded_at TEXT,"+
                "created_at TEXT NOT NULL,updated_at TEXT NOT NULL,deleted_at TEXT)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_legal_document_lookup ON legal_documents(kind,number,taxonomy_remote_id)");
        db.execSQL("CREATE TABLE IF NOT EXISTS legal_sync_state(id INTEGER PRIMARY KEY CHECK(id=1),manifest_version TEXT,taxonomy_version TEXT,last_checked_at TEXT,last_success_at TEXT)");

        backfillCaseClients(db);
        tagRecognizableDemoRows(db);
    }

    /** Additive v11 migration for v0.9.1. Existing rows and the encrypted file are preserved. */
    private void migrateV11(SQLiteDatabase db) {
        addColumn(db,"cases","judgment_number TEXT");
        addColumn(db,"cases","order_number TEXT");
        db.execSQL("CREATE TABLE IF NOT EXISTS case_attachments(id INTEGER PRIMARY KEY AUTOINCREMENT,"+
                "uid TEXT UNIQUE NOT NULL,case_id INTEGER NOT NULL REFERENCES cases(id),"+
                "display_name TEXT NOT NULL,mime_type TEXT NOT NULL,content_uri TEXT NOT NULL,"+
                "created_at TEXT NOT NULL,updated_at TEXT NOT NULL,deleted_at TEXT,is_demo INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_case_attachments_case ON case_attachments(case_id,deleted_at)");
    }

    private void backfillCaseClients(SQLiteDatabase db) {
        try (Cursor rows=db.rawQuery("SELECT c.id,c.client_id,c.created_at,c.updated_at,COALESCE(c.is_demo,0) FROM cases c WHERE c.client_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM case_clients cc WHERE cc.case_id=c.id AND cc.client_id=c.client_id AND cc.deleted_at IS NULL)",null)) {
            while(rows.moveToNext()) {
                ContentValues v=new ContentValues();identity(v);v.put("case_id",rows.getLong(0));v.put("client_id",rows.getLong(1));
                v.put("role","موکل اصلی");v.put("is_primary",1);v.put("is_demo",rows.getInt(4));db.insertOrThrow("case_clients",null,v);
            }
        }
    }

    private void tagRecognizableDemoRows(SQLiteDatabase db) {
        db.execSQL("UPDATE clients SET is_demo=1 WHERE is_demo=0 AND name LIKE '%آزمایشی%' AND notes LIKE '%شخص واقعی نیست%'");
        db.execSQL("UPDATE cases SET is_demo=1 WHERE is_demo=0 AND title LIKE '%آزمایشی%' AND contract_notes LIKE '%فاقد هرگونه اثر واقعی%'");
        db.execSQL("UPDATE tasks SET is_demo=1 WHERE is_demo=0 AND notes LIKE '%آزمایش برنامه%'");
        db.execSQL("UPDATE ledger SET is_demo=1 WHERE is_demo=0 AND description LIKE '%کاملاً فرضی%'");
        db.execSQL("UPDATE worklogs SET is_demo=1 WHERE is_demo=0 AND description LIKE '%کاملاً فرضی%'");
        db.execSQL("UPDATE case_clients SET is_demo=1 WHERE case_id IN(SELECT id FROM cases WHERE is_demo=1) AND client_id IN(SELECT id FROM clients WHERE is_demo=1)");
    }

    private static void identity(ContentValues v) { String t=now();v.put("uid",UUID.randomUUID().toString());v.put("created_at",t);v.put("updated_at",t); }

    private void createScheduleTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS appointments(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "kind TEXT NOT NULL,person_name TEXT NOT NULL,contact_phone TEXT,client_id INTEGER,case_id INTEGER," +
                "visit_date TEXT NOT NULL,start_time TEXT NOT NULL,end_time TEXT NOT NULL,place TEXT,notes TEXT," +
                "created_at TEXT,FOREIGN KEY(client_id) REFERENCES clients(id) ON DELETE SET NULL," +
                "FOREIGN KEY(case_id) REFERENCES cases(id) ON DELETE SET NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS deadlines(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "case_id INTEGER NOT NULL,title TEXT NOT NULL,event_date TEXT NOT NULL,due_date TEXT NOT NULL," +
                "duration_days INTEGER NOT NULL,notes TEXT,completed INTEGER NOT NULL DEFAULT 0,created_at TEXT," +
                "FOREIGN KEY(case_id) REFERENCES cases(id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_appointments_date ON appointments(visit_date,start_time)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_deadlines_due ON deadlines(completed,due_date)");
    }

    private void addColumn(SQLiteDatabase db, String table, String definition) {
        try { db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + definition); }
        catch (Exception ignored) { }
    }

    private void seed(SQLiteDatabase db) {
        String[] first = {"آرمان","بهار","پارسا","ترانه","سامان","درسا","کیان","مهسا","نوید","هلیا"};
        String[] last = {"آزمایشی","نمونه‌پور","فرضی‌نژاد","داده‌آرا","آزمون‌خواه","نمونه‌جو","فرضی‌فر","داده‌ور","آزمون‌پور","نمونه‌یار"};
        String[] categories = {"کیفری","حقوقی","انقلاب","خانواده","دیوان عدالت اداری","نظامی","سایر"};
        String[] subjects = {"مطالبه وجه","الزام به ایفای تعهد","اعتراض به رأی","اختلاف خانوادگی","شکایت اداری","دفاع کیفری","اختلاف قراردادی"};
        String[] expenseCategories = {"هزینه دادرسی","کارشناسی","تمبر و خدمات قضایی","هتل","بلیط هواپیما","تاکسی و فرودگاه","سایر"};
        long[] clientIds = new long[100];
        long[] caseIds = new long[100];
        JalaliDate today = JalaliDate.today();

        for (int i=0;i<100;i++) {
            ContentValues v=new ContentValues();
            v.put("name",first[i%first.length]+" "+last[(i/first.length)%last.length]+" «آزمایشی "+fa(i+1)+"»");
            v.put("national_id",String.format(Locale.US,"%010d",8000000000L+i));
            v.put("father_name","نام پدر فرضی "+fa((i%20)+1));
            v.put("birth_date",String.format(Locale.US,"13%02d/%02d/%02d",50+(i%40),(i%12)+1,(i%28)+1));
            v.put("phone",String.format(Locale.US,"0912%07d",i+1));
            v.put("address","نشانی کاملاً فرضی، شهر نمونه، خیابان آزمایش، پلاک "+fa(i+1));
            v.put("notes","این رکورد صرفاً برای آزمون نرم‌افزار است و شخص واقعی نیست.");
            v.put("is_demo",1);
            identity(v);
            clientIds[i]=db.insertOrThrow("clients",null,v);
        }

        for (int i=0;i<100;i++) {
            ContentValues v=new ContentValues(); String cat=categories[i%categories.length];
            v.put("title","پرونده "+cat+" آزمایشی شماره "+fa(i+1));
            v.put("case_number",String.format(Locale.US,"1405%012d",i+1));
            v.put("archive_number",String.format(Locale.US,"%06d",5000+i));
            v.put("reference","مجتمع قضایی فرضی "+fa((i%8)+1));
            v.put("authority_type",i%3==0?"دادگاه":i%3==1?"دادسرا":"شورای حل اختلاف");
            v.put("province",i%2==0?"تهران":"البرز");
            v.put("judicial_city",i%2==0?"تهران":"کرج");
            v.put("branch","شعبه "+fa((i%50)+1)+" نمونه");
            v.put("stage",i%3==0?"بدوی":i%3==1?"تجدیدنظر":"اجرای احکام");
            v.put("client_id",clientIds[i]);
            v.put("fee_agreed",100000000L+(i*5000000L));
            v.put("agreement_notes","توافق فرضی پرداخت حق‌الوکاله در "+fa((i%3)+1)+" مرحله");
            v.put("contract_notes","عقد وکالت آزمایشی؛ فاقد هرگونه اثر واقعی و حقوقی.");
            v.put("status",i%4==0?"closed":"active");
            v.put("category",cat); v.put("subject",subjects[i%subjects.length]);
            v.put("claim_text","خواسته یا شکایت فرضی شماره "+fa(i+1));
            v.put("evidence","سند فرضی، گواهی نمونه و مکاتبات آزمایشی");
            v.put("summary","شرح مختصر کاملاً ساختگی برای سنجش نمایش اطلاعات پرونده.");
            v.put("financial_notes","یادداشت مالی آزمایشی شماره "+fa(i+1));
            v.put("is_demo",1);
            identity(v);
            caseIds[i]=db.insertOrThrow("cases",null,v);
            ContentValues link=new ContentValues();identity(link);link.put("case_id",caseIds[i]);link.put("client_id",clientIds[i]);
            link.put("role","موکل اصلی");link.put("is_primary",1);link.put("is_demo",1);db.insertOrThrow("case_clients",null,link);
        }

        for (int i=0;i<100;i++) {
            ContentValues v=new ContentValues(); int day=(i%JalaliDate.monthLength(today.year,today.month))+1;
            v.put("title",i%4==0?"مهلت فوری آزمایشی "+fa(i+1):"برنامه کاری آزمایشی "+fa(i+1));
            v.put("case_name","پرونده آزمایشی شماره "+fa((i%100)+1));v.put("case_id",caseIds[i]);v.put("client_id",clientIds[i]);
            v.put("due_date",String.format(Locale.US,"%04d/%02d/%02d",today.year,today.month,day));
            v.put("due_time",String.format(Locale.US,"%02d:%02d",8+(i%10),(i%4)*15));
            v.put("priority",i%4==0?"فوری":i%4==1?"عادی":"کم");
            v.put("done",i%10==0?1:0); v.put("status",i%10==0?"done":i%10==1?"deferred":"open");
            v.put("notes","توضیح فرضی برای آزمایش برنامه، یادآوری و تقویم.");
            v.put("is_demo",1);
            identity(v); db.insertOrThrow("tasks",null,v);
        }

        for (int i=0;i<100;i++) {
            ContentValues v=new ContentValues(); boolean payment=i%2==0;
            v.put("case_id",caseIds[i]); v.put("kind",payment?"payment":"expense");
            v.put("category",payment?"حق‌الوکاله":expenseCategories[i%expenseCategories.length]);
            v.put("amount",payment?25000000L+(i*1000000L):5000000L+(i*250000L));
            v.put("entry_date",String.format(Locale.US,"%04d/%02d/%02d",today.year,today.month,(i%28)+1));
            v.put("paid_by",payment?"موکل":i%4==1?"وکیل":"موکل");
            v.put("description","عملیات مالی کاملاً فرضی شماره "+fa(i+1));
            v.put("is_demo",1);
            identity(v); db.insertOrThrow("ledger",null,v);
        }
        for (int i=0;i<100;i++) {
            ContentValues v=new ContentValues();
            v.put("case_id",caseIds[i]); v.put("client_id",clientIds[i]);
            v.put("action_type",i%4==0?"مطالعه پرونده":i%4==1?"مراجعه به شعبه":i%4==2?"پیگیری پرونده":"ارسال لایحه");
            v.put("action_date",String.format(Locale.US,"%04d/%02d/%02d",today.year,today.month,(i%28)+1));
            v.put("description","گزارش اقدام کاملاً فرضی شماره "+fa(i+1)); identity(v);
            v.put("is_demo",1);
            db.insertOrThrow("worklogs",null,v);
        }
        verifyDemoData(db);
    }

    private void verifyDemoData(SQLiteDatabase db) {
        for (String table:new String[]{"clients","cases","tasks","ledger","worklogs"})
            if (countRows(db,table)!=100)
                throw new IllegalStateException("آزمون تعداد رکوردهای "+table+" ناموفق بود");
        Cursor fk=db.rawQuery("PRAGMA foreign_key_check",null);
        boolean invalid=fk.moveToFirst(); fk.close();
        if(invalid) throw new IllegalStateException("آزمون ارتباط داده‌ها ناموفق بود");
    }

    private int countRows(SQLiteDatabase db,String table){try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+table,null)){return c.moveToFirst()?c.getInt(0):0;}}

    private static String fa(int number) {
        return String.valueOf(number).replace('0','۰').replace('1','۱').replace('2','۲')
                .replace('3','۳').replace('4','۴').replace('5','۵').replace('6','۶')
                .replace('7','۷').replace('8','۸').replace('9','۹');
    }

    long addClient(String name, String nationalId, String father, String birth,
                   String phone, String address, String notes) {
        ContentValues v = new ContentValues();
        v.put("name", name); v.put("national_id", nationalId); v.put("father_name", father);
        v.put("birth_date", birth); v.put("phone", phone); v.put("address", address);
        v.put("notes", notes); identity(v);
        return getWritableDatabase().insertOrThrow("clients", null, v);
    }

    void updateClient(long id,String name,String nationalId,String father,String birth,String phone,String address,String notes){ContentValues v=new ContentValues();v.put("name",name);v.put("national_id",nationalId);v.put("father_name",father);v.put("birth_date",birth);v.put("phone",phone);v.put("address",address);v.put("notes",notes);v.put("updated_at",now());if(getWritableDatabase().update("clients",v,"id=? AND deleted_at IS NULL",new String[]{String.valueOf(id)})!=1)throw new IllegalArgumentException("موکل یافت نشد");}
    void deleteClient(long id){if(scalar("SELECT COUNT(*) FROM case_clients cc JOIN cases c ON c.id=cc.case_id WHERE cc.client_id=? AND cc.deleted_at IS NULL AND c.deleted_at IS NULL",String.valueOf(id))>0)throw new IllegalArgumentException("ابتدا ارتباط این موکل با پرونده‌ها را مدیریت کنید");softDelete("clients",id);}

    long addCase(CaseRecord c) {
        ContentValues v = caseValues(c);
        identity(v);
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{long id=db.insertOrThrow("cases", null, v);if(c.clientId>0)addCaseClient(db,id,c.clientId,"موکل اصلی",true,false);db.setTransactionSuccessful();return id;}finally{db.endTransaction();}
    }
    void updateCase(CaseRecord c){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{ContentValues v=caseValues(c);v.put("updated_at",now());if(db.update("cases",v,"id=? AND deleted_at IS NULL",new String[]{String.valueOf(c.id)})!=1)throw new IllegalArgumentException("پرونده یافت نشد");if(c.clientId>0){ContentValues primaryOff=new ContentValues();primaryOff.put("is_primary",0);primaryOff.put("updated_at",now());db.update("case_clients",primaryOff,"case_id=? AND deleted_at IS NULL",new String[]{String.valueOf(c.id)});addCaseClient(db,c.id,c.clientId,"موکل اصلی",true,false);}db.setTransactionSuccessful();}finally{db.endTransaction();}}
    void deleteCase(long id){SQLiteDatabase db=getWritableDatabase();String[] args={String.valueOf(id)};db.beginTransaction();try{String t=now();ContentValues tomb=new ContentValues();tomb.put("deleted_at",t);tomb.put("updated_at",t);db.update("reminders",tomb,"(target_type='deadline' AND target_id IN(SELECT id FROM deadlines WHERE case_id=?)) OR (target_type='task' AND target_id IN(SELECT id FROM tasks WHERE case_id=?)) OR (target_type='appointment' AND target_id IN(SELECT id FROM appointments WHERE case_id=?))",new String[]{args[0],args[0],args[0]});db.update("installment_payments",tomb,"installment_id IN(SELECT id FROM installments WHERE case_id=?)",args);db.update("contract_clients",tomb,"contract_id IN(SELECT id FROM representation_contracts WHERE case_id=?)",args);db.update("contract_collaborators",tomb,"contract_id IN(SELECT id FROM representation_contracts WHERE case_id=?)",args);for(String child:new String[]{"tasks","ledger","worklogs","appointments","deadlines","installments","case_clients","case_collaborators","representation_contracts","financial_contracts","payment_checks","case_attachments"})db.update(child,tomb,"case_id=? AND deleted_at IS NULL",args);db.update("cases",tomb,"id=? AND deleted_at IS NULL",args);db.setTransactionSuccessful();}finally{db.endTransaction();}}
    private void softDelete(String table,long id){ContentValues v=new ContentValues();v.put("deleted_at",now());v.put("updated_at",now());getWritableDatabase().update(table,v,"id=? AND deleted_at IS NULL",new String[]{String.valueOf(id)});}

    void updateCaseStatus(long id, String status) {
        ContentValues v = new ContentValues(); v.put("status", status); v.put("updated_at", now());
        getWritableDatabase().update("cases", v, "id=? AND deleted_at IS NULL", new String[]{String.valueOf(id)});
    }

    void addClientToCase(long caseId,long clientId,String role,boolean primary){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{try(Cursor count=db.rawQuery("SELECT COUNT(*) FROM case_clients WHERE case_id=? AND deleted_at IS NULL",new String[]{String.valueOf(caseId)})){if(count.moveToFirst()&&count.getInt(0)==0)primary=true;}if(primary){ContentValues off=new ContentValues();off.put("is_primary",0);off.put("updated_at",now());db.update("case_clients",off,"case_id=? AND deleted_at IS NULL",new String[]{String.valueOf(caseId)});}addCaseClient(db,caseId,clientId,role,primary,false);if(primary){ContentValues cv=new ContentValues();cv.put("client_id",clientId);cv.put("updated_at",now());db.update("cases",cv,"id=?",new String[]{String.valueOf(caseId)});}db.setTransactionSuccessful();}finally{db.endTransaction();}}
    void unlinkClientFromCase(long caseId,long clientId){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{String now=now();ContentValues tomb=new ContentValues();tomb.put("deleted_at",now);tomb.put("updated_at",now);if(db.update("case_clients",tomb,"case_id=? AND client_id=? AND deleted_at IS NULL",new String[]{String.valueOf(caseId),String.valueOf(clientId)})!=1)throw new IllegalArgumentException("ارتباط موکل یافت نشد");Long replacement=null;try(Cursor c=db.rawQuery("SELECT client_id FROM case_clients WHERE case_id=? AND deleted_at IS NULL ORDER BY is_primary DESC,id LIMIT 1",new String[]{String.valueOf(caseId)})){if(c.moveToFirst())replacement=c.getLong(0);}ContentValues cv=new ContentValues();if(replacement==null)cv.putNull("client_id");else cv.put("client_id",replacement);cv.put("updated_at",now);db.update("cases",cv,"id=?",new String[]{String.valueOf(caseId)});if(replacement!=null){ContentValues primary=new ContentValues();primary.put("is_primary",1);primary.put("updated_at",now);db.update("case_clients",primary,"case_id=? AND client_id=? AND deleted_at IS NULL",new String[]{String.valueOf(caseId),String.valueOf(replacement)});}db.setTransactionSuccessful();}finally{db.endTransaction();}}
    private void addCaseClient(SQLiteDatabase db,long caseId,long clientId,String role,boolean primary,boolean demo){
        try(Cursor exists=db.rawQuery("SELECT id FROM case_clients WHERE case_id=? AND client_id=? AND deleted_at IS NULL",new String[]{String.valueOf(caseId),String.valueOf(clientId)})){
            if(exists.moveToFirst()){ContentValues update=new ContentValues();update.put("role",role);update.put("is_primary",primary?1:0);update.put("updated_at",now());db.update("case_clients",update,"id=?",new String[]{String.valueOf(exists.getLong(0))});return;}
        }
        ContentValues v=new ContentValues();identity(v);v.put("case_id",caseId);v.put("client_id",clientId);v.put("role",role);v.put("is_primary",primary?1:0);v.put("is_demo",demo?1:0);db.insertOrThrow("case_clients",null,v);
    }
    List<ClientRecord> caseClients(long caseId){ArrayList<ClientRecord> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT cl.id,cl.name,cl.national_id,cl.father_name,cl.birth_date,cl.phone,cl.address,cl.notes,0,0 FROM case_clients cc JOIN clients cl ON cl.id=cc.client_id WHERE cc.case_id=? AND cc.deleted_at IS NULL AND cl.deleted_at IS NULL ORDER BY cc.is_primary DESC,cl.name",new String[]{String.valueOf(caseId)})){while(c.moveToNext())out.add(new ClientRecord(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6),c.getString(7),0,0));}return out;}
    boolean isClientLinked(long caseId,long clientId){return scalarArgs("SELECT COUNT(*) FROM case_clients WHERE case_id=? AND client_id=? AND deleted_at IS NULL",new String[]{String.valueOf(caseId),String.valueOf(clientId)})>0;}

    long addCollaborator(String first,String last,String license,String body,String phone,String notes){ContentValues v=new ContentValues();identity(v);v.put("first_name",first);v.put("last_name",last);v.put("license_number",license);v.put("professional_body",body);v.put("phone",phone);v.put("notes",notes);return getWritableDatabase().insertOrThrow("collaborators",null,v);}
    List<CollaboratorRecord> collaborators(){ArrayList<CollaboratorRecord> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT id,first_name,last_name,license_number,professional_body,phone,notes FROM collaborators WHERE deleted_at IS NULL ORDER BY last_name,first_name",null)){while(c.moveToNext())out.add(new CollaboratorRecord(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6)));}return out;}
    void linkCollaborator(long caseId,long collaboratorId,String mode,double share,String notes){if(share<0||share>100)throw new IllegalArgumentException("درصد سهم باید بین صفر تا صد باشد");ContentValues v=new ContentValues();identity(v);v.put("case_id",caseId);v.put("collaborator_id",collaboratorId);v.put("collaboration_mode",mode);v.put("fee_share_percent",share);v.put("financial_notes",notes);getWritableDatabase().insertOrThrow("case_collaborators",null,v);}
    List<CaseCollaboratorRecord> caseCollaborators(long caseId){ArrayList<CaseCollaboratorRecord> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT x.id,c.id,c.first_name||' '||c.last_name,c.license_number,x.collaboration_mode,COALESCE(x.fee_share_percent,0),x.financial_notes FROM case_collaborators x JOIN collaborators c ON c.id=x.collaborator_id WHERE x.case_id=? AND x.deleted_at IS NULL AND c.deleted_at IS NULL ORDER BY c.last_name,c.first_name",new String[]{String.valueOf(caseId)})){while(c.moveToNext())out.add(new CaseCollaboratorRecord(c.getLong(0),c.getLong(1),c.getString(2),c.getString(3),c.getString(4),c.getDouble(5),c.getString(6)));}return out;}

    long addRepresentationContract(Long caseId,String number,String date,String subject,String scope,long fee,boolean opponentsAny,String opponents,boolean delegation,String collaborationMode,String notes,List<Long> clientIds,List<Long> collaboratorIds){
        if(blank(number))throw new IllegalArgumentException("شماره قرارداد الزامی است");if(clientIds==null||clientIds.isEmpty())throw new IllegalArgumentException("حداقل یک موکل باید به قرارداد متصل باشد");SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{ContentValues v=new ContentValues();identity(v);if(caseId!=null&&caseId>0)v.put("case_id",caseId);else v.putNull("case_id");v.put("contract_number",number);v.put("contract_date",date);v.put("subject",subject);v.put("authority_scope",scope);v.put("fee_amount",fee);v.put("opponents_any",opponentsAny?1:0);v.put("opponents_text",opponents);v.put("delegation_allowed",delegation?1:0);v.put("collaboration_mode",collaborationMode);v.put("notes",notes);long id=db.insertOrThrow("representation_contracts",null,v);for(Long clientId:clientIds){ContentValues link=new ContentValues();identity(link);link.put("contract_id",id);link.put("client_id",clientId);db.insertOrThrow("contract_clients",null,link);}if(collaboratorIds!=null)for(Long collaboratorId:collaboratorIds){ContentValues link=new ContentValues();identity(link);link.put("contract_id",id);link.put("collaborator_id",collaboratorId);link.put("collaboration_mode",collaborationMode);if(caseId!=null)try(Cursor share=db.rawQuery("SELECT fee_share_percent,financial_notes FROM case_collaborators WHERE case_id=? AND collaborator_id=? AND deleted_at IS NULL ORDER BY id DESC LIMIT 1",new String[]{String.valueOf(caseId),String.valueOf(collaboratorId)})){if(share.moveToFirst()){if(!share.isNull(0))link.put("fee_share_percent",share.getDouble(0));link.put("notes",share.getString(1));}}db.insertOrThrow("contract_collaborators",null,link);}db.setTransactionSuccessful();return id;}finally{db.endTransaction();}}
    List<RepresentationContractRecord> representationContracts(Long caseId,Long clientId){ArrayList<RepresentationContractRecord> out=new ArrayList<>();StringBuilder where=new StringBuilder(" WHERE rc.deleted_at IS NULL");ArrayList<String> args=new ArrayList<>();if(caseId!=null){where.append(" AND rc.case_id=?");args.add(String.valueOf(caseId));}if(clientId!=null){where.append(" AND EXISTS(SELECT 1 FROM contract_clients x WHERE x.contract_id=rc.id AND x.client_id=? AND x.deleted_at IS NULL)");args.add(String.valueOf(clientId));}try(Cursor c=getReadableDatabase().rawQuery("SELECT rc.id,rc.contract_number,rc.contract_date,rc.subject,rc.authority_scope,rc.fee_amount,rc.opponents_any,rc.opponents_text,rc.delegation_allowed,rc.collaboration_mode,rc.notes,COALESCE(rc.case_id,0) FROM representation_contracts rc"+where+" ORDER BY rc.contract_date DESC,rc.id DESC",args.toArray(new String[0]))){while(c.moveToNext())out.add(new RepresentationContractRecord(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getLong(5),c.getInt(6)!=0,c.getString(7),c.getInt(8)!=0,c.getString(9),c.getString(10),c.getLong(11)));}return out;}
    String contractClientNames(long contractId){return joined("SELECT GROUP_CONCAT(c.name,'، ') FROM contract_clients x JOIN clients c ON c.id=x.client_id WHERE x.contract_id=? AND x.deleted_at IS NULL AND c.deleted_at IS NULL",contractId);}
    String contractCollaboratorNames(long contractId){return joined("SELECT GROUP_CONCAT(c.first_name||' '||c.last_name,'، ') FROM contract_collaborators x JOIN collaborators c ON c.id=x.collaborator_id WHERE x.contract_id=? AND x.deleted_at IS NULL AND c.deleted_at IS NULL",contractId);}
    String caseOpponentText(long caseId){return joined("SELECT GROUP_CONCAT(CASE WHEN opponents_any=1 THEN 'هر شخص حقیقی یا حقوقی' ELSE opponents_text END,'، ') FROM representation_contracts WHERE case_id=? AND deleted_at IS NULL",caseId);}
    void deleteRepresentationContract(long id){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{String deleted=now();ContentValues tomb=new ContentValues();tomb.put("deleted_at",deleted);tomb.put("updated_at",deleted);db.update("contract_clients",tomb,"contract_id=? AND deleted_at IS NULL",new String[]{String.valueOf(id)});db.update("contract_collaborators",tomb,"contract_id=? AND deleted_at IS NULL",new String[]{String.valueOf(id)});if(db.update("representation_contracts",tomb,"id=? AND deleted_at IS NULL",new String[]{String.valueOf(id)})!=1)throw new IllegalArgumentException("قرارداد یافت نشد");db.setTransactionSuccessful();}finally{db.endTransaction();}}

    long addFinancialContract(long caseId,String date,long total,String terms,String notes){if(total<0)throw new IllegalArgumentException("مبلغ قرارداد نمی‌تواند منفی باشد");ContentValues v=new ContentValues();identity(v);v.put("case_id",caseId);v.put("contract_date",date);v.put("total_amount",total);v.put("payment_terms",terms);v.put("notes",notes);return getWritableDatabase().insertOrThrow("financial_contracts",null,v);}
    List<FinancialContractRecord> financialContracts(long caseId){ArrayList<FinancialContractRecord> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT id,contract_date,total_amount,payment_terms,notes FROM financial_contracts WHERE case_id=? AND deleted_at IS NULL ORDER BY contract_date DESC,id DESC",new String[]{String.valueOf(caseId)})){while(c.moveToNext())out.add(new FinancialContractRecord(c.getLong(0),c.getString(1),c.getLong(2),c.getString(3),c.getString(4)));}return out;}
    long addPaymentCheck(long caseId,Long installmentId,String number,String dueDate,long amount,String bank,String branch,String status,String notes){if(amount<=0)throw new IllegalArgumentException("مبلغ چک باید مثبت باشد");if(blank(number))throw new IllegalArgumentException("شماره چک الزامی است");SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{ContentValues v=new ContentValues();identity(v);v.put("case_id",caseId);if(installmentId!=null)v.put("installment_id",installmentId);v.put("check_number",number);v.put("due_date",dueDate);v.put("amount",amount);v.put("bank",bank);v.put("branch",branch);v.put("collection_status",status);v.put("notes",notes);long id=db.insertOrThrow("payment_checks",null,v);if("pending".equals(status))addStageReminders(db,"check",id,dueDate,"09:00",new int[]{7,3,2,1,0});db.setTransactionSuccessful();return id;}finally{db.endTransaction();}}
    List<PaymentCheckRecord> paymentChecks(Long caseId,String dueBefore){ArrayList<PaymentCheckRecord> out=new ArrayList<>();StringBuilder w=new StringBuilder(" WHERE deleted_at IS NULL");ArrayList<String>a=new ArrayList<>();if(caseId!=null){w.append(" AND case_id=?");a.add(String.valueOf(caseId));}if(dueBefore!=null){w.append(" AND due_date<=?");a.add(dueBefore);}try(Cursor c=getReadableDatabase().rawQuery("SELECT id,case_id,check_number,due_date,amount,bank,branch,collection_status,notes FROM payment_checks"+w+" ORDER BY due_date,id",a.toArray(new String[0]))){while(c.moveToNext())out.add(new PaymentCheckRecord(c.getLong(0),c.getLong(1),c.getString(2),c.getString(3),c.getLong(4),c.getString(5),c.getString(6),c.getString(7),c.getString(8)));}return out;}
    void updatePaymentCheckStatus(long id,String status){if(!"pending".equals(status)&&!"collected".equals(status)&&!"bounced".equals(status)&&!"cancelled".equals(status))throw new IllegalArgumentException("وضعیت چک معتبر نیست");SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{ContentValues v=new ContentValues();v.put("collection_status",status);v.put("updated_at",now());if(db.update("payment_checks",v,"id=? AND deleted_at IS NULL",new String[]{String.valueOf(id)})!=1)throw new IllegalArgumentException("چک یافت نشد");if(!"pending".equals(status)){ContentValues tomb=new ContentValues();tomb.put("deleted_at",now());tomb.put("updated_at",now());db.update("reminders",tomb,"target_type='check' AND target_id=? AND deleted_at IS NULL",new String[]{String.valueOf(id)});}db.setTransactionSuccessful();}finally{db.endTransaction();}}
    void deletePaymentCheck(long id){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{try(Cursor linked=db.rawQuery("SELECT COUNT(*) FROM installment_payments WHERE check_id=? AND deleted_at IS NULL",new String[]{String.valueOf(id)})){if(linked.moveToFirst()&&linked.getInt(0)>0)throw new IllegalArgumentException("چک به یک پرداخت وصول‌شده متصل است و قابل حذف نیست");}String deleted=now();ContentValues tomb=new ContentValues();tomb.put("deleted_at",deleted);tomb.put("updated_at",deleted);db.update("reminders",tomb,"target_type='check' AND target_id=? AND deleted_at IS NULL",new String[]{String.valueOf(id)});if(db.update("payment_checks",tomb,"id=? AND deleted_at IS NULL",new String[]{String.valueOf(id)})!=1)throw new IllegalArgumentException("چک یافت نشد");db.setTransactionSuccessful();}finally{db.endTransaction();}}

    long addCaseAttachment(long caseId,String name,String mime,String uri){if(blank(name)||blank(uri))throw new IllegalArgumentException("فایل معتبر نیست");ContentValues v=new ContentValues();identity(v);v.put("case_id",caseId);v.put("display_name",name);v.put("mime_type",blank(mime)?"application/octet-stream":mime);v.put("content_uri",uri);return getWritableDatabase().insertOrThrow("case_attachments",null,v);}
    List<CaseAttachmentRecord> caseAttachments(long caseId){ArrayList<CaseAttachmentRecord> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT id,case_id,display_name,mime_type,content_uri,created_at FROM case_attachments WHERE case_id=? AND deleted_at IS NULL ORDER BY id DESC",new String[]{String.valueOf(caseId)})){while(c.moveToNext())out.add(new CaseAttachmentRecord(c.getLong(0),c.getLong(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5)));}return out;}
    void deleteCaseAttachment(long id){softDelete("case_attachments",id);}

    void addTask(String title, String caseName, String date, String time,
                 String priority, String notes) {
        addTask(getWritableDatabase(), title, caseName, date, time, priority, notes);
    }

    void addLinkedTask(String title,String kind,Long caseId,Long clientId,String date,String start,String end,String place,String priority,String notes) {
        addLinkedTask(title,kind,null,false,caseId,clientId,date,start,end,place,priority,notes);
    }
    void addLinkedTask(String title,String kind,String category,boolean personal,Long caseId,Long clientId,String date,String start,String end,String place,String priority,String notes) {
        ContentValues v=new ContentValues();v.put("title",title);v.put("kind",kind);
        v.put("category",category);v.put("is_personal",personal?1:0);
        if(caseId!=null)v.put("case_id",caseId);if(clientId!=null)v.put("client_id",clientId);
        v.put("due_date",date);v.put("due_time",start);v.put("end_time",end);v.put("place",place);
        v.put("priority",priority);v.put("notes",notes);v.put("status","open");identity(v);
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{long id=db.insertOrThrow("tasks",null,v);addStageReminders(db,"task",id,date,start,new int[]{1,0});db.setTransactionSuccessful();}finally{db.endTransaction();}
    }

    private void addTask(SQLiteDatabase db, String title, String caseName, String date,
                         String time, String priority, String notes) {
        ContentValues v = new ContentValues(); v.put("title", title); v.put("case_name", caseName);
        v.put("due_date", date); v.put("due_time", time); v.put("priority", priority);
        v.put("notes", notes); v.put("status", "open"); identity(v); db.insertOrThrow("tasks", null, v);
    }

    void setTaskDone(long id, boolean done) {
        ContentValues v = new ContentValues(); v.put("done", done ? 1 : 0);
        v.put("status", done ? "done" : "open"); v.put("updated_at", now());
        getWritableDatabase().update("tasks", v, "id=? AND deleted_at IS NULL", new String[]{String.valueOf(id)});
    }

    void addLedger(long caseId, String kind, String category, long amount,
                   String date, String paidBy, String description) {
        ContentValues v = new ContentValues(); v.put("case_id", caseId); v.put("kind", kind);
        v.put("category", category); v.put("amount", amount); v.put("entry_date", date);
        v.put("paid_by", paidBy); v.put("description", description); identity(v);
        getWritableDatabase().insertOrThrow("ledger", null, v);
    }

    void addWorkLog(long caseId,long clientId,String type,String date,String description) {
        ContentValues v=new ContentValues(); v.put("case_id",caseId);
        if(clientId>0)v.put("client_id",clientId); else v.putNull("client_id");
        v.put("action_type",type); v.put("action_date",date); v.put("description",description); identity(v);
        getWritableDatabase().insertOrThrow("worklogs",null,v);
    }

    List<WorkLogRecord> workLogs(Long caseId,Long clientId) {
        ArrayList<WorkLogRecord> result=new ArrayList<>(); StringBuilder w=new StringBuilder(" WHERE 1=1"); ArrayList<String> a=new ArrayList<>();
        w.append(" AND w.deleted_at IS NULL");if(caseId!=null){w.append(" AND w.case_id=?");a.add(String.valueOf(caseId));}
        if(clientId!=null){w.append(" AND (w.client_id=? OR EXISTS(SELECT 1 FROM case_clients cc WHERE cc.case_id=w.case_id AND cc.client_id=? AND cc.deleted_at IS NULL))");a.add(String.valueOf(clientId));a.add(String.valueOf(clientId));}
        Cursor c=getReadableDatabase().rawQuery("SELECT w.id,w.case_id,w.client_id,w.action_type,w.action_date,w.description,cs.title,COALESCE(cl.name,(SELECT GROUP_CONCAT(cl2.name,'، ') FROM case_clients cc2 JOIN clients cl2 ON cl2.id=cc2.client_id WHERE cc2.case_id=w.case_id AND cc2.deleted_at IS NULL AND cl2.deleted_at IS NULL)) FROM worklogs w LEFT JOIN cases cs ON cs.id=w.case_id LEFT JOIN clients cl ON cl.id=w.client_id"+w+" ORDER BY w.action_date DESC,w.id DESC",a.toArray(new String[0]));
        while(c.moveToNext())result.add(new WorkLogRecord(c.getLong(0),c.getLong(1),c.getLong(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6),c.getString(7)));
        c.close();return result;
    }

    List<ClientRecord> clients() {
        ArrayList<ClientRecord> result = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT cl.id,cl.name,cl.national_id," +
                "cl.father_name,cl.birth_date,cl.phone,cl.address,cl.notes," +
                "COUNT(DISTINCT cs.id),COUNT(DISTINCT CASE WHEN cs.status='active' THEN cs.id END) " +
                "FROM clients cl LEFT JOIN case_clients cc ON cc.client_id=cl.id AND cc.deleted_at IS NULL LEFT JOIN cases cs ON cs.id=cc.case_id AND cs.deleted_at IS NULL WHERE cl.deleted_at IS NULL " +
                "GROUP BY cl.id ORDER BY cl.name", null);
        while (c.moveToNext()) result.add(new ClientRecord(c.getLong(0), c.getString(1),
                c.getString(2), c.getString(3), c.getString(4), c.getString(5),
                c.getString(6), c.getString(7), c.getInt(8), c.getInt(9)));
        c.close(); return result;
    }

    List<CaseRecord> cases(String status, String category, Long clientId) {
        ArrayList<CaseRecord> result = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE c.deleted_at IS NULL");
        ArrayList<String> args = new ArrayList<>();
        if (status != null) { where.append(" AND c.status=?"); args.add(status); }
        if (category != null && !"همه".equals(category)) { where.append(" AND c.category=?"); args.add(category); }
        if (clientId != null) { where.append(" AND EXISTS(SELECT 1 FROM case_clients filter_cc WHERE filter_cc.case_id=c.id AND filter_cc.client_id=? AND filter_cc.deleted_at IS NULL)"); args.add(String.valueOf(clientId)); }
        Cursor c = getReadableDatabase().rawQuery("SELECT c.id,c.title,c.reference,c.stage," +
                "COALESCE(c.client_id,0),(SELECT GROUP_CONCAT(cl2.name,'، ') FROM case_clients cc2 JOIN clients cl2 ON cl2.id=cc2.client_id WHERE cc2.case_id=c.id AND cc2.deleted_at IS NULL AND cl2.deleted_at IS NULL),c.fee_agreed,c.agreement_notes,c.status," +
                "c.category,c.case_number,c.archive_number,c.branch,c.subject,c.claim_text," +
                "c.evidence,c.summary,c.financial_notes,c.contract_notes,c.authority_type,c.province,c.judicial_city,c.contract_number,c.contract_date,c.judgment_number,c.order_number FROM cases c " +
                where + " ORDER BY c.id DESC",
                args.toArray(new String[0]));
        while (c.moveToNext()) result.add(caseFrom(c));
        c.close(); return result;
    }

    List<TaskRecord> tasksForDate(String date) {
        ArrayList<TaskRecord> result = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT t.id,t.title,COALESCE(cs.title,cl.name,t.case_name),t.due_date,t.due_time," +
                "t.priority,t.done,t.status,t.notes,t.kind,t.end_time,t.place,COALESCE(t.case_id,0),COALESCE(t.client_id,0) FROM tasks t LEFT JOIN cases cs ON cs.id=t.case_id LEFT JOIN clients cl ON cl.id=t.client_id WHERE t.deleted_at IS NULL AND t.due_date=? ORDER BY t.done,CASE t.priority " +
                "WHEN 'فوری' THEN 0 ELSE 1 END,due_time", new String[]{date});
        while (c.moveToNext()) result.add(taskFrom(c)); c.close(); return result;
    }

    List<TaskRecord> openTasks(int limit) {
        ArrayList<TaskRecord> result = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT t.id,t.title,COALESCE(cs.title,cl.name,t.case_name),t.due_date,t.due_time," +
                "t.priority,t.done,t.status,t.notes,t.kind,t.end_time,t.place,COALESCE(t.case_id,0),COALESCE(t.client_id,0) FROM tasks t LEFT JOIN cases cs ON cs.id=t.case_id LEFT JOIN clients cl ON cl.id=t.client_id WHERE t.deleted_at IS NULL AND t.done=0 ORDER BY t.due_date," +
                "CASE priority WHEN 'فوری' THEN 0 ELSE 1 END LIMIT " + limit, null);
        while (c.moveToNext()) result.add(taskFrom(c)); c.close(); return result;
    }

    int countCases(String status) { return status==null?scalar("SELECT COUNT(*) FROM cases WHERE deleted_at IS NULL",null):scalar("SELECT COUNT(*) FROM cases WHERE deleted_at IS NULL AND status=?", status); }
    int countClients() { return scalar("SELECT COUNT(*) FROM clients WHERE deleted_at IS NULL", null); }
    int countToday(String date) { return scalar("SELECT COUNT(*) FROM tasks WHERE deleted_at IS NULL AND due_date=? AND done=0", date); }
    int countAllTasks() { return scalar("SELECT COUNT(*) FROM tasks WHERE deleted_at IS NULL", null); }
    int countAppointments(String date) { return scalar("SELECT COUNT(*) FROM appointments WHERE deleted_at IS NULL AND visit_date=?", date); }
    int countDeadlines(String date) { return scalar("SELECT COUNT(*) FROM deadlines WHERE deleted_at IS NULL AND completed=0 AND due_date<=?", date); }
    int taskCountInMonth(String prefix) { return scalar("SELECT COUNT(*) FROM tasks WHERE deleted_at IS NULL AND due_date LIKE ?", prefix + "%"); }
    int countOverdueTasks(String today){return scalar("SELECT COUNT(*) FROM tasks WHERE deleted_at IS NULL AND done=0 AND due_date<?",today);}
    int countNeedsAction(String today){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(DISTINCT c.id) FROM cases c LEFT JOIN deadlines d ON d.case_id=c.id AND d.deleted_at IS NULL AND d.completed=0 AND d.due_date<=? LEFT JOIN tasks t ON t.case_id=c.id AND t.deleted_at IS NULL AND t.done=0 AND t.due_date<=? WHERE c.status='active' AND c.deleted_at IS NULL AND (d.id IS NOT NULL OR t.id IS NOT NULL)",new String[]{today,today})){return c.moveToFirst()?c.getInt(0):0;}}
    List<CaseRecord> casesNeedingAction(String threshold){HashSet<Long> ids=new HashSet<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT DISTINCT cs.id FROM cases cs LEFT JOIN deadlines d ON d.case_id=cs.id AND d.completed=0 AND d.deleted_at IS NULL AND d.due_date<=? LEFT JOIN tasks t ON t.case_id=cs.id AND t.done=0 AND t.deleted_at IS NULL AND t.due_date<=? WHERE cs.status='active' AND cs.deleted_at IS NULL AND (d.id IS NOT NULL OR t.id IS NOT NULL)",new String[]{threshold,threshold})){while(c.moveToNext())ids.add(c.getLong(0));}ArrayList<CaseRecord> out=new ArrayList<>();for(CaseRecord record:cases("active","همه",null))if(ids.contains(record.id))out.add(record);return out;}

    private int scalar(String sql, String arg) {
        Cursor c = getReadableDatabase().rawQuery(sql, arg == null ? null : new String[]{arg});
        int value = c.moveToFirst() ? c.getInt(0) : 0; c.close(); return value;
    }
    private int scalarArgs(String sql,String[] args){try(Cursor c=getReadableDatabase().rawQuery(sql,args)){return c.moveToFirst()?c.getInt(0):0;}}
    private String joined(String sql,long id){try(Cursor c=getReadableDatabase().rawQuery(sql,new String[]{String.valueOf(id)})){return c.moveToFirst()&&!c.isNull(0)?c.getString(0):"";}}

    List<LedgerRecord> ledger(long caseId) {
        ArrayList<LedgerRecord> result = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT id,kind,category,amount,entry_date," +
                "paid_by,description FROM ledger WHERE deleted_at IS NULL AND case_id=? ORDER BY id DESC",
                new String[]{String.valueOf(caseId)});
        while (c.moveToNext()) result.add(new LedgerRecord(c.getLong(0), c.getString(1),
                c.getString(2), c.getLong(3), c.getString(4), c.getString(5), c.getString(6)));
        c.close(); return result;
    }

    AccountSummary summary(CaseRecord record) {
        AccountSummary a = new AccountSummary(); a.agreed = record.feeAgreed;
        for (LedgerRecord l : ledger(record.id)) {
            if ("payment".equals(l.kind) && "حق‌الوکاله".equals(l.category)) a.received += l.amount;
            if ("payment".equals(l.kind) && "بازپرداخت هزینه".equals(l.category)) a.reimbursed += l.amount;
            if ("expense".equals(l.kind)) { a.expenses += l.amount; if ("وکیل".equals(l.paidBy)) a.lawyerPaid += l.amount; }
        }
        long net = a.agreed + a.lawyerPaid - a.received - a.reimbursed;
        a.debt = Math.max(0, net); a.credit = Math.max(0, -net); return a;
    }

    long addInstallment(long caseId,String title,long amount,String dueDate){if(amount<=0)throw new IllegalArgumentException("مبلغ قسط باید مثبت باشد");SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{ContentValues v=new ContentValues();v.put("case_id",caseId);v.put("title",title);v.put("amount",amount);v.put("due_date",dueDate);identity(v);long id=db.insertOrThrow("installments",null,v);addStageReminders(db,"installment",id,dueDate,"09:00",new int[]{7,3,2,1,0});db.setTransactionSuccessful();return id;}finally{db.endTransaction();}}
    List<InstallmentRecord> installments(long caseId){ArrayList<InstallmentRecord> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT i.id,i.title,i.amount,i.due_date,COALESCE(SUM(CASE WHEN ip.deleted_at IS NULL AND l.deleted_at IS NULL THEN ip.amount ELSE 0 END),0) paid FROM installments i LEFT JOIN installment_payments ip ON ip.installment_id=i.id LEFT JOIN ledger l ON l.id=ip.ledger_id WHERE i.case_id=? AND i.deleted_at IS NULL GROUP BY i.id ORDER BY i.due_date,i.id",new String[]{String.valueOf(caseId)})){while(c.moveToNext())out.add(new InstallmentRecord(c.getLong(0),c.getString(1),c.getLong(2),c.getString(3),c.getLong(4)));}return out;}
    void payInstallment(long id,long amount,String date,String payer,String description){payInstallment(id,amount,date,payer,"transfer",null,description);}
    void payInstallment(long id,long amount,String date,String payer,String paymentMethod,Long checkId,String description){if(amount<=0)throw new IllegalArgumentException("مبلغ پرداخت باید مثبت باشد");SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{long caseId,total,paid;try(Cursor c=db.rawQuery("SELECT i.case_id,i.amount,COALESCE(SUM(CASE WHEN ip.deleted_at IS NULL AND l.deleted_at IS NULL THEN ip.amount ELSE 0 END),0) FROM installments i LEFT JOIN installment_payments ip ON ip.installment_id=i.id LEFT JOIN ledger l ON l.id=ip.ledger_id WHERE i.id=? AND i.deleted_at IS NULL GROUP BY i.id",new String[]{String.valueOf(id)})){if(!c.moveToFirst())throw new IllegalArgumentException("قسط یافت نشد");caseId=c.getLong(0);total=c.getLong(1);paid=c.getLong(2);}if(amount>total-paid)throw new IllegalArgumentException("پرداخت بیش از مانده قسط است");ContentValues l=new ContentValues();l.put("case_id",caseId);l.put("kind","payment");l.put("category","حق‌الوکاله");l.put("amount",amount);l.put("entry_date",date);l.put("paid_by",payer);l.put("description",description);identity(l);long ledgerId=db.insertOrThrow("ledger",null,l);ContentValues p=new ContentValues();p.put("installment_id",id);p.put("ledger_id",ledgerId);p.put("amount",amount);p.put("payment_method",paymentMethod);if(checkId!=null)p.put("check_id",checkId);identity(p);db.insertOrThrow("installment_payments",null,p);db.setTransactionSuccessful();}finally{db.endTransaction();}}

    long addAppointment(String kind,String person,String phone,Long clientId,Long caseId,String date,String start,String end,String place,String notes) {
        ContentValues v=new ContentValues();v.put("kind",kind);v.put("person_name",person);v.put("contact_phone",phone);
        if(clientId!=null)v.put("client_id",clientId);if(caseId!=null)v.put("case_id",caseId);
        v.put("visit_date",date);v.put("start_time",start);v.put("end_time",end);v.put("place",place);v.put("notes",notes);identity(v);
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{long id=db.insertOrThrow("appointments",null,v);addStageReminders(db,"appointment",id,date,start,new int[]{7,3,2,1,0});db.setTransactionSuccessful();return id;}finally{db.endTransaction();}
    }

    List<AppointmentRecord> appointments(String date){ArrayList<AppointmentRecord> list=new ArrayList<>();
        String sql="SELECT a.id,a.kind,a.person_name,a.contact_phone,a.visit_date,a.start_time,a.end_time,a.place,a.notes,COALESCE(a.client_id,0),COALESCE(a.case_id,0),c.title,a.branch,a.authority,a.city,a.attendance_status,a.completed_at FROM appointments a LEFT JOIN cases c ON c.id=a.case_id WHERE a.deleted_at IS NULL"+(date==null?"":" AND a.visit_date=?")+" ORDER BY a.visit_date,a.start_time,a.id";
        Cursor c=getReadableDatabase().rawQuery(sql,date==null?null:new String[]{date});
        while(c.moveToNext()){AppointmentRecord record=new AppointmentRecord(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6),c.getString(7),c.getString(8),c.getLong(9),c.getLong(10),c.getString(11));record.branch=c.getString(12);record.authority=c.getString(13);record.city=c.getString(14);record.attendance=c.getString(15);record.completedAt=c.getString(16);list.add(record);}c.close();return list;
    }

    long addDeadline(long caseId,String title,String eventDate,String dueDate,int duration,String notes){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{ContentValues v=new ContentValues();v.put("case_id",caseId);v.put("title",title);v.put("event_date",eventDate);v.put("due_date",dueDate);v.put("duration_days",duration);v.put("notes",notes);identity(v);long id=db.insertOrThrow("deadlines",null,v);addStageReminders(db,"deadline",id,dueDate,"09:00",new int[]{7,3,2,1,0});db.setTransactionSuccessful();return id;}finally{db.endTransaction();}}
    private void addStageReminders(SQLiteDatabase db,String type,long target,String date,String clock,int[] days){java.util.Calendar base=JalaliDate.calendar(date);String[] hm=clock!=null&&clock.matches("[0-2][0-9]:[0-5][0-9]")?clock.split(":"):new String[]{"09","00"};base.set(java.util.Calendar.HOUR_OF_DAY,Integer.parseInt(hm[0]));base.set(java.util.Calendar.MINUTE,Integer.parseInt(hm[1]));for(int advance:days){java.util.Calendar at=(java.util.Calendar)base.clone();at.add(java.util.Calendar.DAY_OF_YEAR,-advance);ContentValues r=new ContentValues();r.put("target_type",type);r.put("target_id",target);r.put("advance_days",advance);r.put("trigger_at",at.getTimeInMillis());identity(r);db.insertOrThrow("reminders",null,r);}}
    void ensureReminderRules(){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{String[] types={"deadline","appointment","task","installment","check"},queries={"SELECT id,due_date,'09:00' FROM deadlines WHERE completed=0 AND deleted_at IS NULL","SELECT id,visit_date,start_time FROM appointments WHERE deleted_at IS NULL AND attendance_status='planned'","SELECT id,due_date,due_time FROM tasks WHERE done=0 AND due_date IS NOT NULL AND deleted_at IS NULL","SELECT i.id,i.due_date,'09:00' FROM installments i WHERE i.deleted_at IS NULL AND i.amount>(SELECT COALESCE(SUM(ip.amount),0) FROM installment_payments ip WHERE ip.installment_id=i.id AND ip.deleted_at IS NULL)","SELECT id,due_date,'09:00' FROM payment_checks WHERE deleted_at IS NULL AND collection_status='pending'"};for(int i=0;i<types.length;i++)try(Cursor c=db.rawQuery(queries[i],null)){while(c.moveToNext()){String type=types[i];long id=c.getLong(0);try(Cursor exists=db.rawQuery("SELECT 1 FROM reminders WHERE target_type=? AND target_id=? LIMIT 1",new String[]{type,String.valueOf(id)})){if(exists.moveToFirst())continue;}try{addStageReminders(db,type,id,JalaliDate.parse(c.getString(1)).value(),c.getString(2),(i!=2)?new int[]{7,3,2,1,0}:new int[]{1,0});}catch(IllegalArgumentException invalidDate){/* Legacy free-text dates do not block database opening. */}}}db.setTransactionSuccessful();}finally{db.endTransaction();}}
    void completeDeadline(long id){setScheduleStatus("deadline",id,"completed");}

    void setScheduleStatus(String type,long id,String state) {
        if(!java.util.Arrays.asList("planned","completed","cancelled","absent").contains(state))
            throw new IllegalArgumentException("وضعیت معتبر نیست");
        ContentValues v=new ContentValues();String timestamp=now();
        if("deadline".equals(type))v.put("completed","completed".equals(state)?1:"cancelled".equals(state)?2:0);
        else if("appointment".equals(type))v.put("attendance_status",state);
        else throw new IllegalArgumentException("نوع موعد معتبر نیست");
        if("completed".equals(state))v.put("completed_at",timestamp);else v.putNull("completed_at");
        v.put("updated_at",timestamp);
        getWritableDatabase().update("deadline".equals(type)?"deadlines":"appointments",v,"id=? AND deleted_at IS NULL",new String[]{String.valueOf(id)});
    }

    void setAppointmentLocation(long id,String branch,String authority,String city) {
        ContentValues v=new ContentValues();v.put("branch",branch);v.put("authority",authority);v.put("city",city);v.put("updated_at",now());
        getWritableDatabase().update("appointments",v,"id=?",new String[]{String.valueOf(id)});
    }

    void setDeadlineClient(long id,long caseId,Long clientId) {
        if(clientId!=null&&!isClientLinked(caseId,clientId))throw new IllegalArgumentException("موکل به این پرونده مرتبط نیست");
        ContentValues v=new ContentValues();if(clientId==null)v.putNull("client_id");else v.put("client_id",clientId);v.put("updated_at",now());
        getWritableDatabase().update("deadlines",v,"id=? AND case_id=?",new String[]{String.valueOf(id),String.valueOf(caseId)});
    }

    /** Per-target reminders, including preparation events; no global fixed-stage replacement. */
    long addCustomReminder(String type,long target,String date,String time,String label) {
        if(!"deadline".equals(type)&&!"appointment".equals(type))throw new IllegalArgumentException("نوع موعد معتبر نیست");
        java.util.Calendar c=JalaliDate.calendar(JalaliDate.parse(date).value());
        String[] parts=InputValidators.normalizeClock(time).split(":");
        c.set(java.util.Calendar.HOUR_OF_DAY,Integer.parseInt(parts[0]));c.set(java.util.Calendar.MINUTE,Integer.parseInt(parts[1]));
        c.set(java.util.Calendar.SECOND,0);c.set(java.util.Calendar.MILLISECOND,0);
        if(c.getTimeInMillis()<=System.currentTimeMillis())throw new IllegalArgumentException("یادآوری باید در آینده باشد");
        SQLiteDatabase database=getWritableDatabase();database.beginTransaction();
        try {
            try(Cursor existing=database.rawQuery("SELECT id FROM reminders WHERE target_type=? AND target_id=? AND trigger_at=? AND deleted_at IS NULL",new String[]{type,String.valueOf(target),String.valueOf(c.getTimeInMillis())})) {
                if(existing.moveToFirst())throw new IllegalArgumentException("برای این ساعت یادآوری ثبت شده است");
            }
            ContentValues v=new ContentValues();identity(v);v.put("target_type",type);v.put("target_id",target);v.put("trigger_at",c.getTimeInMillis());v.put("advance_days",0);v.put("label",label);
            long id=database.insertOrThrow("reminders",null,v);database.setTransactionSuccessful();return id;
        } finally {database.endTransaction();}
    }

    List<ReminderRecord> remindersFor(String type,long target) {
        ArrayList<ReminderRecord> list=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT id,trigger_at,advance_days,target_type,label FROM reminders WHERE target_type=? AND target_id=? AND deleted_at IS NULL ORDER BY trigger_at",new String[]{type,String.valueOf(target)})) {
            while(c.moveToNext()){ReminderRecord r=new ReminderRecord(c.getLong(0),c.getLong(1),c.getInt(2),c.getString(3));r.label=c.getString(4);list.add(r);}
        }
        return list;
    }

    void removeReminder(long id) {
        ContentValues v=new ContentValues();v.put("deleted_at",now());v.put("updated_at",now());
        getWritableDatabase().update("reminders",v,"id=?",new String[]{String.valueOf(id)});
    }

    String reminderLabel(long id) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT label FROM reminders WHERE id=?",new String[]{String.valueOf(id)})){return c.moveToFirst()?c.getString(0):"";}
    }

    void changeScheduleDate(String type,long id,String date,String start,String end) {
        date=JalaliDate.parse(date).value();
        boolean deadline="deadline".equals(type);
        if(!deadline&&!"appointment".equals(type))throw new IllegalArgumentException("نوع موعد معتبر نیست");
        if(!deadline){start=InputValidators.normalizeClock(start);end=blank(end)?"":InputValidators.normalizeClock(end);if(!blank(end)&&end.compareTo(start)<=0)throw new IllegalArgumentException("پایان باید بعد از شروع باشد");}
        SQLiteDatabase database=getWritableDatabase();database.beginTransaction();
        try {
            if(deadline)try(Cursor c=database.rawQuery("SELECT event_date FROM deadlines WHERE id=?",new String[]{String.valueOf(id)})){if(!c.moveToFirst()||JalaliDate.daysBetween(c.getString(0),date)<0)throw new IllegalArgumentException("تاریخ نهایی پیش از شروع است");}
            ContentValues v=new ContentValues();v.put(deadline?"due_date":"visit_date",date);v.put("updated_at",now());
            if(!deadline){v.put("start_time",start);v.put("end_time",end);}
            database.update(deadline?"deadlines":"appointments",v,"id=?",new String[]{String.valueOf(id)});
            ContentValues old=new ContentValues();old.put("deleted_at",now());old.put("updated_at",now());
            database.update("reminders",old,"target_type=? AND target_id=? AND deleted_at IS NULL",new String[]{type,String.valueOf(id)});
            addStageReminders(database,type,id,date,deadline?"09:00":start,new int[]{7,3,2,1,0});
            database.setTransactionSuccessful();
        } finally {database.endTransaction();}
    }
    private static final String ACTIVE_REMINDER="((target_type='deadline' AND EXISTS(SELECT 1 FROM deadlines d JOIN cases cs ON cs.id=d.case_id WHERE d.id=reminders.target_id AND d.completed=0 AND d.deleted_at IS NULL AND cs.deleted_at IS NULL)) OR (target_type='task' AND EXISTS(SELECT 1 FROM tasks t WHERE t.id=reminders.target_id AND t.done=0 AND t.deleted_at IS NULL)) OR (target_type='appointment' AND EXISTS(SELECT 1 FROM appointments a WHERE a.id=reminders.target_id AND a.deleted_at IS NULL AND a.attendance_status='planned')) OR (target_type='installment' AND EXISTS(SELECT 1 FROM installments i WHERE i.id=reminders.target_id AND i.deleted_at IS NULL AND i.amount>(SELECT COALESCE(SUM(ip.amount),0) FROM installment_payments ip WHERE ip.installment_id=i.id AND ip.deleted_at IS NULL))) OR (target_type='check' AND EXISTS(SELECT 1 FROM payment_checks pc WHERE pc.id=reminders.target_id AND pc.deleted_at IS NULL AND pc.collection_status='pending')))";
    List<ReminderRecord> pendingReminders(){ArrayList<ReminderRecord> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT id,trigger_at,advance_days,target_type FROM reminders WHERE deleted_at IS NULL AND fired_at IS NULL AND trigger_at>? AND "+ACTIVE_REMINDER+" ORDER BY trigger_at LIMIT 1000",new String[]{String.valueOf(System.currentTimeMillis())})){while(c.moveToNext())out.add(new ReminderRecord(c.getLong(0),c.getLong(1),c.getInt(2),c.getString(3)));}return out;}
    boolean fireReminder(long id){ContentValues v=new ContentValues();v.put("fired_at",now());v.put("updated_at",now());SQLiteDatabase db=getWritableDatabase();return db.update("reminders",v,"id=? AND fired_at IS NULL AND deleted_at IS NULL AND "+ACTIVE_REMINDER,new String[]{String.valueOf(id)})==1;}
    List<DeadlineRecord> deadlines(Long caseId,boolean openOnly){ArrayList<DeadlineRecord> list=new ArrayList<>();ArrayList<String> args=new ArrayList<>();StringBuilder w=new StringBuilder(" WHERE d.deleted_at IS NULL AND c.deleted_at IS NULL");if(caseId!=null){w.append(" AND d.case_id=?");args.add(String.valueOf(caseId));}if(openOnly)w.append(" AND d.completed=0");Cursor c=getReadableDatabase().rawQuery("SELECT d.id,d.case_id,d.title,d.event_date,d.due_date,d.duration_days,d.notes,d.completed,c.title,c.case_number,cl.name FROM deadlines d JOIN cases c ON c.id=d.case_id LEFT JOIN clients cl ON cl.id=COALESCE(d.client_id,c.client_id)"+w+" ORDER BY d.due_date,d.id",args.toArray(new String[0]));while(c.moveToNext())list.add(new DeadlineRecord(c.getLong(0),c.getLong(1),c.getString(2),c.getString(3),c.getString(4),c.getInt(5),c.getString(6),c.getInt(7),c.getString(8),c.getString(9),c.getString(10)));c.close();return list;}

    String clientFinancialText(long clientId){StringBuilder b=new StringBuilder();for(CaseRecord c:cases(null,"همه",clientId))b.append(caseFinancialText(c));return b.toString();}
    String caseFinancialText(CaseRecord c){StringBuilder b=new StringBuilder();AccountSummary s=summary(c);b.append("صورت‌حساب موردی پرونده در تاریخ ").append(JalaliDate.today().value()).append("\nموکل: ").append(c.clientName).append(" | پرونده: ").append(c.title).append(" | شماره: ").append(c.caseNumber).append("\nقرارداد شماره ").append(c.contractNumber).append(" مورخ ").append(c.contractDate).append("\nتوافق حق‌الوکاله: ").append(s.agreed).append(" ریال؛ دریافتی: ").append(s.received).append(" ریال\nهزینه‌ها: ").append(s.expenses).append(" ریال؛ هزینه پرداختی وکیل: ").append(s.lawyerPaid).append(" ریال؛ بدهی: ").append(s.debt).append(" ریال؛ بستانکاری موکل: ").append(s.credit).append(" ریال\nقرارداد/توافق: ").append(c.contractNotes).append(" / ").append(c.agreementNotes).append("\nاقساط:\n");for(InstallmentRecord i:installments(c.id))b.append(i.dueDate).append(" | ").append(i.title).append(" | تعهد: ").append(i.amount).append(" ریال | پرداخت: ").append(i.paid).append(" ریال | مانده: ").append(i.amount-i.paid).append(" ریال\n");long planned=0,allocated=0;for(InstallmentRecord i:installments(c.id)){planned+=i.amount;allocated+=i.paid;}b.append("جمع اقساط: ").append(planned).append(" ریال | مبلغ توافقی: ").append(s.agreed).append(" ریال | دریافت خارج از اقساط: ").append(Math.max(0,s.received-allocated)).append(" ریال\nریز دریافت‌ها و مخارج:\n");
        for(LedgerRecord l:ledger(c.id))b.append(l.date).append(" | ").append("expense".equals(l.kind)?"هزینه":"دریافت").append(" | ").append(l.category).append(" | ").append(l.amount).append(" ریال | پرداخت‌کننده: ").append(l.paidBy).append(" | ").append(l.description).append("\n");b.append("\nاین صورت‌حساب پیش از امضا باید با اسناد و قرارداد پرونده تطبیق داده شود.\nنام و امضای موکل: ........................  تاریخ: .................\nنام و امضای وکیل: ........................  تاریخ: .................\n\n");return b.toString();}

    String performanceText(Long caseId,Long clientId){StringBuilder b=new StringBuilder();ArrayList<Long> ids=new ArrayList<>();for(CaseRecord c:cases(null,"همه",clientId))if(caseId==null||c.id==caseId){ids.add(c.id);b.append("پرونده: ").append(c.title).append(" | شماره: ").append(c.caseNumber).append(" | موکل: ").append(c.clientName).append("\n");for(WorkLogRecord x:workLogs(c.id,null))b.append(x.date).append(" | اقدام: ").append(x.type).append(" | ").append(x.description).append("\n");for(AppointmentRecord a:appointments(null))if(a.caseId==c.id)b.append(a.date).append(" | ").append(a.start).append(" تا ").append(a.end).append(" | ").append(a.kind).append(" | ").append(a.person).append(" | ").append(a.place).append("\n");for(LedgerRecord l:ledger(c.id))if("expense".equals(l.kind)&&"وکیل".equals(l.paidBy))b.append(l.date).append(" | هزینه پرداختی وکیل: ").append(l.category).append(" | ").append(l.amount).append(" ریال | ").append(l.description).append("\n");b.append("\n");}if(caseId==null&&clientId!=null)for(AppointmentRecord a:appointments(null))if(a.caseId==0&&a.clientId==clientId)b.append(a.date).append(" | ").append(a.start).append(" تا ").append(a.end).append(" | ").append(a.kind).append(" | ").append(a.person).append(" | ").append(a.place).append("\n");return b.toString();}

    int countDemoRows(){int total=0;for(String table:new String[]{"clients","cases","tasks","ledger","worklogs","appointments","deadlines","installments","installment_payments","case_clients","collaborators","case_collaborators","representation_contracts","contract_clients","contract_collaborators","financial_contracts","payment_checks","case_attachments"})total+=scalar("SELECT COUNT(*) FROM "+table+" WHERE is_demo=1 AND deleted_at IS NULL",null);return total;}
    int countLegalDocuments(){return scalar("SELECT COUNT(*) FROM legal_documents WHERE deleted_at IS NULL",null);}
    List<LegalDocumentRecord> searchLegalDocuments(String query){ArrayList<LegalDocumentRecord> out=new ArrayList<>();String q="%"+(query==null?"":query.trim())+"%";try(Cursor c=getReadableDatabase().rawQuery("SELECT id,kind,title,number,subject,body,source_url,content_version FROM legal_documents WHERE deleted_at IS NULL AND (?='%%' OR title LIKE ? OR number LIKE ? OR subject LIKE ? OR body LIKE ?) ORDER BY title LIMIT 250",new String[]{q,q,q,q,q})){while(c.moveToNext())out.add(new LegalDocumentRecord(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6),c.getString(7)));}return out;}
    void softDeleteDemoData(){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{String t=now();ContentValues tomb=new ContentValues();tomb.put("deleted_at",t);tomb.put("updated_at",t);for(String table:new String[]{"installment_payments","contract_collaborators","contract_clients","case_collaborators","case_clients","case_attachments","payment_checks","installments","financial_contracts","representation_contracts","appointments","deadlines","tasks","ledger","worklogs","collaborators","cases","clients"})db.update(table,tomb,"is_demo=1 AND deleted_at IS NULL",null);db.setTransactionSuccessful();}finally{db.endTransaction();}}
    List<DeletedRecord> deletedRecords(){ArrayList<DeletedRecord> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT 'case',id,title,deleted_at FROM cases WHERE deleted_at IS NOT NULL UNION ALL SELECT 'client',id,name,deleted_at FROM clients WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC",null)){while(c.moveToNext())out.add(new DeletedRecord(c.getString(0),c.getLong(1),c.getString(2),c.getString(3)));}return out;}
    void restoreCase(long id){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{String deleted;try(Cursor c=db.rawQuery("SELECT deleted_at FROM cases WHERE id=? AND deleted_at IS NOT NULL",new String[]{String.valueOf(id)})){if(!c.moveToFirst())throw new IllegalArgumentException("پرونده حذف‌شده یافت نشد");deleted=c.getString(0);}ContentValues live=new ContentValues();live.putNull("deleted_at");live.put("updated_at",now());db.update("contract_clients",live,"contract_id IN(SELECT id FROM representation_contracts WHERE case_id=?) AND deleted_at=?",new String[]{String.valueOf(id),deleted});db.update("contract_collaborators",live,"contract_id IN(SELECT id FROM representation_contracts WHERE case_id=?) AND deleted_at=?",new String[]{String.valueOf(id),deleted});for(String table:new String[]{"tasks","ledger","worklogs","appointments","deadlines","installments","case_clients","case_collaborators","representation_contracts","financial_contracts","payment_checks","case_attachments"})db.update(table,live,"case_id=? AND deleted_at=?",new String[]{String.valueOf(id),deleted});db.update("installment_payments",live,"installment_id IN(SELECT id FROM installments WHERE case_id=?) AND deleted_at=?",new String[]{String.valueOf(id),deleted});db.update("cases",live,"id=?",new String[]{String.valueOf(id)});db.setTransactionSuccessful();}finally{db.endTransaction();}}
    void restoreClient(long id){ContentValues live=new ContentValues();live.putNull("deleted_at");live.put("updated_at",now());if(getWritableDatabase().update("clients",live,"id=? AND deleted_at IS NOT NULL",new String[]{String.valueOf(id)})!=1)throw new IllegalArgumentException("موکل حذف‌شده یافت نشد");}
    void clearAllOfficeData(){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{for(String table:deleteOrder())db.delete(table,null,null);db.setTransactionSuccessful();}finally{db.endTransaction();}}

    String exportJson() throws Exception {
        JSONObject root = new JSONObject(); root.put("format", "KLO-2"); root.put("created", now());
        JSONObject tables = new JSONObject();
        for (String table : backupTables())
            tables.put(table, dump(table));
        root.put("tables", tables); return root.toString();
    }

    void importJson(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        if (!"KLO-2".equals(root.optString("format"))) throw new Exception("نسخه پشتیبان معتبر نیست");
        JSONObject tables = root.getJSONObject("tables");
        for(String table:new String[]{"clients","tasks","cases","ledger","worklogs","appointments","deadlines"})if(!(tables.opt(table) instanceof JSONArray))throw new Exception("فایل پشتیبان ناقص است: "+table);
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for(String table:deleteOrder())db.delete(table,null,null);
            for (String table : backupTables())
                if(tables.has(table))restore(db, table, tables.getJSONArray(table));
            backfillCaseClients(db);
            try(Cursor check=db.rawQuery("PRAGMA foreign_key_check",null)){if(check.moveToFirst())throw new Exception("ارتباط داده‌های پشتیبان نامعتبر است");}
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    private static String[] backupTables(){return new String[]{"clients","cases","collaborators","case_clients","case_collaborators","representation_contracts","contract_clients","contract_collaborators","financial_contracts","installments","payment_checks","case_attachments","ledger","installment_payments","tasks","worklogs","appointments","deadlines","reminders","legal_taxonomy","legal_documents","legal_sync_state"};}
    private static String[] deleteOrder(){return new String[]{"reminders","installment_payments","contract_collaborators","contract_clients","case_collaborators","case_clients","case_attachments","payment_checks","installments","financial_contracts","representation_contracts","appointments","deadlines","tasks","ledger","worklogs","collaborators","cases","clients","legal_documents","legal_taxonomy","legal_sync_state"};}

    private JSONArray dump(String table) throws Exception {
        JSONArray array = new JSONArray(); Cursor c = getReadableDatabase().rawQuery("SELECT * FROM " + table, null);
        while (c.moveToNext()) { JSONObject row = new JSONObject();
            for (int i = 0; i < c.getColumnCount(); i++) {
                if (c.isNull(i)) row.put(c.getColumnName(i), JSONObject.NULL);
                else if (c.getType(i) == Cursor.FIELD_TYPE_INTEGER) row.put(c.getColumnName(i), c.getLong(i));
                else row.put(c.getColumnName(i), c.getString(i));
            } array.put(row);
        } c.close(); return array;
    }

    private void restore(SQLiteDatabase db, String table, JSONArray rows) throws Exception {
        HashSet<String> allowed=new HashSet<>();try(Cursor columns=db.rawQuery("PRAGMA table_info("+table+")",null)){while(columns.moveToNext())allowed.add(columns.getString(1));}
        for (int i = 0; i < rows.length(); i++) { JSONObject row = rows.getJSONObject(i); ContentValues v = new ContentValues();
            Iterator<String> keys = row.keys(); while (keys.hasNext()) { String key = keys.next(); Object value = row.get(key);
                if(!allowed.contains(key))continue;
                if (value == JSONObject.NULL) v.putNull(key); else if (value instanceof Number) v.put(key, ((Number) value).longValue()); else v.put(key, String.valueOf(value));
            } if(allowed.contains("uid")&&(!v.containsKey("uid")||v.getAsString("uid")==null))v.put("uid",UUID.randomUUID().toString());if(allowed.contains("updated_at")&&!v.containsKey("updated_at"))v.put("updated_at",now());db.insertOrThrow(table, null, v);
        }
    }

    private ContentValues caseValues(CaseRecord c) {
        ContentValues v = new ContentValues(); v.put("title", c.title); v.put("reference", c.reference);
        v.put("stage", c.stage); if (c.clientId > 0) v.put("client_id", c.clientId); else v.putNull("client_id");
        v.put("fee_agreed", c.feeAgreed); v.put("agreement_notes", c.agreementNotes);
        v.put("status", c.status); v.put("category", c.category); v.put("case_number", c.caseNumber);
        v.put("archive_number", c.archiveNumber); v.put("branch", c.branch); v.put("subject", c.subject);
        v.put("claim_text", c.claimText); v.put("evidence", c.evidence); v.put("summary", c.summary);
        v.put("financial_notes", c.financialNotes); v.put("contract_notes", c.contractNotes);v.put("contract_number",c.contractNumber);v.put("contract_date",c.contractDate);v.put("judgment_number",c.judgmentNumber);v.put("order_number",c.orderNumber);
        v.put("authority_type",c.authorityType); v.put("province",c.province); v.put("judicial_city",c.judicialCity); return v;
    }

    private CaseRecord caseFrom(Cursor c) { CaseRecord r=new CaseRecord(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4), c.getString(5), c.getLong(6), c.getString(7), c.getString(8), c.getString(9), c.getString(10), c.getString(11), c.getString(12), c.getString(13), c.getString(14), c.getString(15), c.getString(16), c.getString(17), c.getString(18),c.getString(19),c.getString(20),c.getString(21));r.contractNumber=c.getString(22);r.contractDate=c.getString(23);r.judgmentNumber=c.getString(24);r.orderNumber=c.getString(25);return r; }
    private TaskRecord taskFrom(Cursor c) { return new TaskRecord(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getInt(6), c.getString(7), c.getString(8),c.getString(9),c.getString(10),c.getString(11),c.getLong(12),c.getLong(13)); }
    private static String now() { SimpleDateFormat utc=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",Locale.US);utc.setTimeZone(TimeZone.getTimeZone("UTC"));return utc.format(new Date()); }
    private static boolean blank(String value){return value==null||value.trim().isEmpty();}

    static final class ClientRecord {
        final long id; final String name,nationalId,father,birth,phone,address,notes; final int caseCount,activeCount;
        ClientRecord(long id,String name,String nationalId,String father,String birth,String phone,String address,String notes,int caseCount,int activeCount){this.id=id;this.name=name;this.nationalId=nationalId;this.father=father;this.birth=birth;this.phone=phone;this.address=address;this.notes=notes;this.caseCount=caseCount;this.activeCount=activeCount;}
        @Override public String toString(){return name;}
    }
    static final class CaseRecord {
        long id,clientId,feeAgreed; String title,reference,stage,clientName,agreementNotes,status,category,caseNumber,archiveNumber,branch,subject,claimText,evidence,summary,financialNotes,contractNotes,contractNumber,contractDate,judgmentNumber,orderNumber,authorityType,province,judicialCity;
        CaseRecord(){status="active";category="حقوقی";}
        CaseRecord(long id,String title,String reference,String stage,long clientId,String clientName,long fee,String agreement,String status,String category,String caseNumber,String archiveNumber,String branch,String subject,String claimText,String evidence,String summary,String financialNotes,String contractNotes,String authorityType,String province,String judicialCity){this.id=id;this.title=title;this.reference=reference;this.stage=stage;this.clientId=clientId;this.clientName=clientName;this.feeAgreed=fee;this.agreementNotes=agreement;this.status=status;this.category=category;this.caseNumber=caseNumber;this.archiveNumber=archiveNumber;this.branch=branch;this.subject=subject;this.claimText=claimText;this.evidence=evidence;this.summary=summary;this.financialNotes=financialNotes;this.contractNotes=contractNotes;this.authorityType=authorityType;this.province=province;this.judicialCity=judicialCity;}
    }
    static final class TaskRecord { final long id,caseId,clientId; final String title,caseName,date,time,priority,status,notes,kind,endTime,place; final int done; TaskRecord(long id,String title,String caseName,String date,String time,String priority,int done,String status,String notes,String kind,String endTime,String place,long caseId,long clientId){this.id=id;this.title=title;this.caseName=caseName;this.date=date;this.time=time;this.priority=priority;this.done=done;this.status=status;this.notes=notes;this.kind=kind;this.endTime=endTime;this.place=place;this.caseId=caseId;this.clientId=clientId;} }
    static final class LedgerRecord { final long id,amount; final String kind,category,date,paidBy,description; LedgerRecord(long id,String kind,String category,long amount,String date,String paidBy,String description){this.id=id;this.kind=kind;this.category=category;this.amount=amount;this.date=date;this.paidBy=paidBy;this.description=description;} }
    static final class InstallmentRecord {final long id,amount,paid;final String title,dueDate;InstallmentRecord(long id,String title,long amount,String dueDate,long paid){this.id=id;this.title=title;this.amount=amount;this.dueDate=dueDate;this.paid=paid;}}
    static final class CollaboratorRecord {final long id;final String first,last,license,body,phone,notes;CollaboratorRecord(long id,String first,String last,String license,String body,String phone,String notes){this.id=id;this.first=first;this.last=last;this.license=license;this.body=body;this.phone=phone;this.notes=notes;}@Override public String toString(){return first+" "+last;}}
    static final class CaseCollaboratorRecord {final long id,collaboratorId;final String name,license,mode,notes;final double share;CaseCollaboratorRecord(long id,long collaboratorId,String name,String license,String mode,double share,String notes){this.id=id;this.collaboratorId=collaboratorId;this.name=name;this.license=license;this.mode=mode;this.share=share;this.notes=notes;}}
    static final class RepresentationContractRecord {final long id,fee,caseId;final String number,date,subject,scope,opponents,collaborationMode,notes;final boolean opponentsAny,delegation;RepresentationContractRecord(long id,String number,String date,String subject,String scope,long fee,boolean opponentsAny,String opponents,boolean delegation,String collaborationMode,String notes,long caseId){this.id=id;this.number=number;this.date=date;this.subject=subject;this.scope=scope;this.fee=fee;this.opponentsAny=opponentsAny;this.opponents=opponents;this.delegation=delegation;this.collaborationMode=collaborationMode;this.notes=notes;this.caseId=caseId;}}
    static final class FinancialContractRecord {final long id,total;final String date,terms,notes;FinancialContractRecord(long id,String date,long total,String terms,String notes){this.id=id;this.date=date;this.total=total;this.terms=terms;this.notes=notes;}}
    static final class PaymentCheckRecord {final long id,caseId,amount;final String number,dueDate,bank,branch,status,notes;PaymentCheckRecord(long id,long caseId,String number,String dueDate,long amount,String bank,String branch,String status,String notes){this.id=id;this.caseId=caseId;this.number=number;this.dueDate=dueDate;this.amount=amount;this.bank=bank;this.branch=branch;this.status=status;this.notes=notes;}}
    static final class CaseAttachmentRecord {final long id,caseId;final String name,mime,uri,createdAt;CaseAttachmentRecord(long id,long caseId,String name,String mime,String uri,String createdAt){this.id=id;this.caseId=caseId;this.name=name;this.mime=mime;this.uri=uri;this.createdAt=createdAt;}}
    static final class LegalDocumentRecord {final long id;final String kind,title,number,subject,body,sourceUrl,version;LegalDocumentRecord(long id,String kind,String title,String number,String subject,String body,String sourceUrl,String version){this.id=id;this.kind=kind;this.title=title;this.number=number;this.subject=subject;this.body=body;this.sourceUrl=sourceUrl;this.version=version;}}
    static final class DeletedRecord {final String type,title,deletedAt;final long id;DeletedRecord(String type,long id,String title,String deletedAt){this.type=type;this.id=id;this.title=title;this.deletedAt=deletedAt;}}
    static final class ReminderRecord {String label="";final long id,at;final int advanceDays;final String kind;ReminderRecord(long id,long at,int advanceDays,String kind){this.id=id;this.at=at;this.advanceDays=advanceDays;this.kind=kind;}}
    static final class AccountSummary { long agreed,received,reimbursed,expenses,lawyerPaid,debt,credit; }
    static final class WorkLogRecord { final long id,caseId,clientId; final String type,date,description,caseTitle,clientName; WorkLogRecord(long id,long caseId,long clientId,String type,String date,String description,String caseTitle,String clientName){this.id=id;this.caseId=caseId;this.clientId=clientId;this.type=type;this.date=date;this.description=description;this.caseTitle=caseTitle;this.clientName=clientName;} }
    static final class AppointmentRecord {String branch="",authority="",city="",attendance="planned",completedAt="";final long id,clientId,caseId;final String kind,person,phone,date,start,end,place,notes,caseTitle;AppointmentRecord(long id,String kind,String person,String phone,String date,String start,String end,String place,String notes,long clientId,long caseId,String caseTitle){this.id=id;this.kind=kind;this.person=person;this.phone=phone;this.date=date;this.start=start;this.end=end;this.place=place;this.notes=notes;this.clientId=clientId;this.caseId=caseId;this.caseTitle=caseTitle;}}
    static final class DeadlineRecord {final long id,caseId;final String title,eventDate,dueDate,notes,caseTitle,caseNumber,clientName;final int days,completed;DeadlineRecord(long id,long caseId,String title,String eventDate,String dueDate,int days,String notes,int completed,String caseTitle,String caseNumber,String clientName){this.id=id;this.caseId=caseId;this.title=title;this.eventDate=eventDate;this.dueDate=dueDate;this.days=days;this.notes=notes;this.completed=completed;this.caseTitle=caseTitle;this.caseNumber=caseNumber;this.clientName=clientName;}}
}
