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
    final int primary, accent, background, card, text, muted, danger, success, statusIcons;

    private AppTheme(String id, String title, boolean dark, int primary, int accent,
                     int background, int card, int text, int muted, int danger, int success,
                     int statusIcons) {
        this.id=id; this.title=title; this.dark=dark; this.primary=primary; this.accent=accent;
        this.background=background; this.card=card; this.text=text; this.muted=muted;
        this.danger=danger; this.success=success; this.statusIcons=statusIcons;
    }

    static AppTheme from(String id) {
        if (DARK.equals(id)) return new AppTheme(DARK,"تیره حرفه‌ای",true,
                rgb("111827"),rgb("7797FF"),rgb("0F141E"),rgb("1D2431"),rgb("EEF2FA"),rgb("ADB8CA"),rgb("E35D6A"),rgb("55C98E"),Color.WHITE);
        if (LEGAL.equals(id)) return new AppTheme(LEGAL,"رسمی وکالت",false,
                rgb("28324A"),rgb("8A6D3B"),rgb("F4F1EA"),Color.WHITE,rgb("252B36"),rgb("6E7480"),rgb("B33D48"),rgb("32755A"),Color.WHITE);
        if (TURQUOISE.equals(id)) return new AppTheme(TURQUOISE,"فیروزه‌ای کانون",false,
                rgb("075E5A"),rgb("078C83"),rgb("F2F8F7"),Color.WHITE,rgb("183836"),rgb("607A78"),rgb("C84B55"),rgb("19865D"),Color.WHITE);
        if (BLACK_GOLD.equals(id)) return new AppTheme(BLACK_GOLD,"مشکی و طلایی",true,
                rgb("101010"),rgb("C9A84E"),rgb("090909"),rgb("1A1A1A"),rgb("F5F0E3"),rgb("BDB5A2"),rgb("DD6670"),rgb("68B987"),Color.WHITE);
        return new AppTheme(LIGHT,"روشن حرفه‌ای",false,
                rgb("152446"),rgb("2D56D2"),rgb("F4F7FC"),Color.WHITE,rgb("182236"),rgb("67748B"),rgb("D2454E"),rgb("1F8454"),Color.WHITE);
    }

    static String[] ids() { return new String[]{LIGHT,DARK,LEGAL,TURQUOISE,BLACK_GOLD}; }
    static String[] titles() { String[] ids=ids(), out=new String[ids.length]; for(int i=0;i<ids.length;i++)out[i]=from(ids[i]).title; return out; }

    private static int rgb(String hex) { return Color.parseColor("#"+hex); }
}
