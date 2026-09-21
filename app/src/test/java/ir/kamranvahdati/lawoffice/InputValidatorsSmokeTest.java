package ir.kamranvahdati.lawoffice;

public final class InputValidatorsSmokeTest {
    public static void main(String[] args) {
        expect(InputValidators.isValidIranianNationalId("0013540831"), "valid national id");
        expect(InputValidators.isValidIranianNationalId("۰۰۱۳۵۴۰۸۳۱"), "Persian digits");
        expect(!InputValidators.isValidIranianNationalId("1111111111"), "repeated digits");
        expect(!InputValidators.isValidIranianNationalId("0013540838"), "checksum");
        expect(InputValidators.isValidIranianMobile("09128402768"), "local mobile");
        expect(InputValidators.isValidIranianMobile("+989128402768"), "international mobile");
        expect(!InputValidators.isValidIranianMobile("0912000000"), "short mobile");
        expect("13:25".equals(InputValidators.normalizeClock("۱۳:۲۵")), "clock normalization");
        expect(!InputValidators.isValidClock("24:00"), "invalid clock");
        System.out.println("Input validator smoke tests passed");
    }

    private static void expect(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
}
