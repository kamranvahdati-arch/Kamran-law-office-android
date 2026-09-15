package ir.kamranvahdati.lawoffice;

import java.util.Calendar;
import java.util.Locale;

final class JalaliDate {
    final int year;
    final int month;
    final int day;

    JalaliDate(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    static JalaliDate today() {
        Calendar c = Calendar.getInstance();
        return fromGregorian(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH));
    }

    String value() {
        return String.format(Locale.US, "%04d/%02d/%02d", year, month, day);
    }

    static String monthName(int month) {
        String[] names = {"فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
                "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"};
        return names[Math.max(1, Math.min(12, month)) - 1];
    }

    static int monthLength(int year, int month) {
        if (month <= 6) return 31;
        if (month <= 11) return 30;
        int[] g1 = toGregorian(year, 12, 1);
        int[] g2 = toGregorian(year + 1, 1, 1);
        Calendar a = Calendar.getInstance();
        a.clear();
        a.set(g1[0], g1[1] - 1, g1[2]);
        Calendar b = Calendar.getInstance();
        b.clear();
        b.set(g2[0], g2[1] - 1, g2[2]);
        return (int) ((b.getTimeInMillis() - a.getTimeInMillis()) / 86400000L);
    }

    static int firstWeekday(int year, int month) {
        int[] g = toGregorian(year, month, 1);
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(g[0], g[1] - 1, g[2]);
        int day = c.get(Calendar.DAY_OF_WEEK);
        return (day + 1) % 7; // Saturday=0 ... Friday=6
    }

    static JalaliDate fromGregorian(int gy, int gm, int gd) {
        int[] gdm = {0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334};
        int gy2 = gm > 2 ? gy + 1 : gy;
        int days = 355666 + 365 * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100
                + (gy2 + 399) / 400 + gd + gdm[gm - 1];
        int jy = -1595 + 33 * (days / 12053);
        days %= 12053;
        jy += 4 * (days / 1461);
        days %= 1461;
        if (days > 365) {
            jy += (days - 1) / 365;
            days = (days - 1) % 365;
        }
        int jm;
        int jd;
        if (days < 186) {
            jm = 1 + days / 31;
            jd = 1 + days % 31;
        } else {
            jm = 7 + (days - 186) / 30;
            jd = 1 + (days - 186) % 30;
        }
        return new JalaliDate(jy, jm, jd);
    }

    static int[] toGregorian(int jy, int jm, int jd) {
        jy += 1595;
        int days = -355668 + 365 * jy + (jy / 33) * 8 + ((jy % 33) + 3) / 4
                + jd + (jm < 7 ? (jm - 1) * 31 : (jm - 7) * 30 + 186);
        int gy = 400 * (days / 146097);
        days %= 146097;
        if (days > 36524) {
            gy += 100 * (--days / 36524);
            days %= 36524;
            if (days >= 365) days++;
        }
        gy += 4 * (days / 1461);
        days %= 1461;
        if (days > 365) {
            gy += (days - 1) / 365;
            days = (days - 1) % 365;
        }
        int gd = days + 1;
        int[] sal = {0, 31, ((gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0) ? 29 : 28,
                31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        int gm = 1;
        while (gm <= 12 && gd > sal[gm]) gd -= sal[gm++];
        return new int[]{gy, gm, gd};
    }

    static JalaliDate parse(String raw) {
        if(raw==null)throw new IllegalArgumentException("تاریخ را وارد کنید");
        String text=asciiDigits(raw.trim().replace('-', '/'));
        if(!text.matches("[0-9]{4}/[0-9]{1,2}/[0-9]{1,2}"))throw new IllegalArgumentException("تاریخ شمسی به شکل ۱۴۰۵/۰۶/۲۴ باشد");
        String[] p=text.split("/");int y=Integer.parseInt(p[0]),m=Integer.parseInt(p[1]),d=Integer.parseInt(p[2]);
        if(y<1200||y>1700||m<1||m>12||d<1||d>monthLength(y,m))throw new IllegalArgumentException("تاریخ شمسی معتبر نیست");
        return new JalaliDate(y,m,d);
    }

    static String asciiDigits(String raw){StringBuilder b=new StringBuilder();for(char c:raw.toCharArray()){if(c>='۰'&&c<='۹')b.append((char)('0'+c-'۰'));else if(c>='٠'&&c<='٩')b.append((char)('0'+c-'٠'));else b.append(c);}return b.toString();}

    static Calendar calendar(String date){JalaliDate j=parse(date);int[] g=toGregorian(j.year,j.month,j.day);Calendar c=Calendar.getInstance();c.clear();c.set(g[0],g[1]-1,g[2],12,0,0);return c;}

    static String addDays(String date,int days){Calendar c=calendar(date);c.add(Calendar.DAY_OF_MONTH,days);return fromGregorian(c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH)).value();}

    static int daysBetween(String start,String end){Calendar a=calendar(start),b=calendar(end);long delta=b.getTimeInMillis()-a.getTimeInMillis();return (int)Math.round(delta/86400000.0);}
}
