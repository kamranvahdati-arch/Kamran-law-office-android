# VOKANO 10.2 execution checkpoint

Status: IN DEVELOPMENT. Not release-ready. Do not install this development checkpoint over real data.

## Verified starting point

- Repository: `kamranvahdati-arch/Kamran-law-office-android` (verified through GitHub and clone).
- Starting branch: `codex/v10.1`; clean working tree.
- Starting HEAD: `3df8fdbdb0f290b5231280aa343e20ba03d7a345` (GitHub branch and git agree).
- Development branch: `codex/v10.2`.
- Existing database schema: 14; additive migration chain through `OfficeV101.migrate` reviewed before edits.
- Existing app ID: `ir.kamranvahdati.lawoffice`; versionCode 14 / versionName 10.1.
- Existing permanent signing configuration preserved. No signing secret has been read or added.
- GitHub original-production upgrade run `36239832085` queried directly: API 30 and API 35 jobs both success. This is 10.1 evidence, NOT evidence for 10.2.

## Frozen scope

The owner's master execution instruction for 10.2 supersedes conflicting older Master Source decisions, especially attachment-inclusive backups and referral rewards.

1. Preserve ID, signing, all real data and existing healthy features; no reset/destructive migration/release seeds.
2. Refine existing Persian RTL UI with coherent icons, semantic colors, hierarchy and safe light/dark system insets; preserve approved brand assets.
3. Exact menu: dashboard/calendar, cases/clients, tasks, persons, accounting, cooperation, lawyer profile, letterhead, legal content, reports, secure backup, SMS, settings, about, contact.
4. Separate person type from case role; real client count/list versus all persons; extend roles without copying identities.
5. Server-authoritative verification, account entitlement, device transfer and direct-only referrals. Unavailable backend must be explicit and fail closed for professional collaboration/awards.
6. Trial 7 days; valid pre-onboarding referral makes TOTAL trial 30 days. Server-issued permanent random `V` + 6 digits; reserved VOKANO owner Kamran Vahdati; owner confirmation required. Total rewards at 1/3/5 successful referrals: 1/6/12 months, not cumulative stacking. Payment finality and fraud controls are server dependencies.
7. Editable SMS templates and case/role/category recipients through Android SMS app only. No SEND_SMS permission. No invented invitation/download URL.
8. Encrypted metadata-only backup; safely validated replacement per day, retention 30 days, deletion confined to selected workspace. Provider deletion failure gives warning, not backup failure.
9. User-selected SAF workspace survives uninstall; stable case UID folders, portable relative references, old attachment preservation, missing-file handling and reinstall/relink/restore.
10. Professional letterhead, blank printing and subtle VOKANO/vokano.ir footer. Approved owner photo; retain existing contact methods and add clickable vokano.ir.
11. Help, versioned/timestamped terms/privacy acceptance; versioned legal-content interface, no fabricated legal content or coupling to personal website internals.
12. API 30/35, clean release, 10.1→10.2 and feasible 10→10.2 installed upgrades, light/dark/RTL, SAF/backup/reinstall, SMS, account/referral tests; existing-key release upgrade and CI before Ready.

## Work allocation and resume

Actual delegated model routing: SOL for UI and build/test tooling; ASTRA for storage/migration and account/security contracts. Parent handles integration and checkpoints. Integration review must use ASTRA before release.

Resume by checking remote/local HEAD, git status/diff, this file and task-specific evidence. Preserve uncommitted work. Do not re-audit or recreate prior features.

Current first incomplete gate: finish scoped emulator regressions for the latest source, then sign the exact APK with the existing identity and run original-production 10.1→10.2. A test-signed 10.2 APK has compiled in CI; it is not approved for real data or delivery.

## Continuation evidence (2026-10-02)

- Local development checkpoint `6722e433ce968893df1d05fb8db0eb7f3d693789` has tree `cdbe70aa2d42c9114d1e32773f8d5f1e8beacfab`.
- The identical tree was preserved remotely at `a464df8a53e4e765243ed198938630fa9b11f9b7` on `codex/v10.2`. Git push has no local credential; the authenticated GitHub connector created the remote checkpoint. Commit IDs differ; source trees were compared exactly.
- Additive 14→15 schema introduces client membership while preserving ambiguous legacy standalone identities. New Person entries are not clients until designated or linked as clients.
- Account/verification/referral and independent legal-content contracts are checked in. Pure Java account-policy smoke checks passed in the preceding execution; no live backend behavior is claimed.
- Original 10.1 APK recovered and SHA-256 checked: `56a4cc3fe16576b0459b47651add0d15d1b537645a7305aec4c00678dc884c30`.
- Existing recovery identity decrypted in memory; certificate SHA-256 verified: `26055f09370416e9cd61c08246b80c57367470cdb6873e282b5b6521cf097202`. No new key created or private material committed. This is prerequisite evidence, not a successful 10.2 signed build.
- Preserved interrupted edits: MainActivity menu/dashboard/SMS; version bump; AutoBackup/Job/manifest. UI and storage integration remain in progress.
- Focused review found workspace authority/path validation and backup relational-validation gaps; these are being corrected before retention or release is accepted.
- Local emulator acceleration unavailable; API 30/35 execution requires CI or another emulator-capable environment. SDK/Gradle setup and V102 upgrade harness are in progress.

## Executed CI and fixes

- Run `37016447235` (remote `b67f483f5ef3725b08986ca26a2b6eee9448ece1`): real APK/instrumentation builds passed. Installed-upgrade and basic storage checks passed before the test DocumentsProvider crashed. Entire workflow failed; no all-tests success.
- Run `37016785544` (remote `7cc9d11029253448030324e30bdf74496cd9cda5`): build passed, workspace test grants failed. Protected test provider and real persistable grant setup corrected in local `1c1af80`.
- ASTRA scoped integration review fixed migration/restore/automatic-backup concurrency using a shared lock (`4bf4f98`), then fixed settings rollback if SQLite transaction completion fails (`432e008`). No simulated disk-full transaction-commit failure was performed.
- Run `37060407779` (remote `002b32bae3ae4f473e45da2c58e9535860b83151`): clean install plus actual target-only uninstall/reinstall, new tree grant, light backup restore and file-byte preservation passed on API 30 and 35. Upgrade fixtures passed, but provider suite setup deleted its granted root and thereby revoked its own grant. Test fixture corrected in `4b4d110`; assertions retained.
- Run `37061122573` (remote `c6891c3de19a8dcb0954b9d034e0cb7931963515`): API 30/V10 full upgrade+storage+UI suite succeeded when last checked; other jobs pending at that check. Query run directly before claiming the final result.
- Final source refinements awaiting the next run: invitation fields use account configuration contract; explicit daily automatic backup/Keystore test. No backend adapter, live verification, entitlement grant, referral lookup success or anti-fraud service exists in this Android release.
- Original-production workflow `v102-production.yml` is prepared but MUST NOT be described as executed until `release-validation/v102.json` is populated from an actually signed candidate and its workflow has passed. Never use a test-signed APK as the real-data update.
