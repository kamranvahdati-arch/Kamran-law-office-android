package ir.kamranvahdati.lawoffice;

/** Server-dependent features remain off until a production backend and signed contract exist. */
final class FeatureFlags {
    static final boolean REMOTE_ACCOUNT_VERIFICATION = false;
    static final boolean REMOTE_TRIAL_ENTITLEMENT = false;
    static final boolean REMOTE_PAYMENT = false;
    static final boolean REMOTE_ADMIN_NOTICES = false;
    static final boolean REMOTE_SECURE_UPDATE = false;
    static final boolean REMOTE_LEGAL_LIBRARY = false;
    static final boolean PUBLIC_LAWYER_PROFILE = false;
    private FeatureFlags() { }
}
