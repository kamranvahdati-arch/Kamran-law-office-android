package ir.kamranvahdati.lawoffice;

final class DeadlineDateSmokeTest {
    public static void main(String[] args) {
        check("1405/07/01".equals(JalaliDate.addDays("۱۴۰۵/۰۶/۲۵", 7)), "جمع روز و تبدیل ارقام");
        check(JalaliDate.daysBetween("1405/06/25", "1405/07/01") == 7, "روز شمار بین دو ماه");
        check(JalaliDate.daysBetween("1405/07/01", "1405/06/25") == -7, "روزهای گذشته");
        check("1405/01/01".equals(JalaliDate.addDays("1404/12/29", 1)), "مرز سال شمسی");
        try { JalaliDate.parse("1405/13/01"); throw new AssertionError("تاریخ نامعتبر پذیرفته شد"); }
        catch (IllegalArgumentException expected) { }
        java.util.TimeZone previous=java.util.TimeZone.getDefault();
        try {
            for(String zone:new String[]{"UTC","Asia/Tehran","America/New_York","Pacific/Apia","Europe/Berlin"}) {
                java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(zone));
                for(int remaining:new int[]{10,3,2,1,0,-1,-10}) {
                    String due=JalaliDate.addDays("1405/06/29",remaining);
                    check(JalaliDate.daysBetween("1405/06/29",due)==remaining,"countdown "+zone+" "+remaining);
                }
                // Samoa skipped a local date in 2011; civil legal dates must not skip it.
                String skipped=JalaliDate.fromGregorian(2011,12,30).value();
                String before=JalaliDate.fromGregorian(2011,12,29).value();
                check(JalaliDate.addDays(before,1).equals(skipped),"civil date "+zone);
                check(JalaliDate.monthLength(1399,12)==30,"leap Esfand "+zone);
                check(JalaliDate.monthLength(1400,12)==29,"nonleap Esfand "+zone);
            }
        } finally { java.util.TimeZone.setDefault(previous); }
        System.out.println("Jalali deadline smoke tests passed");
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
