package ir.kamranvahdati.lawoffice;

/** Future authenticated transport contract, intentionally without endpoint URLs or a fake server. */
public interface ProfessionalAccountApi {
    String CONTRACT_VERSION = "v1";
    // Every operation must authenticate the account; never infer account identity from referral code.
    void refreshAccount(Callback<AccountResponse> callback);
    void lookupReferral(String code, Callback<ReferralLookupResponse> callback);
    void confirmReferral(String lookupToken, String idempotencyKey, Callback<AccountResponse> callback);
    void submitVerification(VerificationRequest request, Callback<AccountResponse> callback);
    void transferDevice(String sourceInstallationId, String targetInstallationId,
                        long expectedRevision, String idempotencyKey, Callback<AccountResponse> callback);
    void acceptTerms(ProfessionalAccount.TermsAcceptance acceptance, String idempotencyKey, Callback<TermsReceipt> callback);

    interface Callback<T> { void success(T response); void unavailable(String reason); }
    /** Wire shapes for the future adapter, never authorization credentials in their own right. */
    final class AccountResponse {
        public String accountId, entitlementId, subscriptionType, professionalStatus;
        public String verificationSource, verifiedAt, lastProfessionalStatus;
        public String trialStartedAt, trialEndsAt, licenseExpiresAt, serverTime;
        public String referralCode, officialInvitationUrl;
        public ProfessionalAccount.Verification verification;
        public ProfessionalAccount.License license;
        public Integer remainingDays;
        public long revision;
    }
    final class ReferralLookupResponse {
        public String code, ownerAccountId, ownerDisplayName, lookupToken, expiresAt;
        // UI displays ownerDisplayName and requires explicit confirmation of lookupToken.
    }
    final class ReferralProgressResponse {
        public ProfessionalAccount.ReferralStage stage;
        public int successfulDirectReferrals, totalAwardedMonths, nextMilestoneRemaining;
        public String configurationVersion;
    }
    final class TermsReceipt {
        public String accountId, termsVersion, privacyVersion, acceptedAt, serverRecordedAt;
    }
    // Success callbacks require a reviewed authenticated transport; none are invoked offline.
    final class VerificationRequest {
        public final String firstName, lastName, licenseNumber, issuingBody, licenseStatus;
        public VerificationRequest(String firstName, String lastName, String licenseNumber,
                                   String issuingBody, String licenseStatus) {
            this.firstName = required(firstName); this.lastName = required(lastName);
            this.licenseNumber = required(licenseNumber); this.issuingBody = required(issuingBody);
            this.licenseStatus = required(licenseStatus);
        }
        private static String required(String value) {
            if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("professional field required");
            return value.trim();
        }
    }
    final class Unavailable implements ProfessionalAccountApi {
        private void fail(Callback<?> callback) { callback.unavailable(ProfessionalAccount.UNAVAILABLE_MESSAGE); }
        public void refreshAccount(Callback<AccountResponse> callback) { fail(callback); }
        public void lookupReferral(String code, Callback<ReferralLookupResponse> callback) { fail(callback); }
        public void confirmReferral(String token, String key, Callback<AccountResponse> callback) { fail(callback); }
        public void submitVerification(VerificationRequest request, Callback<AccountResponse> callback) { fail(callback); }
        public void transferDevice(String source, String target, long revision, String key, Callback<AccountResponse> callback) { fail(callback); }
        public void acceptTerms(ProfessionalAccount.TermsAcceptance acceptance, String key, Callback<TermsReceipt> callback) { fail(callback); }
    }
}
