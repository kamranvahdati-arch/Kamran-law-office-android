package ir.kamranvahdati.lawoffice;

final class DeadlineDateSmokeTest {
    public static void main(String[] args) {
        check("1405/07/01".equals(JalaliDate.addDays("۱۴۰۵/۰۶/۲۵", 7)), "جمع روز و تبدیل ارقام");
        check(JalaliDate.daysBetween("1405/06/25", "1405/07/01") == 7, "روز شمار بین دو ماه");
        check(JalaliDate.daysBetween("1405/07/01", "1405/06/25") == -7, "روزهای گذشته");
        check("1405/01/01".equals(JalaliDate.addDays("1404/12/29", 1)), "مرز سال شمسی");
        try { JalaliDate.parse("1405/13/01"); throw new AssertionError("تاریخ نامعتبر پذیرفته شد"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Jalali deadline smoke tests passed");
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
