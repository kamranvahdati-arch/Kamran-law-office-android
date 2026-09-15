package ir.kamranvahdati.lawoffice;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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
    static final String DATABASE_NAME = "law_office_demo_v7.db";
    private static final int VERSION = 9;

    OfficeDb(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
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
            identity(v);
            caseIds[i]=db.insertOrThrow("cases",null,v);
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
            identity(v); db.insertOrThrow("ledger",null,v);
        }
        for (int i=0;i<100;i++) {
            ContentValues v=new ContentValues();
            v.put("case_id",caseIds[i]); v.put("client_id",clientIds[i]);
            v.put("action_type",i%4==0?"مطالعه پرونده":i%4==1?"مراجعه به شعبه":i%4==2?"پیگیری پرونده":"ارسال لایحه");
            v.put("action_date",String.format(Locale.US,"%04d/%02d/%02d",today.year,today.month,(i%28)+1));
            v.put("description","گزارش اقدام کاملاً فرضی شماره "+fa(i+1)); identity(v);
            db.insertOrThrow("worklogs",null,v);
        }
        verifyDemoData(db);
    }

    private void verifyDemoData(SQLiteDatabase db) {
        for (String table:new String[]{"clients","cases","tasks","ledger","worklogs"})
            if (DatabaseUtils.queryNumEntries(db,table)!=100)
                throw new IllegalStateException("آزمون تعداد رکوردهای "+table+" ناموفق بود");
        Cursor fk=db.rawQuery("PRAGMA foreign_key_check",null);
        boolean invalid=fk.moveToFirst(); fk.close();
        if(invalid) throw new IllegalStateException("آزمون ارتباط داده‌ها ناموفق بود");
    }

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
    void deleteClient(long id){if(scalar("SELECT COUNT(*) FROM cases WHERE client_id=? AND deleted_at IS NULL",String.valueOf(id))>0)throw new IllegalArgumentException("ابتدا پرونده‌های مرتبط را به موکل دیگری منتقل کنید");softDelete("clients",id);}

    long addCase(CaseRecord c) {
        ContentValues v = caseValues(c);
        identity(v);
        return getWritableDatabase().insertOrThrow("cases", null, v);
    }
    void updateCase(CaseRecord c){ContentValues v=caseValues(c);v.put("updated_at",now());if(getWritableDatabase().update("cases",v,"id=? AND deleted_at IS NULL",new String[]{String.valueOf(c.id)})!=1)throw new IllegalArgumentException("پرونده یافت نشد");}
    void deleteCase(long id){softDelete("cases",id);}
    private void softDelete(String table,long id){ContentValues v=new ContentValues();v.put("deleted_at",now());v.put("updated_at",now());getWritableDatabase().update(table,v,"id=? AND deleted_at IS NULL",new String[]{String.valueOf(id)});}

    void updateCaseStatus(long id, String status) {
        ContentValues v = new ContentValues(); v.put("status", status); v.put("updated_at", now());
        getWritableDatabase().update("cases", v, "id=? AND deleted_at IS NULL", new String[]{String.valueOf(id)});
    }

    void addTask(String title, String caseName, String date, String time,
                 String priority, String notes) {
        addTask(getWritableDatabase(), title, caseName, date, time, priority, notes);
    }

    void addLinkedTask(String title,String kind,Long caseId,Long clientId,String date,String start,String end,String place,String priority,String notes) {
        ContentValues v=new ContentValues();v.put("title",title);v.put("kind",kind);
        if(caseId!=null)v.put("case_id",caseId);if(clientId!=null)v.put("client_id",clientId);
        v.put("due_date",date);v.put("due_time",start);v.put("end_time",end);v.put("place",place);
        v.put("priority",priority);v.put("notes",notes);v.put("status","open");identity(v);
        getWritableDatabase().insertOrThrow("tasks",null,v);
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
        if(clientId!=null){w.append(" AND w.client_id=?");a.add(String.valueOf(clientId));}
        Cursor c=getReadableDatabase().rawQuery("SELECT w.id,w.case_id,w.client_id,w.action_type,w.action_date,w.description,cs.title,cl.name FROM worklogs w LEFT JOIN cases cs ON cs.id=w.case_id LEFT JOIN clients cl ON cl.id=w.client_id"+w+" ORDER BY w.action_date DESC,w.id DESC",a.toArray(new String[0]));
        while(c.moveToNext())result.add(new WorkLogRecord(c.getLong(0),c.getLong(1),c.getLong(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6),c.getString(7)));
        c.close();return result;
    }

    List<ClientRecord> clients() {
        ArrayList<ClientRecord> result = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT cl.id,cl.name,cl.national_id," +
                "cl.father_name,cl.birth_date,cl.phone,cl.address,cl.notes," +
                "COUNT(cs.id),SUM(CASE WHEN cs.status='active' THEN 1 ELSE 0 END) " +
                "FROM clients cl LEFT JOIN cases cs ON cs.client_id=cl.id AND cs.deleted_at IS NULL WHERE cl.deleted_at IS NULL " +
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
        if (clientId != null) { where.append(" AND c.client_id=?"); args.add(String.valueOf(clientId)); }
        Cursor c = getReadableDatabase().rawQuery("SELECT c.id,c.title,c.reference,c.stage," +
                "COALESCE(c.client_id,0),cl.name,c.fee_agreed,c.agreement_notes,c.status," +
                "c.category,c.case_number,c.archive_number,c.branch,c.subject,c.claim_text," +
                "c.evidence,c.summary,c.financial_notes,c.contract_notes,c.authority_type,c.province,c.judicial_city FROM cases c " +
                "LEFT JOIN clients cl ON cl.id=c.client_id" + where + " ORDER BY c.id DESC",
                args.toArray(new String[0]));
        while (c.moveToNext()) result.add(caseFrom(c));
        c.close(); return result;
    }

    List<TaskRecord> tasksForDate(String date) {
        ArrayList<TaskRecord> result = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT t.id,t.title,COALESCE(cs.title,cl.name,t.case_name),t.due_date,t.due_time," +
                "t.priority,t.done,t.status,t.notes FROM tasks t LEFT JOIN cases cs ON cs.id=t.case_id LEFT JOIN clients cl ON cl.id=t.client_id WHERE t.deleted_at IS NULL AND t.due_date=? ORDER BY t.done,CASE t.priority " +
                "WHEN 'فوری' THEN 0 ELSE 1 END,due_time", new String[]{date});
        while (c.moveToNext()) result.add(taskFrom(c)); c.close(); return result;
    }

    List<TaskRecord> openTasks(int limit) {
        ArrayList<TaskRecord> result = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT t.id,t.title,COALESCE(cs.title,cl.name,t.case_name),t.due_date,t.due_time," +
                "t.priority,t.done,t.status,t.notes FROM tasks t LEFT JOIN cases cs ON cs.id=t.case_id LEFT JOIN clients cl ON cl.id=t.client_id WHERE t.deleted_at IS NULL AND t.done=0 ORDER BY t.due_date," +
                "CASE priority WHEN 'فوری' THEN 0 ELSE 1 END LIMIT " + limit, null);
        while (c.moveToNext()) result.add(taskFrom(c)); c.close(); return result;
    }

    int countCases(String status) { return scalar("SELECT COUNT(*) FROM cases WHERE deleted_at IS NULL AND status=?", status); }
    int countClients() { return scalar("SELECT COUNT(*) FROM clients WHERE deleted_at IS NULL", null); }
    int countToday(String date) { return scalar("SELECT COUNT(*) FROM tasks WHERE deleted_at IS NULL AND due_date=? AND done=0", date); }
    int countAllTasks() { return scalar("SELECT COUNT(*) FROM tasks WHERE deleted_at IS NULL", null); }
    int countAppointments(String date) { return scalar("SELECT COUNT(*) FROM appointments WHERE deleted_at IS NULL AND visit_date=?", date); }
    int countDeadlines(String date) { return scalar("SELECT COUNT(*) FROM deadlines WHERE deleted_at IS NULL AND completed=0 AND due_date<=?", date); }
    int taskCountInMonth(String prefix) { return scalar("SELECT COUNT(*) FROM tasks WHERE deleted_at IS NULL AND due_date LIKE ?", prefix + "%"); }
    int countOverdueTasks(String today){return scalar("SELECT COUNT(*) FROM tasks WHERE deleted_at IS NULL AND done=0 AND due_date<?",today);}
    int countNeedsAction(String today){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(DISTINCT c.id) FROM cases c LEFT JOIN deadlines d ON d.case_id=c.id AND d.deleted_at IS NULL AND d.completed=0 AND d.due_date<=? LEFT JOIN tasks t ON t.case_id=c.id AND t.deleted_at IS NULL AND t.done=0 AND t.due_date<=? WHERE c.status='active' AND c.deleted_at IS NULL AND (d.id IS NOT NULL OR t.id IS NOT NULL)",new String[]{today,today})){return c.moveToFirst()?c.getInt(0):0;}}

    private int scalar(String sql, String arg) {
        Cursor c = getReadableDatabase().rawQuery(sql, arg == null ? null : new String[]{arg});
        int value = c.moveToFirst() ? c.getInt(0) : 0; c.close(); return value;
    }

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

    long addInstallment(long caseId,String title,long amount,String dueDate){if(amount<=0)throw new IllegalArgumentException("مبلغ قسط باید مثبت باشد");ContentValues v=new ContentValues();v.put("case_id",caseId);v.put("title",title);v.put("amount",amount);v.put("due_date",dueDate);identity(v);return getWritableDatabase().insertOrThrow("installments",null,v);}
    List<InstallmentRecord> installments(long caseId){ArrayList<InstallmentRecord> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT i.id,i.title,i.amount,i.due_date,COALESCE(SUM(CASE WHEN ip.deleted_at IS NULL AND l.deleted_at IS NULL THEN ip.amount ELSE 0 END),0) paid FROM installments i LEFT JOIN installment_payments ip ON ip.installment_id=i.id LEFT JOIN ledger l ON l.id=ip.ledger_id WHERE i.case_id=? AND i.deleted_at IS NULL GROUP BY i.id ORDER BY i.due_date,i.id",new String[]{String.valueOf(caseId)})){while(c.moveToNext())out.add(new InstallmentRecord(c.getLong(0),c.getString(1),c.getLong(2),c.getString(3),c.getLong(4)));}return out;}
    void payInstallment(long id,long amount,String date,String payer,String description){if(amount<=0)throw new IllegalArgumentException("مبلغ پرداخت باید مثبت باشد");SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{long caseId,total,paid;try(Cursor c=db.rawQuery("SELECT i.case_id,i.amount,COALESCE(SUM(CASE WHEN ip.deleted_at IS NULL AND l.deleted_at IS NULL THEN ip.amount ELSE 0 END),0) FROM installments i LEFT JOIN installment_payments ip ON ip.installment_id=i.id LEFT JOIN ledger l ON l.id=ip.ledger_id WHERE i.id=? AND i.deleted_at IS NULL GROUP BY i.id",new String[]{String.valueOf(id)})){if(!c.moveToFirst())throw new IllegalArgumentException("قسط یافت نشد");caseId=c.getLong(0);total=c.getLong(1);paid=c.getLong(2);}if(amount>total-paid)throw new IllegalArgumentException("پرداخت بیش از مانده قسط است");ContentValues l=new ContentValues();l.put("case_id",caseId);l.put("kind","payment");l.put("category","حق‌الوکاله");l.put("amount",amount);l.put("entry_date",date);l.put("paid_by",payer);l.put("description",description);identity(l);long ledgerId=db.insertOrThrow("ledger",null,l);ContentValues p=new ContentValues();p.put("installment_id",id);p.put("ledger_id",ledgerId);p.put("amount",amount);identity(p);db.insertOrThrow("installment_payments",null,p);db.setTransactionSuccessful();}finally{db.endTransaction();}}

    long addAppointment(String kind,String person,String phone,Long clientId,Long caseId,String date,String start,String end,String place,String notes) {
        ContentValues v=new ContentValues();v.put("kind",kind);v.put("person_name",person);v.put("contact_phone",phone);
        if(clientId!=null)v.put("client_id",clientId);if(caseId!=null)v.put("case_id",caseId);
        v.put("visit_date",date);v.put("start_time",start);v.put("end_time",end);v.put("place",place);v.put("notes",notes);identity(v);
        return getWritableDatabase().insertOrThrow("appointments",null,v);
    }

    List<AppointmentRecord> appointments(String date){ArrayList<AppointmentRecord> list=new ArrayList<>();
        String sql="SELECT a.id,a.kind,a.person_name,a.contact_phone,a.visit_date,a.start_time,a.end_time,a.place,a.notes,COALESCE(a.client_id,0),COALESCE(a.case_id,0),c.title FROM appointments a LEFT JOIN cases c ON c.id=a.case_id WHERE a.deleted_at IS NULL"+(date==null?"":" AND a.visit_date=?")+" ORDER BY a.visit_date,a.start_time,a.id";
        Cursor c=getReadableDatabase().rawQuery(sql,date==null?null:new String[]{date});
        while(c.moveToNext())list.add(new AppointmentRecord(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6),c.getString(7),c.getString(8),c.getLong(9),c.getLong(10),c.getString(11)));c.close();return list;
    }

    long addDeadline(long caseId,String title,String eventDate,String dueDate,int duration,String notes){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{ContentValues v=new ContentValues();v.put("case_id",caseId);v.put("title",title);v.put("event_date",eventDate);v.put("due_date",dueDate);v.put("duration_days",duration);v.put("notes",notes);identity(v);long id=db.insertOrThrow("deadlines",null,v);for(int days:new int[]{7,3,1,0}){java.util.Calendar at=JalaliDate.calendar(dueDate);at.set(java.util.Calendar.HOUR_OF_DAY,9);at.set(java.util.Calendar.MINUTE,0);at.add(java.util.Calendar.DAY_OF_YEAR,-days);ContentValues r=new ContentValues();r.put("target_type","deadline");r.put("target_id",id);r.put("advance_days",days);r.put("trigger_at",at.getTimeInMillis());identity(r);db.insertOrThrow("reminders",null,r);}db.setTransactionSuccessful();return id;}finally{db.endTransaction();}}
    void completeDeadline(long id){ContentValues v=new ContentValues();v.put("completed",1);v.put("updated_at",now());getWritableDatabase().update("deadlines",v,"id=?",new String[]{String.valueOf(id)});}
    List<ReminderRecord> pendingReminders(){ArrayList<ReminderRecord> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT r.id,r.trigger_at,r.advance_days FROM reminders r JOIN deadlines d ON d.id=r.target_id JOIN cases cs ON cs.id=d.case_id WHERE r.target_type='deadline' AND r.deleted_at IS NULL AND r.fired_at IS NULL AND d.completed=0 AND d.deleted_at IS NULL AND cs.deleted_at IS NULL AND r.trigger_at>? ORDER BY r.trigger_at LIMIT 1000",new String[]{String.valueOf(System.currentTimeMillis())})){while(c.moveToNext())out.add(new ReminderRecord(c.getLong(0),c.getLong(1),c.getInt(2)));}return out;}
    boolean fireReminder(long id){ContentValues v=new ContentValues();v.put("fired_at",now());v.put("updated_at",now());SQLiteDatabase db=getWritableDatabase();return db.update("reminders",v,"id=? AND fired_at IS NULL AND deleted_at IS NULL AND EXISTS(SELECT 1 FROM deadlines d JOIN cases c ON c.id=d.case_id WHERE d.id=reminders.target_id AND d.completed=0 AND d.deleted_at IS NULL AND c.deleted_at IS NULL)",new String[]{String.valueOf(id)})==1;}
    List<DeadlineRecord> deadlines(Long caseId,boolean openOnly){ArrayList<DeadlineRecord> list=new ArrayList<>();ArrayList<String> args=new ArrayList<>();StringBuilder w=new StringBuilder(" WHERE d.deleted_at IS NULL AND c.deleted_at IS NULL");if(caseId!=null){w.append(" AND d.case_id=?");args.add(String.valueOf(caseId));}if(openOnly)w.append(" AND d.completed=0");Cursor c=getReadableDatabase().rawQuery("SELECT d.id,d.case_id,d.title,d.event_date,d.due_date,d.duration_days,d.notes,d.completed,c.title,c.case_number,cl.name FROM deadlines d JOIN cases c ON c.id=d.case_id LEFT JOIN clients cl ON cl.id=c.client_id"+w+" ORDER BY d.due_date,d.id",args.toArray(new String[0]));while(c.moveToNext())list.add(new DeadlineRecord(c.getLong(0),c.getLong(1),c.getString(2),c.getString(3),c.getString(4),c.getInt(5),c.getString(6),c.getInt(7),c.getString(8),c.getString(9),c.getString(10)));c.close();return list;}

    String clientFinancialText(long clientId){StringBuilder b=new StringBuilder();for(CaseRecord c:cases(null,"همه",clientId))b.append(caseFinancialText(c));return b.toString();}
    String caseFinancialText(CaseRecord c){StringBuilder b=new StringBuilder();AccountSummary s=summary(c);b.append(c.title).append(" / ").append(c.caseNumber).append("\nتوافق حق‌الوکاله: ").append(s.agreed).append(" ریال؛ دریافتی: ").append(s.received).append(" ریال\nهزینه‌ها: ").append(s.expenses).append(" ریال؛ هزینه پرداختی وکیل: ").append(s.lawyerPaid).append(" ریال؛ بدهی: ").append(s.debt).append(" ریال\nقرارداد/توافق: ").append(c.contractNotes).append(" / ").append(c.agreementNotes).append("\nاقساط:\n");for(InstallmentRecord i:installments(c.id))b.append(i.dueDate).append(" | ").append(i.title).append(" | تعهد: ").append(i.amount).append(" ریال | پرداخت: ").append(i.paid).append(" ریال | مانده: ").append(i.amount-i.paid).append(" ریال\n");
        for(LedgerRecord l:ledger(c.id))b.append(l.date).append(" | ").append(l.category).append(" | ").append(l.amount).append(" ریال | پرداخت‌کننده: ").append(l.paidBy).append(" | ").append(l.description).append("\n");b.append("\n");return b.toString();}

    String performanceText(Long caseId,Long clientId){StringBuilder b=new StringBuilder();ArrayList<Long> ids=new ArrayList<>();for(CaseRecord c:cases(null,"همه",clientId))if(caseId==null||c.id==caseId){ids.add(c.id);b.append("پرونده: ").append(c.title).append(" | شماره: ").append(c.caseNumber).append(" | موکل: ").append(c.clientName).append("\n");for(WorkLogRecord x:workLogs(c.id,null))b.append(x.date).append(" | اقدام: ").append(x.type).append(" | ").append(x.description).append("\n");for(LedgerRecord l:ledger(c.id))if("expense".equals(l.kind)&&"وکیل".equals(l.paidBy))b.append(l.date).append(" | هزینه پرداختی وکیل: ").append(l.category).append(" | ").append(l.amount).append(" ریال | ").append(l.description).append("\n");b.append("\n");}return b.toString();}

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
            db.delete("reminders",null,null);db.delete("installment_payments",null,null);db.delete("installments",null,null);
            db.delete("worklogs", null, null); db.delete("deadlines",null,null);db.delete("appointments",null,null); db.delete("ledger", null, null); db.delete("cases", null, null);
            db.delete("tasks", null, null); db.delete("clients", null, null);
            for (String table : backupTables())
                if(tables.has(table))restore(db, table, tables.getJSONArray(table));
            try(Cursor check=db.rawQuery("PRAGMA foreign_key_check",null)){if(check.moveToFirst())throw new Exception("ارتباط داده‌های پشتیبان نامعتبر است");}
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    private static String[] backupTables(){return new String[]{"clients","cases","tasks","ledger","worklogs","appointments","deadlines","installments","installment_payments","reminders"};}

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
            } if(!v.containsKey("uid")||v.getAsString("uid")==null)v.put("uid",UUID.randomUUID().toString());if(!v.containsKey("updated_at"))v.put("updated_at",now());db.insertOrThrow(table, null, v);
        }
    }

    private ContentValues caseValues(CaseRecord c) {
        ContentValues v = new ContentValues(); v.put("title", c.title); v.put("reference", c.reference);
        v.put("stage", c.stage); if (c.clientId > 0) v.put("client_id", c.clientId); else v.putNull("client_id");
        v.put("fee_agreed", c.feeAgreed); v.put("agreement_notes", c.agreementNotes);
        v.put("status", c.status); v.put("category", c.category); v.put("case_number", c.caseNumber);
        v.put("archive_number", c.archiveNumber); v.put("branch", c.branch); v.put("subject", c.subject);
        v.put("claim_text", c.claimText); v.put("evidence", c.evidence); v.put("summary", c.summary);
        v.put("financial_notes", c.financialNotes); v.put("contract_notes", c.contractNotes);
        v.put("authority_type",c.authorityType); v.put("province",c.province); v.put("judicial_city",c.judicialCity); return v;
    }

    private CaseRecord caseFrom(Cursor c) { return new CaseRecord(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4), c.getString(5), c.getLong(6), c.getString(7), c.getString(8), c.getString(9), c.getString(10), c.getString(11), c.getString(12), c.getString(13), c.getString(14), c.getString(15), c.getString(16), c.getString(17), c.getString(18),c.getString(19),c.getString(20),c.getString(21)); }
    private TaskRecord taskFrom(Cursor c) { return new TaskRecord(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getInt(6), c.getString(7), c.getString(8)); }
    private static String now() { SimpleDateFormat utc=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",Locale.US);utc.setTimeZone(TimeZone.getTimeZone("UTC"));return utc.format(new Date()); }

    static final class ClientRecord {
        final long id; final String name,nationalId,father,birth,phone,address,notes; final int caseCount,activeCount;
        ClientRecord(long id,String name,String nationalId,String father,String birth,String phone,String address,String notes,int caseCount,int activeCount){this.id=id;this.name=name;this.nationalId=nationalId;this.father=father;this.birth=birth;this.phone=phone;this.address=address;this.notes=notes;this.caseCount=caseCount;this.activeCount=activeCount;}
        @Override public String toString(){return name;}
    }
    static final class CaseRecord {
        long id,clientId,feeAgreed; String title,reference,stage,clientName,agreementNotes,status,category,caseNumber,archiveNumber,branch,subject,claimText,evidence,summary,financialNotes,contractNotes,authorityType,province,judicialCity;
        CaseRecord(){status="active";category="حقوقی";}
        CaseRecord(long id,String title,String reference,String stage,long clientId,String clientName,long fee,String agreement,String status,String category,String caseNumber,String archiveNumber,String branch,String subject,String claimText,String evidence,String summary,String financialNotes,String contractNotes,String authorityType,String province,String judicialCity){this.id=id;this.title=title;this.reference=reference;this.stage=stage;this.clientId=clientId;this.clientName=clientName;this.feeAgreed=fee;this.agreementNotes=agreement;this.status=status;this.category=category;this.caseNumber=caseNumber;this.archiveNumber=archiveNumber;this.branch=branch;this.subject=subject;this.claimText=claimText;this.evidence=evidence;this.summary=summary;this.financialNotes=financialNotes;this.contractNotes=contractNotes;this.authorityType=authorityType;this.province=province;this.judicialCity=judicialCity;}
    }
    static final class TaskRecord { final long id; final String title,caseName,date,time,priority,status,notes; final int done; TaskRecord(long id,String title,String caseName,String date,String time,String priority,int done,String status,String notes){this.id=id;this.title=title;this.caseName=caseName;this.date=date;this.time=time;this.priority=priority;this.done=done;this.status=status;this.notes=notes;} }
    static final class LedgerRecord { final long id,amount; final String kind,category,date,paidBy,description; LedgerRecord(long id,String kind,String category,long amount,String date,String paidBy,String description){this.id=id;this.kind=kind;this.category=category;this.amount=amount;this.date=date;this.paidBy=paidBy;this.description=description;} }
    static final class InstallmentRecord {final long id,amount,paid;final String title,dueDate;InstallmentRecord(long id,String title,long amount,String dueDate,long paid){this.id=id;this.title=title;this.amount=amount;this.dueDate=dueDate;this.paid=paid;}}
    static final class ReminderRecord {final long id,at;final int advanceDays;ReminderRecord(long id,long at,int advanceDays){this.id=id;this.at=at;this.advanceDays=advanceDays;}}
    static final class AccountSummary { long agreed,received,reimbursed,expenses,lawyerPaid,debt,credit; }
    static final class WorkLogRecord { final long id,caseId,clientId; final String type,date,description,caseTitle,clientName; WorkLogRecord(long id,long caseId,long clientId,String type,String date,String description,String caseTitle,String clientName){this.id=id;this.caseId=caseId;this.clientId=clientId;this.type=type;this.date=date;this.description=description;this.caseTitle=caseTitle;this.clientName=clientName;} }
    static final class AppointmentRecord {final long id,clientId,caseId;final String kind,person,phone,date,start,end,place,notes,caseTitle;AppointmentRecord(long id,String kind,String person,String phone,String date,String start,String end,String place,String notes,long clientId,long caseId,String caseTitle){this.id=id;this.kind=kind;this.person=person;this.phone=phone;this.date=date;this.start=start;this.end=end;this.place=place;this.notes=notes;this.clientId=clientId;this.caseId=caseId;this.caseTitle=caseTitle;}}
    static final class DeadlineRecord {final long id,caseId;final String title,eventDate,dueDate,notes,caseTitle,caseNumber,clientName;final int days,completed;DeadlineRecord(long id,long caseId,String title,String eventDate,String dueDate,int days,String notes,int completed,String caseTitle,String caseNumber,String clientName){this.id=id;this.caseId=caseId;this.title=title;this.eventDate=eventDate;this.dueDate=dueDate;this.days=days;this.notes=notes;this.completed=completed;this.caseTitle=caseTitle;this.caseNumber=caseNumber;this.clientName=clientName;}}
}
