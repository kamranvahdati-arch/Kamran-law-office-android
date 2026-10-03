package ir.kamranvahdati.lawoffice;

/** V10.2 account boundary. No local professional verification or entitlement grants. */
public final class ProfessionalAccount {
    private ProfessionalAccount() { }
    public enum Verification { UNVERIFIED, PENDING, VERIFIED, REJECTED, SUSPENDED_OR_INVALID }
    public enum License { UNAVAILABLE, TRIAL, ACTIVE, EXPIRED, SUSPENDED, REVOKED }
    public enum ReferralStage { SENT, REGISTERED, TRIAL, PAID, SUCCESSFUL }
    public static final int BASE_TRIAL_DAYS = 7;
    public static final int REFERRED_TRIAL_DAYS = 30;
    public static final String VANITY_CODE = "VOKANO";
    public static final String VANITY_OWNER = "کامران وحدتی";
    public static final String UNAVAILABLE_MESSAGE = "اتصال به سامانه حساب، احراز وکالت و اشتراک هنوز فعال نیست";

    /** Policy preview only. The server establishes eligibility and authoritative start/end times. */
    public static int expectedTrialTotalDays(boolean serverConfirmedReferralBeforeOnboarding) {
        return serverConfirmedReferralBeforeOnboarding ? REFERRED_TRIAL_DAYS : BASE_TRIAL_DAYS;
    }
    /** Presentation/contract expectation only; never awards subscription time. */
    public static int milestoneTotalMonths(int successfulDirectReferrals) {
        if (successfulDirectReferrals < 0) throw new IllegalArgumentException("negative count");
        if (successfulDirectReferrals >= 5) return 12;
        if (successfulDirectReferrals >= 3) return 6;
        return successfulDirectReferrals >= 1 ? 1 : 0;
    }
    public static int referralsToNextMilestone(int successfulDirectReferrals) {
        if (successfulDirectReferrals < 0) throw new IllegalArgumentException("negative count");
        if (successfulDirectReferrals < 1) return 1;
        if (successfulDirectReferrals < 3) return 3 - successfulDirectReferrals;
        if (successfulDirectReferrals < 5) return 5 - successfulDirectReferrals;
        return 0;
    }
    public static boolean isReferralCodeFormat(String code) {
        return code != null && (VANITY_CODE.equals(code) || code.matches("V[0-9]{6}"));
    }
    /** Safe default for this release until an authenticated backend adapter exists. */
    public static Snapshot unavailable() { return new Snapshot(); }
    public static final class Snapshot {
        public final Verification verification = Verification.UNVERIFIED;
        public final License license = License.UNAVAILABLE;
        public final Integer remainingDays = null;
        public final String referralCode = null;
        // Populated only by a future reviewed authenticated account/configuration adapter.
        public final String verifiedDisplayName = null;
        public final String officialInvitationUrl = null;
        private Snapshot() { }
        public boolean canUseProfessionalCollaboration() { return false; }
        public String displayText() { return UNAVAILABLE_MESSAGE; }
    }
    /** Local evidence of acceptance only; server acknowledgment must be independently recorded. */
    public static final class TermsAcceptance {
        public final String termsVersion, privacyVersion;
        public final long acceptedAtEpochMillis;
        public final boolean serverAcknowledged = false;
        public TermsAcceptance(String termsVersion, String privacyVersion, long acceptedAtEpochMillis) {
            if (termsVersion == null || termsVersion.trim().isEmpty() || privacyVersion == null
                    || privacyVersion.trim().isEmpty() || acceptedAtEpochMillis <= 0)
                throw new IllegalArgumentException("version and timestamp required");
            this.termsVersion = termsVersion;
            this.privacyVersion = privacyVersion;
            this.acceptedAtEpochMillis = acceptedAtEpochMillis;
        }
    }
}
