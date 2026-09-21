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

## Tests written, **not yet executed**

- `VokanoDashboardTest`: checks SQL counters against saved records, opens all four dashboard destinations, edits a personal task without duplicating its reminder and checks bundled icon resources.
- `VokanoUpgradeTest` and the PR workflow: first install the actual 9.1 *base-package* release, write an appointment using its instrumentation test, install 10 over it using the same CI debug certificate, then verify the row survives. These are only planned executable checks until GitHub Actions actually runs.
- Existing Java date tests, instrumentation suite, theme snapshots, emulator smoke and new upgrade script are configured in `.github/workflows/build-apk.yml` but have **not run against this v10 commit**.

## Remaining release blockers

The current workspace has a Java runtime but no `javac`, Gradle executable, Android SDK or emulator. Local build/test commands cannot run here. A push of the v10 branch was blocked by automated approval review pending destination verification; repository ownership was subsequently verified, but a direct retry failed because this environment has no GitHub HTTPS credentials. Accordingly there is no v10 CI run, installable APK, emulator smoke result, or demonstrated upgrade yet. Do not label v10 operational or give any older 9.1 APK as its output.

The distributed 9.1 **preview** has application ID `ir.kamranvahdati.lawoffice.preview`; the v10 base release has `ir.kamranvahdati.lawoffice`. Android therefore cannot install the latter *over* preview or transfer its private database automatically. Moreover the existing release build uses the default debug signing key, which is not stable across CI jobs. An operational, future-upgradable release requires a persistent release signing identity outside the repository and a verified secure transfer/restore of data from preview. Never delete the installed 9.1 app until its encrypted backup has been restored and checked in v10. The proposed CI migration test checks same-ID upgrade only; it cannot prove cross-ID preview data transfer.
