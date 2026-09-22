# VOKANO 10: implementation and verification checkpoint

Baseline was `e538d37` (local v9.1 source), with matching remote v9.1 tree on `codex/v091-deadlines-audit` at `8343781a33f8a31040fdefd07a371b1b837e0770`. See `AUDIT_V10_BEFORE.md` for feature-by-feature findings.

## Implemented in the current source

- Version name `10.0`, code `12`, branded application label; approved VOKANO JPEGs copied byte-for-byte from the user's uploaded images into `drawable-nodpi` (light/dark icon and wordmark). Professional institution logos remain bundled and are selected from the profile.
- Central VOKANO light/dark palette; formal, turquoise and black/gold themes retain their own Android dialog control palettes. Cards, actions, calendar, text and date/time fields draw from the active theme. The status bar remains dark with light icons; navigation bar/insets remain managed in the existing shell.
- Startup branding is displayed while app initialization completes, without a fixed timer or artificial wait. Header has VOKANO brand/menu and no redundant plus button. Lawyer greeting, photo, institution logo and full Jalali date are below it; neither photo nor logo navigates to profile.
- All twelve dashboard cards read the existing encrypted database, and four newly surfaced cards navigate to filtered hearing, deadline, personal task and consultation lists. Six quick actions open the existing forms with hearing/meeting type preselected. The database schema stays at v13; no migration or reset is introduced.
- Calendar dots and seven-day list include deadlines. Daily agenda combines appointments, legal deadlines and tasks; overdue deadlines and uncompleted appointments remain visible. Existing urgency bands and reminder rules are retained.
- Personal task list now includes completed records. Editing a task keeps the row identity and shifts its existing reminder timestamps when the date/time changes, within one transaction.

## Checks completed locally

- `git diff --check`: passed.
- Compared the original approved image files with copied assets byte-for-byte (verify with `cmp` on the current checkout); no image regeneration or internet download was used.
- Compared the remote repository metadata to the configured Git remote: both identify `kamranvahdati-arch/Kamran-law-office-android`. A scan of newly changed files found no signing key, authentication token or user dataset. Fixture phone in the added tests is synthetic.
- Inspected CI screenshots of the light and dark dashboards and dark settings on a 640-pixel viewport. The app view screenshots do not include Android's system bars; their insets cannot be proven visually from those screenshots.

## Executed tests (GitHub Actions run 57)

- [Run 57](https://github.com/kamranvahdati-arch/Kamran-law-office-android/actions/runs/35650620522) completed successfully on Android API 30 and 35 for commit `c8bfcddcf80c1cda445f888939a36ffa5e4bcaf3`. Both jobs built release and preview APKs and passed `DeadlineDateSmokeTest` and `InputValidatorsSmokeTest`.
- Each emulator ran 19 instrumentation tests with 0 failures and 1 intentional skip: `VokanoUpgradeTest` only runs when an actual earlier APK is installed. This suite includes dashboard navigation/counters, personal task edit/reminder retention, encrypted migration, finance, restore, notification delivery, and profile/theme checks.
- On each emulator the UI smoke test passed (1 test) and captured 10 app-view screenshots of dashboard and settings across five themes. The preview APK was installed and launched, and its process was observed.
- The upgrade stage installed the real 9.1 *base-package* release, seeded an appointment through its own test (1 passed), installed 10 over it without uninstalling, and verified the persisted record and database v13 (1 passed). The emulator was kept alive throughout this sequence.
- An earlier run 56 failed only because its upgrade command was placed after the emulator action had shut down the device. The workflow ordering was corrected in run 57; do not count run 56 as a passing run.

## Remaining release blockers

The current workspace has no `javac`, Gradle executable, Android SDK or emulator, so native build and smoke results above come from the actual GitHub Actions run. A future final source commit that changes implementation or this report must be rebuilt and verified before its APK can be claimed as the artifact of that final commit.

The distributed 9.1 **preview** has application ID `ir.kamranvahdati.lawoffice.preview`; the v10 base release has `ir.kamranvahdati.lawoffice`. Android therefore cannot install the latter *over* preview or transfer its private database automatically. Moreover the existing release build uses the default debug signing key, which is not stable across CI jobs. A reliably future-upgradable distribution requires a persistent signing identity outside the repository and a verified secure transfer/restore of data from preview. Never delete the installed 9.1 preview until its encrypted backup has been restored and checked in v10. The passed CI migration test checks same-ID upgrade only; it cannot prove cross-ID preview data transfer. These APKs are debug-signed test releases, not production-signed distributions.

## Additional transfer gate being implemented

The 9.1 preview already exports an authenticated `.klo` file (`KLO-BUNDLE-1`, `KLO-2` database plus non-sensitive profile settings). The v10 base package already imports that format and validates relational integrity in one transaction. A separate emulator test is being added to export from an installed 9.1 *preview* APK and import into the installed v10 *base* APK. Until the workflow records a passing run for that new test, cross-package transfer remains unverified. Neither version bundles photo or attachment bytes in `.klo`; those files still require separate preservation. The signing caveat above remains even if the backup-transfer test passes.
