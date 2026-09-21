package ir.kamranvahdati.lawoffice;

import java.util.Locale;

/** Pure validation helpers. Structural validation is not identity verification. */
final class InputValidators {
    private InputValidators() { }

    static String normalizeDigits(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '\u06f0' && c <= '\u06f9') out.append((char) ('0' + c - '\u06f0'));
            else if (c >= '\u0660' && c <= '\u0669') out.append((char) ('0' + c - '\u0660'));
            else out.append(c);
        }
        return out.toString().trim();
    }

    static boolean isValidIranianNationalId(String raw) {
        String value = normalizeDigits(raw).replace("-", "").replace(" ", "");
        if (!value.matches("[0-9]{10}") || value.matches("([0-9])\\1{9}")) return false;
        int sum = 0;
        for (int i = 0; i < 9; i++) sum += (value.charAt(i) - '0') * (10 - i);
        int remainder = sum % 11;
        int expected = remainder < 2 ? remainder : 11 - remainder;
        return value.charAt(9) - '0' == expected;
    }

    static String normalizeIranianMobile(String raw) {
        String value = normalizeDigits(raw).replaceAll("[\\s()\\-]", "");
        if (value.startsWith("0098")) value = "0" + value.substring(4);
        else if (value.startsWith("+98")) value = "0" + value.substring(3);
        else if (value.startsWith("98") && value.length() == 12) value = "0" + value.substring(2);
        return value;
    }

    static boolean isValidIranianMobile(String raw) {
        return normalizeIranianMobile(raw).matches("09[0-9]{9}");
    }

    static boolean isValidClock(String raw) {
        String value = normalizeDigits(raw);
        if (!value.matches("[0-9]{1,2}:[0-9]{2}")) return false;
        String[] parts = value.split(":", -1);
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        return hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59;
    }

    static String normalizeClock(String raw) {
        if (!isValidClock(raw)) throw new IllegalArgumentException("ساعت باید بین ۰۰:۰۰ تا ۲۳:۵۹ باشد");
        String[] parts = normalizeDigits(raw).split(":", -1);
        return String.format(Locale.US, "%02d:%02d", Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
}
