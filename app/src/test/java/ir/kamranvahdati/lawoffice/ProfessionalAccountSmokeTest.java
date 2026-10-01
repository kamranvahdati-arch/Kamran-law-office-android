package ir.kamranvahdati.lawoffice;

public final class ProfessionalAccountSmokeTest {
    public static void main(String[] args) {
        int[] totals = {0, 1, 1, 6, 6, 12, 12};
        for (int i = 0; i < totals.length; i++) check(ProfessionalAccount.milestoneTotalMonths(i) == totals[i], "total " + i);
        check(ProfessionalAccount.milestoneTotalMonths(3) - ProfessionalAccount.milestoneTotalMonths(1) == 5, "increment 3");
        check(ProfessionalAccount.milestoneTotalMonths(5) - ProfessionalAccount.milestoneTotalMonths(3) == 6, "increment 5");
        check(ProfessionalAccount.referralsToNextMilestone(2) == 1, "distance");
        check(ProfessionalAccount.referralsToNextMilestone(5) == 0, "final milestone");
        check(ProfessionalAccount.BASE_TRIAL_DAYS == 7 && ProfessionalAccount.REFERRED_TRIAL_DAYS == 30, "total trial policy");
        check(ProfessionalAccount.expectedTrialTotalDays(false) == 7, "base trial total");
        check(ProfessionalAccount.expectedTrialTotalDays(true) == 30, "referral total never 37");
        check(ProfessionalAccount.isReferralCodeFormat("V000001"), "leading zeros");
        check(ProfessionalAccount.isReferralCodeFormat("VOKANO"), "vanity format");
        check(!ProfessionalAccount.isReferralCodeFormat("V1234567"), "length");
        check(!ProfessionalAccount.isReferralCodeFormat("V۱۲۳۴۵۶"), "ASCII contract");
        check(!ProfessionalAccount.unavailable().canUseProfessionalCollaboration(), "unavailable gate");
        check(ProfessionalAccount.unavailable().remainingDays == null, "no fake countdown");
        final int[] failures = {0};
        ProfessionalAccountApi api = new ProfessionalAccountApi.Unavailable();
        ProfessionalAccountApi.Callback<ProfessionalAccountApi.AccountResponse> callback = rejectingCallback(failures);
        api.lookupReferral("VOKANO", rejectingCallback(failures));
        api.confirmReferral("fabricated", "key", callback);
        api.refreshAccount(callback);
        check(failures[0] == 3, "offline operations fail even vanity");
        ProfessionalAccount.TermsAcceptance acceptance = new ProfessionalAccount.TermsAcceptance("1", "1", 1L);
        check(!acceptance.serverAcknowledged, "no false server receipt");
        try { new ProfessionalAccount.TermsAcceptance("", "1", 1); throw new AssertionError("empty version"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Professional account foundation tests passed; no live backend tests claimed");
    }
    private static <T> ProfessionalAccountApi.Callback<T> rejectingCallback(final int[] failures) {
        return new ProfessionalAccountApi.Callback<T>() {
            public void success(T response) { throw new AssertionError("offline success fabricated"); }
            public void unavailable(String reason) { check(!reason.isEmpty(), "reason"); failures[0]++; }
        };
    }
    private static void check(boolean condition, String label) { if (!condition) throw new AssertionError(label); }
}
