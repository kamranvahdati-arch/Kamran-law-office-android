# VOKANO 10 production baseline

The owner superseded the 9.1 migration requirement: 9.1 contains only disposable test data. V10 is a clean install. No further 9.1 migration implementation or CI transfer tests are required. Existing schema upgrade code is retained for future data preservation, not replaced with destructive fallback.

- Permanent application ID: `ir.kamranvahdati.lawoffice`.
- Version name: `10.0`; version code: `13`; SQLCipher schema: `13` (unchanged).
- Release onCreate does not run demo seeding. Only BuildConfig.DEBUG enables the existing fixtures.
- Release keystore/passwords must never appear in the repository, source ZIP, logs or APK. Debug signing is permitted only in explicit test builds (`-PallowTestSigning`). Operational builds require all VOKANO_RELEASE_* variables and `-PrequireProductionSigning`.

## Full backup

The UI writes VKB3 encrypted backups with a ZIP payload containing the complete KLO-2 database snapshot, nonsecret profile settings (including photo/logo references), every attachment including soft-deleted records, and all finalized office-media files. The database snapshot is transactional. AES-256-GCM authenticates ordered 1 MiB frames with an authenticated final frame; PBKDF2-HMAC-SHA256 uses a random salt and 210,000 iterations. No media is silently omitted. Streaming media supports backups over the previous 25 MiB limit. The JSON manifest limit is 32 MiB; available device storage limits media staging.

Restore authenticates the entire encrypted file before live data changes, rejects duplicate/unexpected ZIP entries and unsupported schemas, validates every file size/SHA-256 and reference, stages fresh private media names, rewrites URIs for the current installation, and imports database rows transactionally with foreign key checks. Existing media files are not deleted during restore. A failed prepared restore removes only the new staged files. Profile passwords, biometric state and encryption keys are not exported. Profile preferences are synchronously restored with rollback on a reported DB error; Android preferences and SQLCipher are separate stores, so a power loss during final commit is not a single cross-store atomic transaction. File bytes remain preserved. Backup files must be stored outside app private storage before uninstalling the app.

The ordinary UI does not claim that a legacy 9.1 database-only backup contains files. Full backup restore supports the new version-10 full archive.

## Permanent signing bootstrap and future releases

`bootstrap-production.yml` is an explicitly labeled one-time workflow for the owner-authorized first identity. It checks out the PR head SHA, obtains a unique draft-release lock, creates a 4096-bit RSA Android signing key in runner temporary storage, and immediately encrypts the recovery bundle using AES-256-GCM and the owner's RSA-OAEP-SHA256 recovery public key. Only the public recovery key is tracked. The encrypted recovery is preserved as a draft release asset before building. Neither the plaintext keystore nor credentials are uploaded to GitHub. Do not remove the `vokano-permanent-signing-identity` release lock or regenerate a key for an update. A failed attempt requires investigation and reuse/recovery of the existing identity, not another key.

The permanent-signed APK is built once and the exact same bytes are installed/tested on API 30 and 35. `provenance.json` binds APK/source/certificate hashes to the source commit. Instrumentation APK is a separate test-only artifact and is never part of the user app.

Future builds use `release.yml`, the protected `vokano-release` environment and these secrets: `VOKANO_RELEASE_KEYSTORE_BASE64`, `VOKANO_RELEASE_KEY_ALIAS`, `VOKANO_RELEASE_STORE_PASSWORD`, `VOKANO_RELEASE_KEY_PASSWORD`, `VOKANO_RELEASE_CERT_SHA256`. `release-existing-key.py` refuses missing secrets or a different certificate. Owner must retain the original keystore and credentials in two secure independent locations. Set protected environment approvals on GitHub before enabling unattended releases. Never attach signing passwords to issues, PRs or logs.

## Required test evidence

A green ordinary build verifies code with TEST signing only. It is not production delivery. ProductionBaselineTest is separately run against the non-debuggable release APK: empty initial database, representative clients/cases/contracts/payments/checks/tasks/deadlines/hearings/consultations/photo/PDF/28 MiB attachment, full backup, OS `pm clear`, restore with a newly created database key, all-table/field comparison, file SHA-256 equality, actual PDF/image decoding, wrong-password refusal and foreign-key rollback. Then real release dashboard/navigation/themes and Jalali/time dialogs are exercised. The normal debug suite separately covers detailed editing, soft deletion/restore, reminders and earlier schema upgrades.

No production claim is valid until both the permanent-signing workflow and both device jobs have completed successfully. Detailed actual results belong in the delivery report, not inferred from test source.
