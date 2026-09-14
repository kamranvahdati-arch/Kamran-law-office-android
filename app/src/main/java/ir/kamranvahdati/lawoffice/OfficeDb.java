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

final class OfficeDb extends SQLiteOpenHelper {
    static final String DATABASE_NAME = "law_office_demo_v7.db";
    private static final int VERSION = 7;

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
            v.put("created_at",now()); v.put("updated_at",now());
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
            v.put("created_at",now()); v.put("updated_at",now());
            caseIds[i]=db.insertOrThrow("cases",null,v);
        }

        for (int i=0;i<100;i++) {
            ContentValues v=new ContentValues(); int day=(i%JalaliDate.monthLength(today.year,today.month))+1;
            v.put("title",i%4==0?"مهلت فوری آزمایشی "+fa(i+1):"برنامه کاری آزمایشی "+fa(i+1));
            v.put("case_name","پرونده آزمایشی شماره "+fa((i%100)+1));
            v.put("due_date",String.format(Locale.US,"%04d/%02d/%02d",today.year,today.month,day));
            v.put("due_time",String.format(Locale.US,"%02d:%02d",8+(i%10),(i%4)*15));
            v.put("priority",i%4==0?"فوری":i%4==1?"عادی":"کم");
            v.put("done",i%10==0?1:0); v.put("status",i%10==0?"done":i%10==1?"deferred":"open");
            v.put("notes","توضیح فرضی برای آزمایش برنامه، یادآوری و تقویم.");
            v.put("created_at",now()); v.put("updated_at",now()); db.insertOrThrow("tasks",null,v);
        }

        for (int i=0;i<100;i++) {
            ContentValues v=new ContentValues(); boolean payment=i%2==0;
            v.put("case_id",caseIds[i]); v.put("kind",payment?"payment":"expense");
            v.put("category",payment?"حق‌الوکاله":expenseCategories[i%expenseCategories.length]);
            v.put("amount",payment?25000000L+(i*1000000L):5000000L+(i*250000L));
            v.put("entry_date",String.format(Locale.US,"%04d/%02d/%02d",today.year,today.month,(i%28)+1));
            v.put("paid_by",payment?"موکل":i%4==1?"وکیل":"موکل");
            v.put("description","عملیات مالی کاملاً فرضی شماره "+fa(i+1));
            v.put("created_at",now()); db.insertOrThrow("ledger",null,v);
        }
        for (int i=0;i<100;i++) {
            ContentValues v=new ContentValues();
            v.put("case_id",caseIds[i]); v.put("client_id",clientIds[i]);
            v.put("action_type",i%4==0?"مطالعه پرونده":i%4==1?"مراجعه به شعبه":i%4==2?"پیگیری پرونده":"ارسال لایحه");
            v.put("action_date",String.format(Locale.US,"%04d/%02d/%02d",today.year,today.month,(i%28)+1));
            v.put("description","گزارش اقدام کاملاً فرضی شماره "+fa(i+1)); v.put("created_at",now());
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
        v.put("notes", notes); v.put("created_at", now()); v.put("updated_at", now());
        return getWritableDatabase().insertOrThrow("clients", null, v);
    }

    long addCase(CaseRecord c) {
        ContentValues v = caseValues(c);
        v.put("created_at", now()); v.put("updated_at", now());
        return getWritableDatabase().insertOrThrow("cases", null, v);
    }

    void updateCaseStatus(long id, String status) {
        ContentValues v = new ContentValues(); v.put("status", status); v.put("updated_at", now());
        getWritableDatabase().update("cases", v, "id=?", new String[]{String.valueOf(id)});
    }

    void addTask(String title, String caseName, String date, String time,
                 String priority, String notes) {
        addTask(getWritableDatabase(), title, caseName, date, time, priority, notes);
    }

    private void addTask(SQLiteDatabase db, String title, String caseName, String date,
                         String time, String priority, String notes) {
        ContentValues v = new ContentValues(); v.put("title", title); v.put("case_name", caseName);
        v.put("due_date", date); v.put("due_time", time); v.put("priority", priority);
        v.put("notes", notes); v.put("status", "open"); v.put("created_at", now());
        v.put("updated_at", now()); db.insertOrThrow("tasks", null, v);
    }

    void setTaskDone(long id, boolean done) {
        ContentValues v = new ContentValues(); v.put("done", done ? 1 : 0);
        v.put("status", done ? "done" : "open"); v.put("updated_at", now());
        getWritableDatabase().update("tasks", v, "id=?", new String[]{String.valueOf(id)});
    }

    void addLedger(long caseId, String kind, String category, long amount,
                   String date, String paidBy, String description) {
        ContentValues v = new ContentValues(); v.put("case_id", caseId); v.put("kind", kind);
        v.put("category", category); v.put("amount", amount); v.put("entry_date", date);
        v.put("paid_by", paidBy); v.put("description", description); v.put("created_at", now());
        getWritableDatabase().insertOrThrow("ledger", null, v);
    }

    void addWorkLog(long caseId,long clientId,String type,String date,String description) {
        ContentValues v=new ContentValues(); v.put("case_id",caseId);
        if(clientId>0)v.put("client_id",clientId); else v.putNull("client_id");
        v.put("action_type",type); v.put("action_date",date); v.put("description",description); v.put("created_at",now());
        getWritableDatabase().insertOrThrow("worklogs",null,v);
    }

    List<WorkLogRecord> workLogs(Long caseId,Long clientId) {
        ArrayList<WorkLogRecord> result=new ArrayList<>(); StringBuilder w=new StringBuilder(" WHERE 1=1"); ArrayList<String> a=new ArrayList<>();
        if(caseId!=null){w.append(" AND w.case_id=?");a.add(String.valueOf(caseId));}
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
                "FROM clients cl LEFT JOIN cases cs ON cs.client_id=cl.id " +
                "GROUP BY cl.id ORDER BY cl.name", null);
        while (c.moveToNext()) result.add(new ClientRecord(c.getLong(0), c.getString(1),
                c.getString(2), c.getString(3), c.getString(4), c.getString(5),
                c.getString(6), c.getString(7), c.getInt(8), c.getInt(9)));
        c.close(); return result;
    }

    List<CaseRecord> cases(String status, String category, Long clientId) {
        ArrayList<CaseRecord> result = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
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
        Cursor c = getReadableDatabase().rawQuery("SELECT id,title,case_name,due_date,due_time," +
                "priority,done,status,notes FROM tasks WHERE due_date=? ORDER BY done,CASE priority " +
                "WHEN 'فوری' THEN 0 ELSE 1 END,due_time", new String[]{date});
        while (c.moveToNext()) result.add(taskFrom(c)); c.close(); return result;
    }

    List<TaskRecord> openTasks(int limit) {
        ArrayList<TaskRecord> result = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT id,title,case_name,due_date,due_time," +
                "priority,done,status,notes FROM tasks WHERE done=0 ORDER BY due_date," +
                "CASE priority WHEN 'فوری' THEN 0 ELSE 1 END LIMIT " + limit, null);
        while (c.moveToNext()) result.add(taskFrom(c)); c.close(); return result;
    }

    int countCases(String status) { return scalar("SELECT COUNT(*) FROM cases WHERE status=?", status); }
    int countClients() { return scalar("SELECT COUNT(*) FROM clients", null); }
    int countToday(String date) { return scalar("SELECT COUNT(*) FROM tasks WHERE due_date=? AND done=0", date); }
    int countAllTasks() { return scalar("SELECT COUNT(*) FROM tasks", null); }
    int taskCountInMonth(String prefix) { return scalar("SELECT COUNT(*) FROM tasks WHERE due_date LIKE ?", prefix + "%"); }

    private int scalar(String sql, String arg) {
        Cursor c = getReadableDatabase().rawQuery(sql, arg == null ? null : new String[]{arg});
        int value = c.moveToFirst() ? c.getInt(0) : 0; c.close(); return value;
    }

    List<LedgerRecord> ledger(long caseId) {
        ArrayList<LedgerRecord> result = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT id,kind,category,amount,entry_date," +
                "paid_by,description FROM ledger WHERE case_id=? ORDER BY id DESC",
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

    String exportJson() throws Exception {
        JSONObject root = new JSONObject(); root.put("format", "KLO-2"); root.put("created", now());
        JSONObject tables = new JSONObject();
        for (String table : new String[]{"clients", "tasks", "cases", "ledger", "worklogs"})
            tables.put(table, dump(table));
        root.put("tables", tables); return root.toString();
    }

    void importJson(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        if (!"KLO-2".equals(root.optString("format"))) throw new Exception("نسخه پشتیبان معتبر نیست");
        JSONObject tables = root.getJSONObject("tables"); SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("worklogs", null, null); db.delete("ledger", null, null); db.delete("cases", null, null);
            db.delete("tasks", null, null); db.delete("clients", null, null);
            for (String table : new String[]{"clients", "tasks", "cases", "ledger", "worklogs"})
                if(tables.has(table))restore(db, table, tables.getJSONArray(table));
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

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
        for (int i = 0; i < rows.length(); i++) { JSONObject row = rows.getJSONObject(i); ContentValues v = new ContentValues();
            Iterator<String> keys = row.keys(); while (keys.hasNext()) { String key = keys.next(); Object value = row.get(key);
                if (value == JSONObject.NULL) v.putNull(key); else if (value instanceof Number) v.put(key, ((Number) value).longValue()); else v.put(key, String.valueOf(value));
            } db.insertOrThrow(table, null, v);
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
    private static String now() { return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()); }

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
    static final class AccountSummary { long agreed,received,reimbursed,expenses,lawyerPaid,debt,credit; }
    static final class WorkLogRecord { final long id,caseId,clientId; final String type,date,description,caseTitle,clientName; WorkLogRecord(long id,long caseId,long clientId,String type,String date,String description,String caseTitle,String clientName){this.id=id;this.caseId=caseId;this.clientId=clientId;this.type=type;this.date=date;this.description=description;this.caseTitle=caseTitle;this.clientName=clientName;} }
}
