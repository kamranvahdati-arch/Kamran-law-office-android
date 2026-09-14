package ir.kamranvahdati.lawoffice;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.print.*;
import android.text.format.DateFormat;
import android.view.*;
import android.webkit.*;
import android.widget.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private final int NAVY = Color.rgb(23, 38, 74);
    private final int BLUE = Color.rgb(49, 89, 216);
    private final int PAPER = Color.rgb(244, 246, 250);
    private final int INK = Color.rgb(22, 33, 54);
    private final int MUTED = Color.rgb(112, 124, 145);

    private Db db;
    private LinearLayout page;
    private LinearLayout root;
    private TextView title;
    private TextView subtitle;
    private int active = 0;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            startApplication();
        } catch (Throwable firstError) {
            // This public preview contains sample data only, so a damaged or
            // incompatible preview database can safely be recreated.
            try {
                deleteDatabase(Db.DATABASE_NAME);
                startApplication();
            } catch (Throwable finalError) {
                showStartupError(finalError);
            }
        }
    }

    private void startApplication() {
        getWindow().setStatusBarColor(NAVY);
        db = new Db(this);
        buildShell();
        showDashboard();
    }

    private void showStartupError(Throwable error) {
        LinearLayout fallback = new LinearLayout(this);
        fallback.setOrientation(LinearLayout.VERTICAL);
        fallback.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        fallback.setGravity(Gravity.CENTER);
        fallback.setPadding(dp(24), dp(24), dp(24), dp(24));
        fallback.setBackgroundColor(Color.WHITE);

        TextView heading = text("برنامه با خطا روبه‌رو شد", 20, INK);
        heading.setTypeface(null, 1);
        heading.setGravity(Gravity.CENTER);
        fallback.addView(heading);

        TextView message = text("لطفاً از این صفحه عکس بگیرید و برای پشتیبانی ارسال کنید.\\n\\n"
                + error.getClass().getSimpleName() + ": " + safe(error.getMessage()),
                13, Color.rgb(150, 40, 40));
        message.setGravity(Gravity.CENTER);
        message.setPadding(0, dp(18), 0, 0);
        fallback.addView(message);
        setContentView(fallback);
    }

    private void buildShell() {
        root = column();
        root.setBackgroundColor(PAPER);

        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(13), dp(20), dp(13));
        header.setBackgroundColor(NAVY);

        TextView mark = text("⚖", 27, Color.rgb(233, 201, 145));
        header.addView(mark, lp(dp(48), dp(48)));

        LinearLayout names = column();
        names.setPadding(dp(12), 0, 0, 0);
        title = text("میز کار امروز", 20, Color.WHITE);
        title.setTypeface(null, 1);
        subtitle = text("دفتر وکالت کامران وحدتی", 12, Color.rgb(192, 202, 223));
        names.addView(title);
        names.addView(subtitle);
        header.addView(names, weight());

        TextView add = text("+", 30, Color.WHITE);
        add.setGravity(Gravity.CENTER);
        add.setBackground(round(BLUE, 12));
        add.setOnClickListener(v -> showAddTask(null));
        header.addView(add, lp(dp(46), dp(46)));
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        page = column();
        page.setPadding(dp(14), dp(16), dp(14), dp(24));
        scroll.addView(page);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout nav = row();
        nav.setPadding(dp(4), dp(5), dp(4), dp(6));
        nav.setBackgroundColor(Color.WHITE);
        String[] labels = {"میز کار", "موکلان", "کارها", "تقویم", "پرونده‌ها", "تنظیمات"};
        String[] icons = {"⌂", "♙", "✓", "□", "▣", "⚙"};
        for (int i = 0; i < labels.length; i++) {
            final int destination = i;
            LinearLayout item = column();
            item.setGravity(Gravity.CENTER);
            TextView icon = text(icons[i], 18, i == 0 ? BLUE : MUTED);
            TextView label = text(labels[i], 9, i == 0 ? BLUE : MUTED);
            label.setGravity(Gravity.CENTER);
            item.addView(icon);
            item.addView(label);
            item.setOnClickListener(v -> navigate(destination));
            nav.addView(item, new LinearLayout.LayoutParams(0, dp(58), 1));
        }
        root.addView(nav);
        setContentView(root);
    }

    private void navigate(int n) {
        active = n;
        if (n == 0) showDashboard();
        else if (n == 1) showClients();
        else if (n == 2) showTasks();
        else if (n == 3) showCalendar();
        else if (n == 4) showCases();
        else showSettings();
    }

    private void clear(String heading, String subheading) {
        page.removeAllViews();
        title.setText(heading);
        subtitle.setText(subheading);
    }

    private void showDashboard() {
        clear("میز کار امروز", "نسخه آزمایشی آفلاین");
        LinearLayout stats = row();
        stats.addView(stat("پرونده فعال", String.valueOf(db.count("cases")), "▣"),
                new LinearLayout.LayoutParams(0, dp(112), 1));
        stats.addView(stat("کار باز", String.valueOf(db.openTasks()), "○"),
                new LinearLayout.LayoutParams(0, dp(112), 1));
        stats.addView(stat("انجام‌شده", String.valueOf(db.doneTasks()), "✓"),
                new LinearLayout.LayoutParams(0, dp(112), 1));
        page.addView(stats);

        sectionHead("کارهای پیش رو", "افزودن", v -> showAddTask(null));
        for (Task item : db.tasks(false, 4)) page.addView(taskCard(item));

        sectionHead("پرونده‌های نیازمند توجه", "افزودن", v -> showAddCase());
        for (CaseItem item : db.cases(3)) page.addView(caseCard(item));
    }

    private void showClients() {
        clear("موکلان", "مشخصات و پرونده‌های هر موکل");
        sectionHead("فهرست موکلان", "موکل جدید", v -> showAddClient());
        List<ClientItem> list = db.clients();
        if (list.isEmpty()) page.addView(empty("هنوز موکلی ثبت نشده است."));
        for (ClientItem item : list) page.addView(clientCard(item));
    }

    private void showTasks() {
        clear("برنامه کاری", "انجام‌شده، باز یا موکول‌شده");
        sectionHead("همه کارها", "کار جدید", v -> showAddTask(null));
        List<Task> list = db.tasks(true, 100);
        if (list.isEmpty()) page.addView(empty("هنوز کاری ثبت نشده است."));
        for (Task item : list) page.addView(taskCard(item));
    }

    private void showCalendar() {
        clear("تقویم کاری", "برای مشاهده برنامه، یک روز را انتخاب کنید");
        CalendarView calendar = new CalendarView(this);
        calendar.setFirstDayOfWeek(Calendar.SATURDAY);
        calendar.setBackgroundColor(Color.WHITE);
        page.addView(calendar, new LinearLayout.LayoutParams(-1, dp(310)));

        TextView selected = text("برنامه امروز · " + dateOf(calendar.getDate()), 15, INK);
        selected.setTypeface(null, 1);
        selected.setPadding(dp(4), dp(20), dp(4), dp(10));
        page.addView(selected);

        LinearLayout daily = column();
        page.addView(daily);
        renderDay(daily, dateOf(calendar.getDate()));

        calendar.setOnDateChangeListener((view, year, month, day) -> {
            String date = String.format(Locale.US, "%04d/%02d/%02d", year, month + 1, day);
            selected.setText("برنامه روز · " + date);
            renderDay(daily, date);
        });
    }

    private void renderDay(LinearLayout target, String date) {
        target.removeAllViews();
        List<Task> list = db.tasksOn(date);
        if (list.isEmpty()) target.addView(empty("برای این روز برنامه‌ای ثبت نشده است."));
        for (Task item : list) target.addView(taskCard(item));

        TextView add = text("+ افزودن کار برای " + date, 13, BLUE);
        add.setGravity(Gravity.CENTER);
        add.setPadding(dp(12), dp(14), dp(12), dp(14));
        add.setBackground(round(Color.WHITE, 12));
        add.setOnClickListener(v -> showAddTask(date));
        target.addView(add);
    }

    private void showCases() {
        clear("پرونده‌ها", "برای مشاهده حساب پرونده روی آن بزنید");
        sectionHead("فهرست پرونده‌ها", "پرونده جدید", v -> showAddCase());
        List<CaseItem> list = db.cases(100);
        if (list.isEmpty()) page.addView(empty("هنوز پرونده‌ای ثبت نشده است."));
        for (CaseItem item : list) page.addView(caseCard(item));
    }

    private void showSettings() {
        clear("تنظیمات", "نسخه ۰.۳ آزمایشی");
        page.addView(info("حریم خصوصی",
                "برنامه هیچ مجوز اینترنتی درخواست نمی‌کند و اطلاعات در حافظه داخلی گوشی ذخیره می‌شود."));
        page.addView(info("پشتیبان‌گیری",
                "قابلیت پشتیبان رمزگذاری‌شده در نسخه بعدی اضافه خواهد شد."));
        page.addView(info("گزارش مالی",
                "ریز حق‌الوکاله و هزینه‌های پرونده برای چاپ، ذخیره PDF و امضای موکل آماده می‌شود."));
    }

    private View taskCard(Task task) {
        LinearLayout card = row();
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(12), dp(12), dp(12));
        card.setBackground(round(Color.WHITE, 14));
        margin(card, 0, 0, 0, 9);

        CheckBox done = new CheckBox(this);
        done.setChecked(task.done == 1);
        done.setOnCheckedChangeListener((button, checked) -> {
            db.setDone(task.id, checked);
            if (active == 0) showDashboard();
            else if (active == 3) showCalendar();
            else showTasks();
        });
        card.addView(done);

        LinearLayout body = column();
        TextView heading = text(task.title, 14, INK);
        heading.setTypeface(null, 1);
        body.addView(heading);
        String state = task.status.equals("deferred") ? "موکول‌شده"
                : (task.done == 1 ? "انجام‌شده" : "باز");
        body.addView(text(safe(task.caseName) + " · " + safe(task.dueDate) + " · " + state,
                11, MUTED));
        card.addView(body, weight());

        LinearLayout actions = column();
        TextView priority = text(task.priority, 10,
                task.priority.equals("فوری") ? Color.rgb(190, 55, 55) : Color.rgb(153, 106, 22));
        priority.setGravity(Gravity.CENTER);
        priority.setPadding(dp(9), dp(5), dp(9), dp(5));
        priority.setBackground(round(task.priority.equals("فوری")
                ? Color.rgb(255, 235, 235) : Color.rgb(255, 244, 223), 20));
        actions.addView(priority);

        if (task.done == 0) {
            TextView defer = text("موکول به فردا", 9, BLUE);
            defer.setPadding(dp(4), dp(7), dp(4), 0);
            defer.setOnClickListener(v -> {
                db.defer(task.id, nextDate(task.dueDate));
                if (active == 0) showDashboard();
                else if (active == 3) showCalendar();
                else showTasks();
            });
            actions.addView(defer);
        }
        card.addView(actions);
        return card;
    }

    private View clientCard(ClientItem client) {
        LinearLayout card = column();
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(round(Color.WHITE, 14));
        margin(card, 0, 0, 0, 10);

        TextView name = text(client.name, 15, INK);
        name.setTypeface(null, 1);
        card.addView(name);
        card.addView(text("تلفن: " + safe(client.phone) + " · کد ملی: " + safe(client.nationalId),
                11, MUTED));
        card.addView(text("نام پدر: " + safe(client.fatherName) + " · تولد: "
                + safe(client.birthDate), 11, MUTED));
        if (!safe(client.address).equals("ثبت نشده"))
            card.addView(text("نشانی: " + client.address, 11, MUTED));
        return card;
    }

    private View caseCard(CaseItem item) {
        LinearLayout card = row();
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(15), dp(14), dp(15), dp(14));
        card.setBackground(round(Color.WHITE, 14));
        margin(card, 0, 0, 0, 9);

        TextView icon = text("§", 21, BLUE);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(round(Color.rgb(237, 241, 255), 11));
        card.addView(icon, lp(dp(44), dp(44)));

        LinearLayout body = column();
        body.setPadding(dp(12), 0, 0, 0);
        TextView heading = text(item.title, 14, INK);
        heading.setTypeface(null, 1);
        body.addView(heading);
        body.addView(text(safe(item.clientName) + " · " + safe(item.reference)
                + " · " + safe(item.stage), 11, MUTED));
        card.addView(body, weight());
        card.setOnClickListener(v -> showCaseAccount(item));
        return card;
    }

    private View ledgerCard(LedgerItem item) {
        LinearLayout card = row();
        card.setPadding(dp(14), dp(11), dp(14), dp(11));
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackground(round(Color.WHITE, 12));
        margin(card, 0, 0, 0, 8);

        LinearLayout body = column();
        TextView heading = text(item.category, 13, INK);
        heading.setTypeface(null, 1);
        body.addView(heading);
        body.addView(text(safe(item.date) + " · پرداخت‌کننده: " + safe(item.paidBy)
                + " · " + safe(item.description), 10, MUTED));
        card.addView(body, weight());

        TextView amount = text(money(item.amount), 12,
                item.kind.equals("payment") ? Color.rgb(35, 135, 83) : Color.rgb(190, 55, 55));
        amount.setTypeface(null, 1);
        card.addView(amount);
        return card;
    }

    private void showAddTask(String presetDate) {
        LinearLayout form = form();
        EditText name = input("عنوان کار");
        EditText caseName = input("پرونده مرتبط (اختیاری)");
        EditText due = input("برای انتخاب تاریخ لمس کنید");
        due.setFocusable(false);
        due.setText(presetDate == null ? today() : presetDate);
        due.setOnClickListener(v -> pickDate(due));

        Spinner priority = new Spinner(this);
        priority.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"عادی", "فوری", "کم"}));

        form.addView(name);
        form.addView(caseName);
        form.addView(due);
        form.addView(priority);

        new AlertDialog.Builder(this)
                .setTitle("ثبت کار جدید")
                .setView(form)
                .setNegativeButton("انصراف", null)
                .setPositiveButton("ثبت", (dialog, which) -> {
                    String value = name.getText().toString().trim();
                    if (!value.isEmpty()) {
                        db.addTask(value, caseName.getText().toString().trim(),
                                due.getText().toString().trim(),
                                priority.getSelectedItem().toString());
                        showTasks();
                    }
                }).show();
    }

    private void showAddClient() {
        LinearLayout form = form();
        EditText name = input("نام و نام خانوادگی");
        EditText nationalId = input("کد ملی");
        EditText father = input("نام پدر");
        EditText birth = input("تاریخ تولد");
        EditText phone = input("شماره تلفن");
        EditText address = input("نشانی");
        EditText notes = input("توضیحات");

        form.addView(name);
        form.addView(nationalId);
        form.addView(father);
        form.addView(birth);
        form.addView(phone);
        form.addView(address);
        form.addView(notes);

        new AlertDialog.Builder(this)
                .setTitle("ثبت موکل جدید")
                .setView(form)
                .setNegativeButton("انصراف", null)
                .setPositiveButton("ثبت", (dialog, which) -> {
                    String value = name.getText().toString().trim();
                    if (!value.isEmpty()) {
                        db.addClient(value, nationalId.getText().toString().trim(),
                                father.getText().toString().trim(),
                                birth.getText().toString().trim(),
                                phone.getText().toString().trim(),
                                address.getText().toString().trim(),
                                notes.getText().toString().trim());
                        showClients();
                    }
                }).show();
    }

    private void showAddCase() {
        LinearLayout form = form();
        EditText name = input("عنوان پرونده");
        EditText reference = input("مرجع رسیدگی");
        EditText stage = input("مرحله فعلی");
        EditText fee = input("حق‌الوکاله توافق‌شده (ریال)");
        fee.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        EditText agreement = input("شرح توافقات مالی");

        List<ClientItem> clients = db.clientsWithEmpty();
        Spinner client = new Spinner(this);
        client.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, clients));

        form.addView(name);
        form.addView(client);
        form.addView(reference);
        form.addView(stage);
        form.addView(fee);
        form.addView(agreement);

        new AlertDialog.Builder(this)
                .setTitle("ثبت پرونده جدید")
                .setView(form)
                .setNegativeButton("انصراف", null)
                .setPositiveButton("ثبت", (dialog, which) -> {
                    String value = name.getText().toString().trim();
                    if (!value.isEmpty()) {
                        ClientItem selected = (ClientItem) client.getSelectedItem();
                        db.addCase(value, reference.getText().toString().trim(),
                                stage.getText().toString().trim(), selected.id,
                                number(fee), agreement.getText().toString().trim());
                        showCases();
                    }
                }).show();
    }

    private void showCaseAccount(CaseItem item) {
        clear("حساب پرونده", item.title);
        AccountSummary summary = db.summary(item.id, item.feeAgreed);
        page.addView(info("موکل", safe(item.clientName)));
        page.addView(info("توافق مالی", safe(item.agreementNotes)));

        LinearLayout totals = row();
        totals.addView(stat("حق‌الوکاله", money(summary.feeAgreed), "◉"),
                new LinearLayout.LayoutParams(0, dp(112), 1));
        totals.addView(stat("دریافتی", money(summary.receivedFee), "✓"),
                new LinearLayout.LayoutParams(0, dp(112), 1));
        totals.addView(stat("مانده", money(summary.balance), "○"),
                new LinearLayout.LayoutParams(0, dp(112), 1));
        page.addView(totals);

        String accountState = summary.clientDebt > 0 ? "بدهکار"
                : summary.clientCredit > 0 ? "بستانکار" : "تسویه‌شده";
        page.addView(info("هزینه‌ها و وضعیت حساب",
                "کل هزینه‌ها: " + money(summary.expenses)
                        + "\nپرداخت‌شده توسط وکیل: " + money(summary.lawyerExpenses)
                        + "\nبازپرداخت هزینه توسط موکل: " + money(summary.reimbursements)
                        + "\nوضعیت موکل: " + accountState
                        + "\nبدهی موکل: " + money(summary.clientDebt)
                        + "\nبستانکاری موکل: " + money(summary.clientCredit)));

        sectionHead("ریز حساب", "ثبت مالی", v -> showAddLedger(item));
        List<LedgerItem> rows = db.ledger(item.id);
        if (rows.isEmpty()) page.addView(empty("هنوز پرداخت یا هزینه‌ای ثبت نشده است."));
        for (LedgerItem row : rows) page.addView(ledgerCard(row));

        TextView print = text("چاپ یا ذخیره PDF برای امضا", 13, Color.WHITE);
        print.setGravity(Gravity.CENTER);
        print.setPadding(dp(12), dp(14), dp(12), dp(14));
        print.setBackground(round(BLUE, 12));
        print.setOnClickListener(v -> printReport(item));
        page.addView(print);

        TextView share = text("اشتراک‌گذاری متن صورت‌حساب", 13, BLUE);
        share.setGravity(Gravity.CENTER);
        share.setPadding(dp(12), dp(14), dp(12), dp(14));
        share.setOnClickListener(v -> shareReport(item));
        page.addView(share);
    }

    private void showAddLedger(CaseItem item) {
        LinearLayout form = form();

        Spinner operation = new Spinner(this);
        operation.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{
                        "دریافت حق‌الوکاله از موکل",
                        "بازپرداخت هزینه از موکل",
                        "هزینه پرداختی توسط موکل",
                        "هزینه پرداختی توسط وکیل"
                }));

        Spinner category = new Spinner(this);
        category.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{
                        "هزینه دادرسی", "هزینه کارشناسی", "تمبر و خدمات قضایی",
                        "هتل", "بلیط هواپیما", "تاکسی و فرودگاه",
                        "اقامت و مأموریت", "سایر"
                }));

        EditText amount = input("مبلغ به ریال");
        amount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        EditText date = input("تاریخ");
        date.setText(today());
        EditText description = input("شرح، شماره رسید یا نحوه پرداخت");

        form.addView(operation);
        form.addView(category);
        form.addView(amount);
        form.addView(date);
        form.addView(description);

        new AlertDialog.Builder(this)
                .setTitle("ثبت پرداخت یا هزینه")
                .setView(form)
                .setNegativeButton("انصراف", null)
                .setPositiveButton("ثبت", (dialog, which) -> {
                    int selected = operation.getSelectedItemPosition();
                    String kind = selected < 2 ? "payment" : "expense";
                    String ledgerCategory = selected == 0 ? "حق‌الوکاله"
                            : selected == 1 ? "بازپرداخت هزینه"
                            : category.getSelectedItem().toString();
                    String paidBy = selected == 3 ? "وکیل" : "موکل";

                    db.addLedger(item.id, kind, ledgerCategory, number(amount),
                            date.getText().toString().trim(), paidBy,
                            description.getText().toString().trim());
                    showCaseAccount(db.caseById(item.id));
                }).show();
    }

    private void shareReport(CaseItem item) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, "صورت‌حساب پرونده " + item.title);
        send.putExtra(Intent.EXTRA_TEXT, db.report(item));
        startActivity(Intent.createChooser(send, "اشتراک‌گذاری صورت‌حساب"));
    }

    private void printReport(CaseItem item) {
        WebView web = new WebView(this);
        web.getSettings().setDefaultTextEncodingName("UTF-8");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                PrintManager manager =
                        (PrintManager) getSystemService(Context.PRINT_SERVICE);
                PrintDocumentAdapter adapter =
                        view.createPrintDocumentAdapter("صورت‌حساب " + item.title);
                manager.print("صورت‌حساب پرونده " + item.title, adapter,
                        new PrintAttributes.Builder()
                                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                .build());
            }
        });
        web.loadDataWithBaseURL(null, db.reportHtml(item),
                "text/html", "UTF-8", null);
    }

    private View stat(String label, String value, String icon) {
        LinearLayout box = column();
        box.setGravity(Gravity.CENTER);
        box.setPadding(3, 7, 3, 7);
        box.setBackground(round(Color.WHITE, 14));
        margin(box, 3, 0, 3, 0);
        box.addView(text(icon, 20, BLUE));
        TextView number = text(value, 16, INK);
        number.setGravity(Gravity.CENTER);
        number.setTypeface(null, 1);
        box.addView(number);
        TextView caption = text(label, 10, MUTED);
        caption.setGravity(Gravity.CENTER);
        box.addView(caption);
        return box;
    }

    private void sectionHead(String label, String action, View.OnClickListener listener) {
        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(2), dp(22), dp(2), dp(10));
        TextView heading = text(label, 16, INK);
        heading.setTypeface(null, 1);
        header.addView(heading, weight());
        TextView button = text(action, 12, BLUE);
        button.setPadding(dp(10), dp(7), dp(10), dp(7));
        button.setOnClickListener(listener);
        header.addView(button);
        page.addView(header);
    }

    private View info(String heading, String body) {
        LinearLayout card = column();
        card.setPadding(dp(17), dp(16), dp(17), dp(16));
        card.setBackground(round(Color.WHITE, 14));
        margin(card, 0, 0, 0, 12);
        TextView title = text(heading, 15, INK);
        title.setTypeface(null, 1);
        card.addView(title);
        TextView description = text(body, 12, MUTED);
        description.setPadding(0, dp(8), 0, 0);
        description.setLineSpacing(3, 1.2f);
        card.addView(description);
        return card;
    }

    private View empty(String value) {
        TextView view = text(value, 13, MUTED);
        view.setGravity(Gravity.CENTER);
        view.setPadding(10, dp(35), 10, dp(35));
        return view;
    }

    private LinearLayout form() {
        LinearLayout layout = column();
        layout.setPadding(dp(20), dp(8), dp(20), 0);
        return layout;
    }

    private EditText input(String hint) {
        EditText view = new EditText(this);
        view.setHint(hint);
        view.setTextSize(14);
        view.setSingleLine(true);
        view.setPadding(dp(12), dp(7), dp(12), dp(7));
        view.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(55)));
        return view;
    }

    private void pickDate(EditText target) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this,
                (view, year, month, day) -> target.setText(String.format(
                        Locale.US, "%04d/%02d/%02d", year, month + 1, day)),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private String dateOf(long millis) {
        return DateFormat.format("yyyy/MM/dd", new Date(millis)).toString();
    }

    private String today() {
        return dateOf(System.currentTimeMillis());
    }

    private String nextDate(String source) {
        Calendar calendar = Calendar.getInstance();
        try {
            String[] parts = source.split("/");
            if (parts.length == 3) {
                calendar.set(Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]) - 1,
                        Integer.parseInt(parts[2]));
            }
        } catch (Exception ignored) {
        }
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        return dateOf(calendar.getTimeInMillis());
    }

    private long number(EditText input) {
        try {
            return Long.parseLong(input.getText().toString()
                    .replace(",", "").trim());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "ثبت نشده" : value;
    }

    private String money(long amount) {
        return String.format(Locale.US, "%,d ریال", amount);
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return layout;
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return layout;
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        view.setTextDirection(View.TEXT_DIRECTION_RTL);
        return view;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        return shape;
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, -2, 1);
    }

    private LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private void margin(View view, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params =
                (LinearLayout.LayoutParams) view.getLayoutParams();
        if (params == null) params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        view.setLayoutParams(params);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }

    static class Task {
        long id;
        String title, caseName, dueDate, priority, status;
        int done;

        Task(long id, String title, String caseName, String dueDate,
             String priority, int done, String status) {
            this.id = id;
            this.title = title;
            this.caseName = caseName;
            this.dueDate = dueDate;
            this.priority = priority;
            this.done = done;
            this.status = status == null ? (done == 1 ? "done" : "open") : status;
        }
    }

    static class ClientItem {
        long id;
        String name, nationalId, fatherName, birthDate, phone, address, notes;

        ClientItem(long id, String name, String nationalId, String fatherName,
                   String birthDate, String phone, String address, String notes) {
            this.id = id;
            this.name = name;
            this.nationalId = nationalId;
            this.fatherName = fatherName;
            this.birthDate = birthDate;
            this.phone = phone;
            this.address = address;
            this.notes = notes;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static class CaseItem {
        long id, clientId, feeAgreed;
        String title, reference, stage, clientName, agreementNotes;

        CaseItem(long id, String title, String reference, String stage,
                 long clientId, String clientName, long feeAgreed,
                 String agreementNotes) {
            this.id = id;
            this.title = title;
            this.reference = reference;
            this.stage = stage;
            this.clientId = clientId;
            this.clientName = clientName;
            this.feeAgreed = feeAgreed;
            this.agreementNotes = agreementNotes;
        }
    }

    static class LedgerItem {
        long id, amount;
        String kind, category, date, paidBy, description;

        LedgerItem(long id, String kind, String category, long amount,
                   String date, String paidBy, String description) {
            this.id = id;
            this.kind = kind;
            this.category = category;
            this.amount = amount;
            this.date = date;
            this.paidBy = paidBy;
            this.description = description;
        }
    }

    static class AccountSummary {
        long feeAgreed, receivedFee, expenses, lawyerExpenses;
        long reimbursements, balance, clientDebt, clientCredit;
    }

    static class Db extends SQLiteOpenHelper {
        static final String DATABASE_NAME = "law_office_preview_v4.db";

        Db(Context context) {
            super(context, DATABASE_NAME, null, 3);
        }

        @Override
        public void onConfigure(SQLiteDatabase database) {
            super.onConfigure(database);
            database.setForeignKeyConstraintsEnabled(true);
        }

        @Override
        public void onCreate(SQLiteDatabase database) {
            database.execSQL("CREATE TABLE tasks("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "title TEXT NOT NULL,case_name TEXT,due_date TEXT,"
                    + "priority TEXT NOT NULL,done INTEGER NOT NULL DEFAULT 0,"
                    + "status TEXT NOT NULL DEFAULT 'open',"
                    + "created_at TEXT,updated_at TEXT)");

            database.execSQL("CREATE TABLE clients("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,"
                    + "national_id TEXT,father_name TEXT,birth_date TEXT,"
                    + "phone TEXT,address TEXT,notes TEXT,"
                    + "created_at TEXT,updated_at TEXT)");

            database.execSQL("CREATE TABLE cases("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,"
                    + "reference TEXT,stage TEXT,client_id INTEGER,"
                    + "fee_agreed INTEGER NOT NULL DEFAULT 0,"
                    + "agreement_notes TEXT,created_at TEXT,updated_at TEXT,"
                    + "FOREIGN KEY(client_id) REFERENCES clients(id) ON DELETE SET NULL)");

            database.execSQL("CREATE TABLE ledger("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,case_id INTEGER NOT NULL,"
                    + "kind TEXT NOT NULL,category TEXT NOT NULL,"
                    + "amount INTEGER NOT NULL DEFAULT 0,entry_date TEXT,"
                    + "paid_by TEXT,description TEXT,created_at TEXT,"
                    + "FOREIGN KEY(case_id) REFERENCES cases(id) ON DELETE CASCADE)");

            seed(database);
        }

        @Override
        public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
            database.beginTransaction();
            try {
                if (oldVersion < 2) {
                    database.execSQL("ALTER TABLE tasks ADD COLUMN status "
                            + "TEXT NOT NULL DEFAULT 'open'");
                    database.execSQL("ALTER TABLE tasks ADD COLUMN created_at TEXT");
                    database.execSQL("ALTER TABLE tasks ADD COLUMN updated_at TEXT");
                    database.execSQL("ALTER TABLE cases ADD COLUMN created_at TEXT");
                    database.execSQL("ALTER TABLE cases ADD COLUMN updated_at TEXT");
                    database.execSQL("UPDATE tasks SET status=CASE WHEN done=1 "
                            + "THEN 'done' ELSE 'open' END,"
                            + "created_at=datetime('now'),updated_at=datetime('now')");
                    database.execSQL("UPDATE cases SET created_at=datetime('now'),"
                            + "updated_at=datetime('now')");
                }
                if (oldVersion < 3) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS clients("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,"
                            + "national_id TEXT,father_name TEXT,birth_date TEXT,"
                            + "phone TEXT,address TEXT,notes TEXT,"
                            + "created_at TEXT,updated_at TEXT)");
                    database.execSQL("ALTER TABLE cases ADD COLUMN client_id INTEGER");
                    database.execSQL("ALTER TABLE cases ADD COLUMN fee_agreed "
                            + "INTEGER NOT NULL DEFAULT 0");
                    database.execSQL("ALTER TABLE cases ADD COLUMN agreement_notes TEXT");
                    database.execSQL("CREATE TABLE IF NOT EXISTS ledger("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "case_id INTEGER NOT NULL,kind TEXT NOT NULL,"
                            + "category TEXT NOT NULL,amount INTEGER NOT NULL DEFAULT 0,"
                            + "entry_date TEXT,paid_by TEXT,description TEXT,created_at TEXT,"
                            + "FOREIGN KEY(case_id) REFERENCES cases(id) ON DELETE CASCADE)");
                }
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
        }

        private void seed(SQLiteDatabase database) {
            Calendar calendar = Calendar.getInstance();
            String today = new SimpleDateFormat("yyyy/MM/dd", Locale.US)
                    .format(calendar.getTime());
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            String tomorrow = new SimpleDateFormat("yyyy/MM/dd", Locale.US)
                    .format(calendar.getTime());

            addTask(database, "بررسی نظریه کارشناسی",
                    "پرونده آزمایشی ملکی", today, "فوری");
            addTask(database, "مرور نهایی پیش‌نویس لایحه",
                    "پرونده آزمایشی تجاری", today, "عادی");
            addTask(database, "تماس آزمایشی با موکل",
                    "پرونده آزمایشی خانواده", tomorrow, "عادی");

            long client = addClient(database, "موکل آزمایشی", "۰۰۰۰۰۰۰۰۰۰",
                    "نام پدر نمونه", "۱۳۶۰/۰۱/۰۱", "۰۹۱۲۰۰۰۰۰۰۰",
                    "نشانی آزمایشی", "صرفاً اطلاعات نمونه");
            addCase(database, "پرونده آزمایشی ملکی",
                    "مرجع قضایی نمونه", "کارشناسی", client,
                    500000000L, "پرداخت حق‌الوکاله در دو مرحله");
            addCase(database, "پرونده آزمایشی تجاری",
                    "مرجع قضایی نمونه", "تجدیدنظر", client,
                    300000000L, "توافق آزمایشی");
        }

        void addTask(String title, String caseName, String dueDate, String priority) {
            addTask(getWritableDatabase(), title, caseName, dueDate, priority);
        }

        private void addTask(SQLiteDatabase database, String title,
                             String caseName, String dueDate, String priority) {
            ContentValues values = new ContentValues();
            String now = now();
            values.put("title", title);
            values.put("case_name", caseName);
            values.put("due_date", dueDate);
            values.put("priority", priority);
            values.put("status", "open");
            values.put("created_at", now);
            values.put("updated_at", now);
            database.insert("tasks", null, values);
        }

        void addClient(String name, String nationalId, String fatherName,
                       String birthDate, String phone, String address, String notes) {
            addClient(getWritableDatabase(), name, nationalId, fatherName,
                    birthDate, phone, address, notes);
        }

        private long addClient(SQLiteDatabase database, String name,
                               String nationalId, String fatherName,
                               String birthDate, String phone, String address,
                               String notes) {
            ContentValues values = new ContentValues();
            String now = now();
            values.put("name", name);
            values.put("national_id", nationalId);
            values.put("father_name", fatherName);
            values.put("birth_date", birthDate);
            values.put("phone", phone);
            values.put("address", address);
            values.put("notes", notes);
            values.put("created_at", now);
            values.put("updated_at", now);
            return database.insert("clients", null, values);
        }

        void addCase(String title, String reference, String stage,
                     long clientId, long fee, String agreement) {
            addCase(getWritableDatabase(), title, reference, stage,
                    clientId, fee, agreement);
        }

        private long addCase(SQLiteDatabase database, String title,
                             String reference, String stage, long clientId,
                             long fee, String agreement) {
            ContentValues values = new ContentValues();
            String now = now();
            values.put("title", title);
            values.put("reference", reference);
            values.put("stage", stage);
            if (clientId > 0) values.put("client_id", clientId);
            values.put("fee_agreed", fee);
            values.put("agreement_notes", agreement);
            values.put("created_at", now);
            values.put("updated_at", now);
            return database.insert("cases", null, values);
        }

        void addLedger(long caseId, String kind, String category, long amount,
                       String date, String paidBy, String description) {
            ContentValues values = new ContentValues();
            values.put("case_id", caseId);
            values.put("kind", kind);
            values.put("category", category);
            values.put("amount", amount);
            values.put("entry_date", date);
            values.put("paid_by", paidBy);
            values.put("description", description);
            values.put("created_at", now());
            getWritableDatabase().insert("ledger", null, values);
        }

        void setDone(long id, boolean checked) {
            ContentValues values = new ContentValues();
            values.put("done", checked ? 1 : 0);
            values.put("status", checked ? "done" : "open");
            values.put("updated_at", now());
            getWritableDatabase().update("tasks", values,
                    "id=?", new String[]{String.valueOf(id)});
        }

        void defer(long id, String date) {
            ContentValues values = new ContentValues();
            values.put("due_date", date);
            values.put("done", 0);
            values.put("status", "deferred");
            values.put("updated_at", now());
            getWritableDatabase().update("tasks", values,
                    "id=?", new String[]{String.valueOf(id)});
        }

        int count(String table) {
            Cursor cursor = getReadableDatabase()
                    .rawQuery("SELECT COUNT(*) FROM " + table, null);
            cursor.moveToFirst();
            int result = cursor.getInt(0);
            cursor.close();
            return result;
        }

        int openTasks() {
            Cursor cursor = getReadableDatabase()
                    .rawQuery("SELECT COUNT(*) FROM tasks WHERE done=0", null);
            cursor.moveToFirst();
            int result = cursor.getInt(0);
            cursor.close();
            return result;
        }

        int doneTasks() {
            return count("tasks") - openTasks();
        }

        List<Task> tasks(boolean all, int limit) {
            ArrayList<Task> result = new ArrayList<>();
            String sql = "SELECT id,title,case_name,due_date,priority,done,status "
                    + "FROM tasks " + (all ? "" : "WHERE done=0 ")
                    + "ORDER BY done ASC,id DESC LIMIT " + limit;
            Cursor cursor = getReadableDatabase().rawQuery(sql, null);
            while (cursor.moveToNext()) {
                result.add(new Task(cursor.getLong(0), cursor.getString(1),
                        cursor.getString(2), cursor.getString(3),
                        cursor.getString(4), cursor.getInt(5),
                        cursor.getString(6)));
            }
            cursor.close();
            return result;
        }

        List<Task> tasksOn(String date) {
            ArrayList<Task> result = new ArrayList<>();
            Cursor cursor = getReadableDatabase().rawQuery(
                    "SELECT id,title,case_name,due_date,priority,done,status "
                            + "FROM tasks WHERE due_date=? "
                            + "ORDER BY done ASC,id DESC",
                    new String[]{date});
            while (cursor.moveToNext()) {
                result.add(new Task(cursor.getLong(0), cursor.getString(1),
                        cursor.getString(2), cursor.getString(3),
                        cursor.getString(4), cursor.getInt(5),
                        cursor.getString(6)));
            }
            cursor.close();
            return result;
        }

        List<ClientItem> clients() {
            ArrayList<ClientItem> result = new ArrayList<>();
            Cursor cursor = getReadableDatabase().rawQuery(
                    "SELECT id,name,national_id,father_name,birth_date,"
                            + "phone,address,notes FROM clients ORDER BY name", null);
            while (cursor.moveToNext()) {
                result.add(new ClientItem(cursor.getLong(0), cursor.getString(1),
                        cursor.getString(2), cursor.getString(3),
                        cursor.getString(4), cursor.getString(5),
                        cursor.getString(6), cursor.getString(7)));
            }
            cursor.close();
            return result;
        }

        List<ClientItem> clientsWithEmpty() {
            ArrayList<ClientItem> result = new ArrayList<>();
            result.add(new ClientItem(0, "بدون موکل",
                    "", "", "", "", "", ""));
            result.addAll(clients());
            return result;
        }

        List<CaseItem> cases(int limit) {
            ArrayList<CaseItem> result = new ArrayList<>();
            Cursor cursor = getReadableDatabase().rawQuery(
                    "SELECT c.id,c.title,c.reference,c.stage,"
                            + "COALESCE(c.client_id,0),cl.name,c.fee_agreed,"
                            + "c.agreement_notes FROM cases c "
                            + "LEFT JOIN clients cl ON cl.id=c.client_id "
                            + "ORDER BY c.id DESC LIMIT " + limit, null);
            while (cursor.moveToNext()) result.add(caseFrom(cursor));
            cursor.close();
            return result;
        }

        CaseItem caseById(long id) {
            Cursor cursor = getReadableDatabase().rawQuery(
                    "SELECT c.id,c.title,c.reference,c.stage,"
                            + "COALESCE(c.client_id,0),cl.name,c.fee_agreed,"
                            + "c.agreement_notes FROM cases c "
                            + "LEFT JOIN clients cl ON cl.id=c.client_id "
                            + "WHERE c.id=?", new String[]{String.valueOf(id)});
            CaseItem item = null;
            if (cursor.moveToFirst()) item = caseFrom(cursor);
            cursor.close();
            return item;
        }

        private CaseItem caseFrom(Cursor cursor) {
            return new CaseItem(cursor.getLong(0), cursor.getString(1),
                    cursor.getString(2), cursor.getString(3),
                    cursor.getLong(4), cursor.getString(5),
                    cursor.getLong(6), cursor.getString(7));
        }

        List<LedgerItem> ledger(long caseId) {
            ArrayList<LedgerItem> result = new ArrayList<>();
            Cursor cursor = getReadableDatabase().rawQuery(
                    "SELECT id,kind,category,amount,entry_date,paid_by,description "
                            + "FROM ledger WHERE case_id=? ORDER BY id DESC",
                    new String[]{String.valueOf(caseId)});
            while (cursor.moveToNext()) {
                result.add(new LedgerItem(cursor.getLong(0), cursor.getString(1),
                        cursor.getString(2), cursor.getLong(3),
                        cursor.getString(4), cursor.getString(5),
                        cursor.getString(6)));
            }
            cursor.close();
            return result;
        }

        AccountSummary summary(long caseId, long fee) {
            AccountSummary summary = new AccountSummary();
            summary.feeAgreed = fee;
            for (LedgerItem item : ledger(caseId)) {
                if (item.kind.equals("payment")
                        && item.category.equals("حق‌الوکاله"))
                    summary.receivedFee += item.amount;
                if (item.kind.equals("payment")
                        && item.category.equals("بازپرداخت هزینه"))
                    summary.reimbursements += item.amount;
                if (item.kind.equals("expense")) {
                    summary.expenses += item.amount;
                    if (item.paidBy.equals("وکیل"))
                        summary.lawyerExpenses += item.amount;
                }
            }

            summary.balance = Math.max(0,
                    summary.feeAgreed - summary.receivedFee);
            long net = summary.feeAgreed + summary.lawyerExpenses
                    - summary.receivedFee - summary.reimbursements;
            summary.clientDebt = Math.max(0, net);
            summary.clientCredit = Math.max(0, -net);
            return summary;
        }

        String report(CaseItem item) {
            AccountSummary summary = summary(item.id, item.feeAgreed);
            StringBuilder text = new StringBuilder();
            text.append("صورت‌حساب پرونده\n\n");
            text.append("عنوان پرونده: ").append(item.title).append("\n");
            text.append("موکل: ").append(item.clientName == null
                    ? "ثبت نشده" : item.clientName).append("\n");
            text.append("مرجع رسیدگی: ").append(item.reference).append("\n");
            text.append("شرح توافق: ").append(item.agreementNotes == null
                    ? "ثبت نشده" : item.agreementNotes).append("\n\n");
            text.append("حق‌الوکاله توافق‌شده: ")
                    .append(format(summary.feeAgreed)).append(" ریال\n");
            text.append("حق‌الوکاله دریافت‌شده: ")
                    .append(format(summary.receivedFee)).append(" ریال\n");
            text.append("کل هزینه‌های پرونده: ")
                    .append(format(summary.expenses)).append(" ریال\n");
            text.append("هزینه‌های پرداختی وکیل: ")
                    .append(format(summary.lawyerExpenses)).append(" ریال\n");
            text.append("بازپرداخت هزینه توسط موکل: ")
                    .append(format(summary.reimbursements)).append(" ریال\n");
            text.append("بدهی موکل: ")
                    .append(format(summary.clientDebt)).append(" ریال\n");
            text.append("بستانکاری موکل: ")
                    .append(format(summary.clientCredit)).append(" ریال\n\n");
            text.append("ریز عملیات:\n");

            for (LedgerItem row : ledger(item.id)) {
                text.append(row.date).append(" | ")
                        .append(row.category).append(" | ")
                        .append(format(row.amount)).append(" ریال | پرداخت‌کننده: ")
                        .append(row.paidBy).append(" | ")
                        .append(row.description == null ? "" : row.description)
                        .append("\n");
            }

            text.append("\nاینجانب، مفاد و مبالغ این صورت‌حساب را مشاهده و تأیید نمودم.\n\n");
            text.append("نام و امضای موکل: ........................\n");
            text.append("تاریخ: ........................\n");
            text.append("نام و امضای وکیل: ........................");
            return text.toString();
        }

        String reportHtml(CaseItem item) {
            String escaped = report(item)
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\n", "<br>");
            return "<!doctype html><html dir='rtl'><head><meta charset='utf-8'>"
                    + "<style>body{font-family:sans-serif;direction:rtl;text-align:right;"
                    + "padding:36px;line-height:2;color:#162136}"
                    + "h1{color:#17264a;border-bottom:2px solid #3159d8;padding-bottom:12px}"
                    + ".box{border:1px solid #ccd2df;border-radius:10px;padding:18px}"
                    + "</style></head><body><h1>دفتر وکالت کامران وحدتی</h1>"
                    + "<div class='box'>" + escaped + "</div></body></html>";
        }

        static String now() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    .format(new Date());
        }

        static String format(long amount) {
            return String.format(Locale.US, "%,d", amount);
        }
    }
}
