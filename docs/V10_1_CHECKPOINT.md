# VOKANO 10.1 continuation checkpoint

Baseline: `e3aa48bec758ef9bce0016fa8618fc48001f6450`, branch `codex/v10-vokano`.
Working branch: `codex/v10.1`.
Status: source checkpoint, NOT release-approved; no final APK built in this continuation.

## Preserved work

The interrupted version bump and additive OfficeV101 database/domain changes were preserved in `8fb5452`. No application data was reset. The source continues from that checkpoint rather than replacing the baseline.

## Source changes

- Central existing IranLocations selectors used for case, person, colleague and appointment locations. Profile selectors retained. Legacy location text is not rewritten by migration.
- Canonical clients IDs reused for natural/legal persons; editable person details, case roles and shared contract references.
- Existing colleague, case and contract services reused; duplicate license/body and contract-number checks; independent/joint modes and creation from contract forms.
- Independent accounting menu, receipts/expenses, accounts, entity references, person cash-flow view, date/person/case/account/type reports and check-status filter. Existing case financial summary and check lifecycle retained. Checks still use the existing case-based check model; independent personal income/expense does not require a case.
- Existing dashboard actions retained; person-type chooser plus receipt and payment shortcuts added.
- Legal-content menu exposed, local search/detail/filter and honest disconnected empty state; no fabricated content or runtime scraping.
- Local collaboration forms with requester/profile locations, work type, specialties, terms, availability, Jalali deadline and proposed amount. No network submission.
- Explicit system-bar/cutout/keyboard padding and backdrop-dependent icon appearance. No brand, theme architecture, application ID or signing-identity change.

## Migration

Schema 13 to 14 adds columns and tables only. Existing person/case/contract IDs, names, dates, amounts, profile and attachment references are retained. New tables are person_roles, contract_parties, financial_accounts, independent_ledger and collaboration_requests. Existing backup engine and format are retained; its already-started table-list integration was preserved and deletion order adjusted to respect the new ledger-to-contract foreign key. No new signing key was created.

## Executed local checks

- Javac parser: six changed/new Java source and instrumentation files parse without syntax errors. This is NOT Android compilation/type validation.
- DeadlineDateSmokeTest: passed.
- InputValidatorsSmokeTest: passed.
- Python validation runner syntax and CI YAML parsing: passed.
- git diff --check: passed.

## Prepared but NOT executed

`.github/workflows/v101.yml` builds the actual baseline application source with fixture-only instrumentation, installs it, records database values and attachment hash, then performs `adb install -r` of 10.1 without uninstall/clear. It compares every prior row/column, preferences and attachment bytes, then runs focused domain and light/dark insets tests on API 30 and 35. Builds in this workflow use one TEST signing identity, not the permanent production key. A successful run therefore does not by itself prove a permanent-signed production upgrade.

## Blocking gates

Git push was rejected by automatic approval review: explicit user permission to transmit the changed source to GitHub was required. Do not bypass the rejection. No push success or CI execution is claimed. Local environment has Java compiler modules but no Android SDK, Gradle installation or emulator.

Remaining: permission to push this exact branch to the existing repository; Android build/type-check and resolve any findings; execute focused tests and upgrade on both APIs; test visible screens/dialogs and bar icons; build using the EXISTING permanent signing identity and verify actual production-signed upgrade/data/attachments; record final release commit and artifact hashes. Production release secrets were not configured by this continuation, and their availability is unverified.
