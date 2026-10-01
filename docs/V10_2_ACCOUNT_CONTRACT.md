# Android 10.2 account boundary (proposed v1 contract)

Status: **client foundation; NOT a live integration or a local licensing system**.
The inspected Platform foundation has licensing claims/activation/transfer and professional
verification type contracts. This does not establish deployed endpoints, credentials, payment
settlement, referral services, official registry APIs, or a production signing adapter.
Platform sources were read only. No API URL is invented or called.

Android `ProfessionalAccount.unavailable()` is the only operational snapshot factory.
It grants neither professional collaboration nor a trial/license. Existing local office data
must remain accessible independently of backend availability. UI displays unavailable rather
than a fictitious remaining-days count. `ProfessionalAccountApi.Unavailable` explicitly fails
all requests. The interface and wire DTOs describe integration work, not a completed transport.
A reviewed authenticated transport must implement the typed success callbacks when
the Platform capability exists. Mutable wire DTOs must never serve as authorization proof.

## Required authenticated account service

* Return stable account ID, entitlement ID, license state/type/remaining days, server time,
  revision, trial start/end, expiry; verified source, verification timestamp and latest
  professional status. Verification states: unverified, pending, verified, rejected,
  suspended-or-invalid. Existing Platform `expired` maps to suspended-or-invalid.
* Verification request carries first/last name, issuing professional body, license number,
  claimed license status. Account owner cannot write a verification decision. Official authorized
  API review or manual authorized reviewer checks official evidence; no scraping/CAPTCHA bypass.
  Only authenticated, current verified identity permits collaboration/public lawyer identity.
* Account subscription survives a device transfer. Server checks source/target installation,
  expected revision and idempotency key and atomically deactivates source/activates target.
  Device attestation/activation proof follows the existing Platform licensing contract. No APK
  private key, hard-coded entitlement or shared signing secret is introduced.
* Versioned terms/privacy acceptance: local version and timestamp are pending evidence only.
  Server receipt includes authenticated account ID, versions, accepted-at and server-recorded-at.
  Do not equate local acceptance with server acknowledgment.

## Referral/Trial server rules (supersede older product rules)

Base trial is **7 days**. A valid, explicitly confirmed referral before onboarding completes
changes the trial end to **trial start + 30 days**, never +37 or confirmation-time +30.
Optional code means registration without any referral is supported. Campaign prefill must not
silently confirm a referrer.

Server issues each verified lawyer a permanent random `V` + exactly six ASCII digits, with a
unique database constraint and collision retries. No personal/geographic input; account ID
is independent. Reserved vanity `VOKANO` resolves to **کامران وحدتی** through the server.
Even this known vanity must be looked up, not accepted via an offline exception.

Lookup returns canonical code, owner ID, owner display name, expiring opaque lookup token.
UI shows the returned owner name before an explicit confirmation. Confirmation binds token,
current authenticated user and code transactionally before onboarding ends. One referrer/user;
reject self referral, duplicate accounts and replay. Never use client code-format validity as
proof that a code exists or is eligible.

Success requires a genuine new user, valid direct referral, completed onboarding/trial and
qualifying subscription payment settled by backend after any refund/cancel window. Server
configuration controls qualifying products/prices. Payment webhook is verified; reward ledger
has unique referred-user/milestone keys and atomic idempotent processing. No multilevel credit.
Sent/Registered/Trial/Paid/Successful are presentation states from actual server events. A
client opening SMS cannot prove Sent or Paid. Display earned credit only from ledger data.

| Successful direct referrals | Total reward months | Increment at milestone |
|---|---:|---:|
| 1 | 1 | 1 |
| 3 | 6 | 5 |
| 5 | 12 | 6 |

Counts above 5 retain 12 months in this version. Android milestone helpers are explanatory/test
expectations only and cannot award time. Server must enforce these same totals and reversals
according to payment policy. The former 2-month/referral and 10/24 model is not implemented.

## Integration instructions for UI

Use `ProfessionalAccount.unavailable().displayText()` for account status and
`canUseProfessionalCollaboration()` as the current fail-closed gate. Do not hide/delete local
legacy collaboration drafts; prevent submission and professional presentation. Display 7/30
and milestone rules as help/policy, never as an activated entitlement. Keep referral confirmation,
transfer and verification actions unavailable with the explicit dependency explanation.
No fabricated referral code, owner lookup success, download URL or license countdown.
Introduction SMS needs server-issued code/name and configured official invitation URL;
without these, show that invitation is unavailable rather than generate a misleading link.

Device movement must use the Platform's atomic transfer command: first verify ownership and
activation proof for target, check revision/idempotency, deactivate source and activate target
in one server transaction. A failure leaves the prior valid activation intact. Do not independently
issue client-side deactivate then activate calls that could strand the subscription. If future
endpoints are separate, server orchestrates the transaction and returns confirmed activation
states for both installations. Android has no authority to mark either step successful offline.

`LegalContentApi` is the independent versioned v1 search/get contract for laws, articles,
unification rulings and advisory opinions. It carries category, cursor, source date, official
source URL and content revision. Entries/pages are immutable. HTTPS is only transport shape
validation, not proof of official authenticity; the future provider must verify source eligibility
and content before returning success. Current unavailable adapter never returns invented content.
No personal website frontend/database or runtime scraper is coupled to Android.
