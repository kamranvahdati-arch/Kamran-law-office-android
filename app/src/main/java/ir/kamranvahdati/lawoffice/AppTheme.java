package ir.kamranvahdati.lawoffice;

import android.graphics.Color;

/** Central theme presets; screens must consume these values instead of adding local palettes. */
final class AppTheme {
    static final String LIGHT = "professional_light";
    static final String DARK = "professional_dark";
    static final String LEGAL = "formal_legal";
    static final String TURQUOISE = "bar_turquoise";
    static final String BLACK_GOLD = "black_gold";

    final String id, title;
    final boolean dark;
    final int primary, accent, background, card, surfaceVariant, text, muted, divider,
            onPrimary, danger, success, statusIcons;

    private AppTheme(String id, String title, boolean dark, int primary, int accent,
                     int background, int card, int surfaceVariant, int text, int muted,
                     int divider, int onPrimary, int danger, int success, int statusIcons) {
        this.id=id; this.title=title; this.dark=dark; this.primary=primary; this.accent=accent;
        this.background=background; this.card=card; this.surfaceVariant=surfaceVariant;
        this.text=text; this.muted=muted; this.divider=divider; this.onPrimary=onPrimary;
        this.danger=danger; this.success=success; this.statusIcons=statusIcons;
    }

    static AppTheme from(String id) {
        if (DARK.equals(id)) return new AppTheme(DARK,"تیره حرفه‌ای",true,
                rgb("0A2D5B"),rgb("00C2A8"),rgb("0D1726"),rgb("18283B"),rgb("26364B"),rgb("F2F8FC"),rgb("B6C6D8"),rgb("41556D"),Color.WHITE,rgb("FF7784"),rgb("70D7AD"),Color.WHITE);
        if (LEGAL.equals(id)) return new AppTheme(LEGAL,"رسمی وکالت",false,
                rgb("28324A"),rgb("8A6D3B"),rgb("F4F1EA"),Color.WHITE,rgb("ECE7DD"),rgb("252B36"),rgb("626873"),rgb("D4CCBE"),Color.WHITE,rgb("B33D48"),rgb("32755A"),Color.WHITE);
        if (TURQUOISE.equals(id)) return new AppTheme(TURQUOISE,"فیروزه‌ای کانون",false,
                rgb("075E5A"),rgb("078C83"),rgb("F2F8F7"),Color.WHITE,rgb("E3F1EF"),rgb("183836"),rgb("56706E"),rgb("C9DEDB"),Color.WHITE,rgb("C84B55"),rgb("19865D"),Color.WHITE);
        if (BLACK_GOLD.equals(id)) return new AppTheme(BLACK_GOLD,"مشکی و طلایی",true,
                rgb("101010"),rgb("C9A84E"),rgb("090909"),rgb("1A1A1A"),rgb("29251C"),rgb("F5F0E3"),rgb("BDB5A2"),rgb("3F392A"),Color.WHITE,rgb("DD6670"),rgb("68B987"),Color.WHITE);
        return new AppTheme(LIGHT,"روشن حرفه‌ای",false,
                rgb("0A2D5B"),rgb("1478C8"),rgb("F3FAFC"),Color.WHITE,rgb("EAF3F7"),rgb("14293F"),rgb("536879"),rgb("D7E4EC"),Color.WHITE,rgb("BE2943"),rgb("096956"),Color.WHITE);
    }

    static String[] ids() { return new String[]{LIGHT,DARK,LEGAL,TURQUOISE,BLACK_GOLD}; }
    static String[] titles() { String[] ids=ids(), out=new String[ids.length]; for(int i=0;i<ids.length;i++)out[i]=from(ids[i]).title; return out; }

    private static int rgb(String hex) { return Color.parseColor("#"+hex); }
}
