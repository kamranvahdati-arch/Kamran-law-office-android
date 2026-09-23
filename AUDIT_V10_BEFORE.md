# Version 10: source baseline before changes

Source: local `codex/v091-requirements-completion` at `e538d37`; remote v9.1 tip `8343781a33f8a31040fdefd07a371b1b837e0770` has the same content. The separate `.preview` package is the downloadable v9.1 APK. All findings below are from the actual Android sources.

| Requirement | Baseline | Evidence | Action |
| --- | --- | --- | --- |
| VOKANO brand and launcher | Missing | `app_icon.xml`, `strings.xml`, `AppTheme.java` still use Vakil Man palette/name | Package the supplied approved assets; apply central light/dark colors, app name and splash. |
| Header and lawyer identity | Partial | `MainActivity.shell/dashboard`: header has `+`, institution logo in header, old title; greeting/photo and Jalali date already work | Remove redundant `+`; show brand at top and institution logo next to lawyer photo without changing profile navigation. |
| 12 dashboard cards | Partial | Eight working counters in `dashboard`; appointment/deadline/personal data exists but no four dedicated cards | Add filtered counters and routes into existing records/forms. |
| Hearings and consultation forms | Complete data model, partial navigation | `OfficeDb.appointments`, `MainActivity.addAppointment/appointmentList` | Keep storage and reminders; add focused lists and preselected form kinds. |
| Legal deadlines | Complete data model, partial navigation | `OfficeDb.deadlines`, `MainActivity.deadlineList` and schedule actions | Preserve structure; expose global card and quick action. |
| Personal tasks | Partial | Task kind `برنامه شخصی`, `addLinkedTask`; `tasks()` shows only open tasks mixed with work | Add filtered list and a preselected form; retain done/edit flows and stored rows. |
| Quick actions | Partial | Client/case/task/visit only | Add hearing and deadline actions, refresh dashboard after save. |
| Jalali calendar | Partial | `JalaliDate` and calendar/list switch exist; day view excludes deadlines and general events in list mode | Keep Jalali representation, show aggregated daily records and move below quick actions. |
| Daily agenda/priority | Partial | `dayAgenda`, deadline cards and staged reminders already exist; dashboard sections appear before statistics/calendar | Reorder and expose today/near/overdue after calendar, preserving all cards. |
| Upgrade and release signing | Risk | DB schema v13; release signed with default debug key; v9.1 distributed `.preview` suffix | No destructive schema change. Verify same package/signature upgrade on emulator if possible; do not claim compatibility across suffix/signature. |

Scope: Android only. Existing SQLCipher DB name and schema, backup/restore, feature flags, commercial backend placeholders, and navigation model remain intact.
