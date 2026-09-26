package ir.kamranvahdati.lawoffice;

import android.content.ContentValues;
import android.database.Cursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import java.util.*;

/** V10.1 additive domain extension. Existing client IDs are the canonical person IDs. */
final class OfficeV101 {
    static final String[] ROLES={"موکل","خواهان","خوانده","شاکی","متهم","مشتکی‌عنه","محکوم‌له","محکوم‌علیه","طرف مقابل","نماینده شخص حقوقی","سایر"};
    static final String[] MODES={"منفرداً","مجتمعاً","انفرادی","مشترک / مجتمع","انفرادی با ارجاع","همکاری موردی","سایر"};
    static final String[] EXPENSES={"هزینه دادرسی","کارشناسی","تمبر","اجرای احکام","ثبت","سفر","بلیت","اقامت","پیک/ارسال","اداری","سایر"};
    static final String[] WORK={"پیگیری پرونده","نیابت قضایی","اجرای حکم","جلب","مراجعه به دادگاه/دادسرا","مراجعه به اداره ثبت","پزشکی قانونی","امور اداری","اخذ/تحویل مدرک","مطالعه پرونده","حضور در جلسه","سایر"};
    private static final String META="id INTEGER PRIMARY KEY AUTOINCREMENT,uid TEXT UNIQUE NOT NULL,created_at TEXT NOT NULL,updated_at TEXT NOT NULL,deleted_at TEXT";
    final OfficeDb office;
    OfficeV101(OfficeDb office){this.office=office;}
    SQLiteDatabase db(){return office.getWritableDatabase();}
    static void migrate(SQLiteDatabase db){
        for(String col:new String[]{"person_type TEXT NOT NULL DEFAULT 'natural'","first_name TEXT","last_name TEXT","legal_type TEXT","registration_number TEXT","registration_date TEXT","telephone TEXT","province TEXT","city TEXT","postal_code TEXT","representative TEXT"})db.execSQL("ALTER TABLE clients ADD COLUMN "+col);
        // Never split or overwrite a legacy full name by guessing its components.
        for(String col:new String[]{"rank TEXT","province TEXT","city TEXT","address TEXT"})db.execSQL("ALTER TABLE collaborators ADD COLUMN "+col);
        db.execSQL("ALTER TABLE appointments ADD COLUMN province TEXT");
        db.execSQL("CREATE TABLE person_roles("+META+",case_id INTEGER NOT NULL REFERENCES cases(id),person_id INTEGER NOT NULL REFERENCES clients(id),role TEXT NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX person_role_live ON person_roles(case_id,person_id,role) WHERE deleted_at IS NULL");
        db.execSQL("CREATE TABLE contract_parties("+META+",contract_id INTEGER NOT NULL REFERENCES representation_contracts(id),person_id INTEGER NOT NULL REFERENCES clients(id),role TEXT NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX contract_party_live ON contract_parties(contract_id,person_id,role) WHERE deleted_at IS NULL");
        db.execSQL("CREATE TABLE financial_accounts("+META+",name TEXT NOT NULL,kind TEXT NOT NULL,bank TEXT,account_number TEXT,card_number TEXT)");
        for(String col:new String[]{"person_id INTEGER REFERENCES clients(id)","account_id INTEGER REFERENCES financial_accounts(id)","contract_id INTEGER REFERENCES representation_contracts(id)","collaborator_id INTEGER REFERENCES collaborators(id)","payment_method TEXT","scope TEXT NOT NULL DEFAULT 'office'"})db.execSQL("ALTER TABLE ledger ADD COLUMN "+col);
        // Only independent transactions need a new table: existing case accounting remains in ledger.
        db.execSQL("CREATE TABLE independent_ledger("+META+",kind TEXT NOT NULL CHECK(kind IN('payment','expense')),category TEXT NOT NULL,amount INTEGER NOT NULL CHECK(amount>0),entry_date TEXT NOT NULL,paid_by TEXT,description TEXT,person_id INTEGER REFERENCES clients(id),account_id INTEGER REFERENCES financial_accounts(id),contract_id INTEGER REFERENCES representation_contracts(id),collaborator_id INTEGER REFERENCES collaborators(id),payment_method TEXT,scope TEXT NOT NULL DEFAULT 'personal')");
        for(String col:new String[]{"person_id INTEGER REFERENCES clients(id)","account_id INTEGER REFERENCES financial_accounts(id)"})db.execSQL("ALTER TABLE payment_checks ADD COLUMN "+col);
        db.execSQL("CREATE TABLE collaboration_requests("+META+",kind TEXT NOT NULL CHECK(kind IN('offer','request')),requester_name TEXT,requester_province TEXT,requester_city TEXT,province TEXT NOT NULL,city TEXT NOT NULL,title TEXT NOT NULL,work_type TEXT,specialties TEXT,description TEXT,conditions TEXT,availability TEXT,due_date TEXT,proposed_amount INTEGER NOT NULL DEFAULT 0 CHECK(proposed_amount>=0),sync_status TEXT NOT NULL DEFAULT 'local',remote_id TEXT)");
        db.execSQL("CREATE INDEX ledger_person_date ON ledger(person_id,entry_date)");
        db.execSQL("CREATE INDEX independent_person_date ON independent_ledger(person_id,entry_date)");
    }
    String appointmentProvince(long id){List<ContentValues> r=rows("SELECT province FROM appointments WHERE id=?",Long.toString(id));return r.isEmpty()?"":text(r.get(0),"province");}
    void setAppointmentProvince(long id,String province){ContentValues v=new ContentValues();v.put("province",province);db().update("appointments",v,"id=?",new String[]{Long.toString(id)});}
    void colleagueDetails(long id,String province,String city,String address,String rank){ContentValues v=new ContentValues();v.put("province",province);v.put("city",city);v.put("address",address);v.put("rank",rank);db().update("collaborators",v,"id=?",new String[]{Long.toString(id)});}
    static ContentValues identity(){ContentValues v=new ContentValues();v.put("uid",UUID.randomUUID().toString());String now=Long.toString(System.currentTimeMillis());v.put("created_at",now);v.put("updated_at",now);return v;}
    static String text(ContentValues v,String key){String x=v.getAsString(key);return x==null?"":x.trim();}
    static void required(String value,String label){if(value==null||value.trim().isEmpty())throw new IllegalArgumentException(label+" الزامی است");}
    static void location(String province,String city){if(province.isEmpty()&&city.isEmpty())return;if(!Arrays.asList(IranLocations.cities(province)).contains(city))throw new IllegalArgumentException("استان و شهر را از فهرست انتخاب کنید");}
    static String date(String value){return value.isEmpty()?"":JalaliDate.parse(value).value();}
    List<ContentValues> rows(String sql,String... args){ArrayList<ContentValues> out=new ArrayList<>();try(Cursor c=db().rawQuery(sql,args)){while(c.moveToNext()){ContentValues v=new ContentValues();for(int i=0;i<c.getColumnCount();i++){if(c.isNull(i))v.putNull(c.getColumnName(i));else if(c.getType(i)==Cursor.FIELD_TYPE_INTEGER)v.put(c.getColumnName(i),c.getLong(i));else v.put(c.getColumnName(i),c.getString(i));}out.add(v);}}return out;}
    ContentValues person(long id){List<ContentValues> r=rows("SELECT * FROM clients WHERE id=?",Long.toString(id));if(r.isEmpty())throw new IllegalArgumentException("شخص یافت نشد");return r.get(0);}
    List<ContentValues> persons(){return rows("SELECT * FROM clients WHERE deleted_at IS NULL ORDER BY name");}
    long savePerson(Long id,ContentValues input){
        ContentValues v=new ContentValues(input);String type=text(v,"person_type");if(!type.equals("natural")&&!type.equals("legal"))throw new IllegalArgumentException("نوع شخص معتبر نیست");
        required(text(v,"name"),"نام");if(id==null||!text(person(id),"province").equals(text(v,"province"))||!text(person(id),"city").equals(text(v,"city")))location(text(v,"province"),text(v,"city"));
        String nid=InputValidators.normalizeDigits(text(v,"national_id")).replace(" ","").replace("-","");
        if(!nid.isEmpty()&&(type.equals("natural")?!InputValidators.isValidIranianNationalId(nid):!nid.matches("[0-9]{11}")))throw new IllegalArgumentException("کد/شناسه ملی معتبر نیست");
        if(!nid.isEmpty()&&!rows("SELECT id FROM clients WHERE person_type=? AND national_id=? AND deleted_at IS NULL AND id<>?",type,nid,id==null?"0":id.toString()).isEmpty())throw new IllegalArgumentException("این شخص قبلاً ثبت شده؛ از فهرست موجود انتخاب کنید");
        String mobile=InputValidators.normalizeIranianMobile(text(v,"phone"));if(!mobile.isEmpty()&&!InputValidators.isValidIranianMobile(mobile))throw new IllegalArgumentException("شماره همراه معتبر نیست");
        String postal=InputValidators.normalizeDigits(text(v,"postal_code"));if(!postal.isEmpty()&&!postal.matches("[0-9]{10}"))throw new IllegalArgumentException("کدپستی باید ۱۰ رقم باشد");
        v.put("national_id",nid);v.put("phone",mobile);v.put("postal_code",postal);v.put("birth_date",date(text(v,"birth_date")));v.put("registration_date",date(text(v,"registration_date")));
        if(id==null){v.putAll(identity());return db().insertOrThrow("clients",null,v);}v.put("updated_at",Long.toString(System.currentTimeMillis()));if(db().update("clients",v,"id=? AND deleted_at IS NULL",new String[]{id.toString()})!=1)throw new IllegalArgumentException("شخص یافت نشد");return id;
    }
    long saveColleague(ContentValues input){ContentValues v=new ContentValues(input);required(text(v,"first_name"),"نام");required(text(v,"last_name"),"نام خانوادگی");location(text(v,"province"),text(v,"city"));String phone=InputValidators.normalizeIranianMobile(text(v,"phone"));if(!phone.isEmpty()&&!InputValidators.isValidIranianMobile(phone))throw new IllegalArgumentException("شماره همراه معتبر نیست");v.put("phone",phone);if(!text(v,"license_number").isEmpty()&&!rows("SELECT id FROM collaborators WHERE license_number=? AND professional_body=? AND deleted_at IS NULL",text(v,"license_number"),text(v,"professional_body")).isEmpty())throw new IllegalArgumentException("وکیل قبلاً ثبت شده؛ از فهرست انتخاب کنید");v.putAll(identity());return db().insertOrThrow("collaborators",null,v);}
    void role(long caseId,long personId,String role){if(!Arrays.asList(ROLES).contains(role))throw new IllegalArgumentException("نقش معتبر نیست");SQLiteDatabase d=db();d.beginTransaction();try{live("cases",caseId);live("clients",personId);if("موکل".equals(role))office.addClientToCase(caseId,personId,role,false);else if(rows("SELECT id FROM person_roles WHERE case_id=? AND person_id=? AND role=? AND deleted_at IS NULL",""+caseId,""+personId,role).isEmpty()){ContentValues v=identity();v.put("case_id",caseId);v.put("person_id",personId);v.put("role",role);d.insertOrThrow("person_roles",null,v);}d.setTransactionSuccessful();}finally{d.endTransaction();}}
    List<ContentValues> roles(long caseId){return rows("SELECT p.id,p.name,r.role FROM person_roles r JOIN clients p ON p.id=r.person_id WHERE r.case_id=? AND r.deleted_at IS NULL AND p.deleted_at IS NULL UNION ALL SELECT p.id,p.name,r.role FROM case_clients r JOIN clients p ON p.id=r.client_id WHERE r.case_id=? AND r.deleted_at IS NULL AND p.deleted_at IS NULL",""+caseId,""+caseId);}
    void contractParty(long contractId,long personId,String role){live("representation_contracts",contractId);live("clients",personId);if(!rows("SELECT id FROM contract_parties WHERE contract_id=? AND person_id=? AND role=? AND deleted_at IS NULL",""+contractId,""+personId,role).isEmpty())return;ContentValues v=identity();v.put("contract_id",contractId);v.put("person_id",personId);v.put("role",role);db().insertOrThrow("contract_parties",null,v);}
    String opponents(long contractId){StringBuilder b=new StringBuilder();for(ContentValues v:rows("SELECT p.name FROM contract_parties r JOIN clients p ON p.id=r.person_id WHERE r.contract_id=? AND r.deleted_at IS NULL AND p.deleted_at IS NULL",""+contractId)){if(b.length()>0)b.append("، ");b.append(text(v,"name"));}return b.toString();}
    long account(ContentValues input){ContentValues v=new ContentValues(input);required(text(v,"name"),"نام حساب");required(text(v,"kind"),"نوع حساب");v.putAll(identity());return db().insertOrThrow("financial_accounts",null,v);}
    long transaction(Long caseId,ContentValues input){ContentValues v=new ContentValues(input);if(!Arrays.asList("payment","expense").contains(text(v,"kind")))throw new IllegalArgumentException("نوع تراکنش معتبر نیست");if(v.getAsLong("amount")==null||v.getAsLong("amount")<=0)throw new IllegalArgumentException("مبلغ باید مثبت باشد");required(text(v,"entry_date"),"تاریخ");v.put("entry_date",date(text(v,"entry_date")));required(text(v,"category"),"نوع دریافت/هزینه");
        Long contract=v.getAsLong("contract_id");if(contract!=null){List<ContentValues> found=rows("SELECT case_id FROM representation_contracts WHERE id=? AND deleted_at IS NULL",""+contract);if(found.isEmpty())throw new IllegalArgumentException("قرارداد یافت نشد");Long linked=found.get(0).getAsLong("case_id");if(linked!=null&&!linked.equals(caseId))throw new IllegalArgumentException("پرونده تراکنش با قرارداد هماهنگ نیست");}
        if(caseId!=null)live("cases",caseId);for(String[] link:new String[][]{{"person_id","clients"},{"account_id","financial_accounts"},{"contract_id","representation_contracts"},{"collaborator_id","collaborators"}}){Long linkedId=v.getAsLong(link[0]);if(linkedId!=null)live(link[1],linkedId);}
        v.putAll(identity());if(caseId!=null)v.put("case_id",caseId);return db().insertOrThrow(caseId==null?"independent_ledger":"ledger",null,v);
    }
    List<ContentValues> transactions(Long caseId,Long personId,Long accountId,String kind,String start,String end){
        String sql="SELECT * FROM (SELECT id,case_id,kind,category,amount,entry_date,paid_by,description,person_id,account_id,contract_id,collaborator_id,payment_method,scope FROM ledger WHERE deleted_at IS NULL UNION ALL SELECT id,NULL case_id,kind,category,amount,entry_date,paid_by,description,person_id,account_id,contract_id,collaborator_id,payment_method,scope FROM independent_ledger WHERE deleted_at IS NULL) t WHERE 1=1";ArrayList<String> args=new ArrayList<>();
        if(caseId!=null){sql+=" AND case_id=?";args.add(""+caseId);}if(personId!=null){sql+=" AND person_id=?";args.add(""+personId);}if(accountId!=null){sql+=" AND account_id=?";args.add(""+accountId);}if(kind!=null){sql+=" AND kind=?";args.add(kind);}if(start!=null&&!start.isEmpty()){sql+=" AND entry_date>=?";args.add(date(start));}if(end!=null&&!end.isEmpty()){sql+=" AND entry_date<=?";args.add(date(end));}return rows(sql+" ORDER BY entry_date DESC,id DESC",args.toArray(new String[0]));
    }
    void live(String table,long id){if(rows("SELECT id FROM "+table+" WHERE id=? AND deleted_at IS NULL",Long.toString(id)).isEmpty())throw new IllegalArgumentException("رکورد مرتبط یافت نشد یا حذف شده است");}
    long collaboration(ContentValues input){ContentValues v=new ContentValues(input);required(text(v,"title"),"عنوان");required(text(v,"province"),"استان");location(text(v,"province"),text(v,"city"));location(text(v,"requester_province"),text(v,"requester_city"));v.put("due_date",date(text(v,"due_date")));v.put("sync_status","local");v.putAll(identity());return db().insertOrThrow("collaboration_requests",null,v);}
}
