# VOKANO 10.1 continuation checkpoint

Baseline: `e3aa48bec758ef9bce0016fa8618fc48001f6450`, branch `codex/v10-vokano`.
Working branch: `codex/v10.1`.
Status: release APK built with the existing permanent certificate; API 30 and 35 build, focused tests and original-production upgrade gates passed on 2026-09-26.

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

## Executed Android validation

- Source build: `adfd4a55934d1236548f16da848a532f58702e67`, workflow run `36239452423`: API 30 and 35 both passed. Release and instrumentation APKs compiled successfully.
- Pinned release/provenance commit: `d37f76612abed49fcd1064725cf76f602f892a68`. Repeat scoped run `36239832084`: API 30 and 35 both passed.
- `V101UpgradeTest`: immutable V10 source built with fixture instrumentation, followed by `adb install -r`; every old database row/column, profile value and attachment hash compared. Schema is 14; no demo records inserted.
- `V101DomainTest`: legal-person creation/duplicate rejection, case-role and opposing-party link idempotence, account-linked personal receipt/case expense, financial filters, inconsistent contract-link rejection, local-only collaboration state, and export/import of the new relationships passed.
- UI test: people/accounting/collaboration/legal routes, disconnected legal content, system insets and light/dark system icons passed on both APIs. Screenshots are retained in workflow artifacts. API 30 legacy appearance flags are checked together with actual contrasting status-icon pixels because the controller getter reported zero after capture while the screenshot showed the correct light appearance. API 35 also checks controller appearance.

## Original permanent-signed upgrade

Run `36239832085` of `.github/workflows/v101-production.yml` passed on API 30 and 35. The workflow installed the ORIGINAL V10 production APK, used its original instrumentation to seed synthetic fixtures, then installed the same-key 10.1 APK with `adb install -r`. No uninstall, application data clear or database reset occurs in this upgrade path.

`V101ProductionUpgradeTest` compared all pre-upgrade table rows/columns, the profile, attachment hashes (including the large-file fixture), and opened the preserved PDF/image. The focused V10.1 domain/UI tests then passed against the permanently signed package. These are controlled fixture upgrades, not tests against a user's personal device/database.

Original APK SHA-256: `022652ff25c369022528e45f50bf61719bea1f8b772ff7fd3f74930881ac138d`.

10.1 APK SHA-256: `56a4cc3fe16576b0459b47651add0d15d1b537645a7305aec4c00678dc884c30`.

Existing certificate SHA-256: `26055f09370416e9cd61c08246b80c57367470cdb6873e282b5b6521cf097202`.

The delivered APK matches the permanently signed APK tested on both APIs byte-for-byte. Signing changed only signature material; all non-META-INF ZIP entries were compared before/after signing. No signing identity, application ID or release signing configuration was changed. Temporary decrypted signing files were removed; no private signing material was uploaded to the repository.

## Deliberate boundaries

Legal content remains a disconnected local foundation pending the real kamranvahdati.ir API. Lawyer cooperation remains local-only with no public backend or live network requests. No fabricated legal content is bundled. Existing case-based checks remain case-based; independent personal income and expenses do not require a case.

The report describes the automated coverage actually executed; it does not claim an exhaustive manual walkthrough of every form on physical devices. Final documentation-only commits do not change the tested APK/source.
