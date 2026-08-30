# Plan: Smart, Grouped Local Notifications for Foster

> Status: **APPROVED WITH CHANGES** — review feedback incorporated (v2).
> Companion doc: `docs/notifications-current-state.md` (audit of today's system)
> Scope: local/on-device only. No FCM/APNs, no server push, no backend changes.

---

## 1. Goal

Replace the current 1-contact-to-1-notification model with a **grouped, priority-aware,
per-day digest model**, surface the two currently-silent data types (custom reminders, missed
check-ins), and bundle the platform hygiene fixes (reboot/update/timezone resilience, tap deep
links, exact-alarm strategy, iOS localization).

## 2. Current system in one paragraph

`HomeViewModel.reconcileReminders` cancels all alarms and re-schedules one alarm per contact
(soonest 64) via `ReminderScheduler` (expect/actual: Android `AlarmManager` + `CheckInReminderReceiver`,
iOS `UNCalendarNotificationTrigger`). Fire time = `next_check_in_date` + `reminder_time`.
Custom reminders and missed check-ins are computed/stored but never notified. Tapping a
notification does nothing; alarms die on reboot; exact-alarm permission is never requested;
iOS copy is untranslated.

## 3. Target design

### 3.1 Unified due-item model (new, commonMain)

```kotlin
sealed interface DueItem {
    val id: String                 // stable item id
    val contactId: String?         // null for standalone custom reminders
    val headline: String           // name or reminder title
    val fireAtEpochMillis: Long
    val priority: Int              // see 3.3
}
// Concrete types: CheckInDue, OverdueCheckIn(daysOverdue), CustomReminderDue(title, recurrence)
```

### 3.2 Two granularities, two mechanisms (D1 — resolved)

Items split by time granularity:

- **Day-granular digest** (check-ins due, overdue check-ins): all of a day's items are grouped
  into ONE notification fired at a **fixed daily hour** — the profile `default_reminder_time`.
  Predictable, and what a "digest" implies.
- **Time-specific custom reminders** ("take medication at 8pm"): semantically bound to their clock
  time. They fire **standalone at their own time** and are never folded into the digest. This
  avoids the regression where an early check-in would drag an 8pm reminder to the morning.

### 3.3 Delivered-days machinery (D1b — the idempotency primitive)

A persisted `deliveredDays: Set<epochDay>` lives in the DataStore snapshot. Reconcile rule for a
day-digest, with `fireTime = today@digestHour`, `quietCutoff` (e.g. 21:00), `δ` (few-minute
coalescing delay):

| Condition | Behavior |
|---|---|
| `dayKey ∈ deliveredDays` | Skip. A day is nudged **at most once**, no matter how many reconciles run. |
| `fireTime > now` | Schedule normally at `fireTime`. |
| `fireTime ≤ now` and `now ≤ quietCutoff` | **Foreground reconcile** (app start/foreground/check-in): user is in the app — surface items in-app and mark the day delivered; no near-instant buzz. **Background/boot path** (re-arm from snapshot with a past-time day): schedule a **coalesced catch-up** at `now + δ`. |
| `fireTime ≤ now` and `now > quietCutoff` | **No buzz tonight.** Items stay in the plan; still-due ones re-bucket into tomorrow's digest at tomorrow's digest hour. Bounded overnight deferral (~12h), never silent. |

Supporting rules:

- **Prune `deliveredDays`** of past days on every reconcile (bounded growth; an item still
  outstanding on a later calendar day gets a fresh nudge via its new `dayKey`).
- **Last item cleared ⇒ `cancelDay`** (D3 correctness rule): if a check-in removes the final item
  for a pending day, cancel the alarm AND drop `dayKey` from the pending/delivered sets before a
  catch-up can fire (e.g. check-in at 8:55pm cancels the 9pm catch-up). No empty "0 people" digest.
  Explicit, tested rule.
- Same catch-up/roll-over machinery covers **time-specific custom reminders whose own time has
  already elapsed at reconcile** — whether a missed clock-time reminder should fire late at all
  remains a product call (default: catch-up within the window, roll past the cutoff).

### 3.4 Priority scoring (copy + channel selection only)

| Type | Base score | Modifier |
|---|---|---|
| Custom reminder | 200 | — |
| Check-in due today | 100 | — |
| Overdue check-in | 300 | +10 per day overdue (cap +100) |

- **Birthday tier dropped for v1 (D2 → a′)**: no real birthday data source exists
  (`recurrence = 'annually'` also covers "renew car registration" — labeling every annual reminder
  a birthday would make the tier meaningless). Annual reminders stay `CustomReminderDue` at 200.
  Revisit when real birthday data exists (`kind` tag on `custom_reminders` or a
  `contacts.birth_date` column).
- Highest-priority item headlines the grouped notification; its type selects the channel/category.

### 3.5 Surfacing the silent types

- **Custom reminders**: new client-side occurrence expansion (`recurrence` + `date_epoch_millis`
  → next fire datetime), mirroring the server's `next_scheduled_check_in_date` logic. Requires a
  new owner-scoped "all custom reminders" query (current `getReminders` is per-contact). Fired
  standalone per §3.2, with the §3.3 elapsed-time rules.
- **Missed/overdue check-ins**: already fetched via `sync_missed_check_ins` into `HomeSnapshot`;
  they join **today's** digest bucket as `OverdueCheckIn` items.
  **Fire-time synthesis fix:** an overdue item's natural due time is in the past — it gets a
  **synthesized fire time of today@digestHour**, never its real past time (prevents
  fire-instantly-on-schedule or stale-drop). An overdue-only day still schedules a digest.

### 3.6 Channels / categories (prep for per-type mutes)

- Android channels created up front: `check_ins`, `custom_reminders`, `missed_check_ins`
  (birthday channel deferred with the tier). Grouped digests post in the highest-priority item's
  channel.
- iOS `UNNotificationCategory` set with matching identifiers.
- No settings UI yet — infrastructure only.

### 3.7 Cap fix

`MaxPendingReminders = 64` becomes **max distinct scheduled days** (`MaxScheduledDays`).
Note: on iOS, pending local notifications are hard-capped at 64 by the OS — there, "scheduled
days" ≈ "pending notifications," so 64 remains a **hard platform ceiling**, not a product choice.
Day-grouping keeps us comfortably under it (days ≤ contacts).

## 4. Bundled platform fixes

| Fix | Approach |
|---|---|
| Reboot / update / timezone resilience (Android) | One receiver handling `RECEIVE_BOOT_COMPLETED`, `MY_PACKAGE_REPLACED` (app updates also wipe alarms), and `ACTION_TIMEZONE_CHANGED` / `ACTION_TIME_CHANGED` (day buckets are local-calendar boundaries — a timezone shift moves every fire time). All paths re-arm from the persisted DataStore day-plan snapshot with no network/auth; timezone events trigger a full re-reconcile. Timezone handling matters more than reboot for a travel-heavy "keep in touch" app. |
| Tap deep link (both platforms, **cold start included**) | Android: `setContentIntent` → MainActivity extras (`dayKey` for digests, `contactId` for standalone custom reminders); read extras in `onCreate` AND `onNewIntent`, buffer, replay once the nav host is ready. iOS: `UNUserNotificationCenterDelegate` `didReceive` may fire before the UI exists — buffer and drain when the navigation graph is ready. Cold start is the *common* case and bites both platforms. |
| Exact-alarm strategy (Android) | **Re-evaluate before building any prompt flow:** (1) a check-in/reminder app plausibly qualifies for `USE_EXACT_ALARM` (Android 13+, granted by default, Play-policy-gated) which deletes the banner/dialog UX entirely; (2) with the fixed digest hour, minute-precision may not matter — an inexact windowed alarm could suffice. Whichever wins: use `setExactAndAllowWhileIdle` so alarms fire during Doze (plain `setExact` gets deferred). Decision before P2 starts. |
| iOS localization | Notification copy moves to localized strings (EN/ES) read via `NSBundle` in the iOS actual; keys shared with Android's `strings.xml`. **Plurals are per-platform**: "N people to check in with today" needs Android `<plurals>` + iOS `.stringsdict` — share keys, not manually-substituted strings, or EN/ES plural rules break. |

## 5. Grouped day view (tap target)

New `@Serializable Screen.DayAgenda(day: String)` in the existing custom nav stack
(`shared/.../navigation/Screen.kt` + `FosterNavHost` + `OnboardingApp` dispatch):

- Lists that day's items: due check-ins (with Check-In button), overdue contacts, and links into
  custom reminders — reusing existing row components where possible.
- Standalone custom-reminder notifications deep-link to the owning contact profile (or the
  reminder context) instead.

## 6. Work breakdown (phased)

| Phase | Work | Size | Files (primary) |
|---|---|---|---|
| P1 | DueItem model, day bucketing, digest-hour + delivered-days machinery, priority scoring, custom-reminder expansion, overdue fire-time synthesis, cap change — pure commonMain, unit-tested | M | `home/domain/ReminderSchedule.kt`, `home/domain/Contact.kt`, new `DueItem.kt`, `SupabaseContactDataSource.kt` (fetch-all custom reminders) |
| P2 | Scheduler API rework + Android actual: channels, grouped receiver copy, content intent, boot/update/timezone receivers + DataStore snapshot (written **before** cancelling — atomicity), exact-alarm decision implementation | L | `shared/**/notifications/*`, `androidApp/AndroidManifest.xml`, `MainActivity.kt` |
| P3 | iOS actual: categories, localized copy + plurals (`.stringsdict`), notification delegate + buffered tap routing bridge | M | `shared/src/iosMain/.../ReminderScheduler.ios.kt`, `iosApp/*.swift` |
| P4 | `DayAgenda` screen + navigation + cold-start deep-link wiring (both platforms) | M | `shared/.../navigation/*`, `onboarding/OnboardingApp.kt`, new screen in `home/` |
| P5 | QA matrix + polish | S | — |

Notes folded in from review:

- **Reconcile atomicity**: write the DataStore snapshot *before* cancelling current alarms
  (a mid-reconcile process kill must not leave zero alarms); consider day-diffing instead of
  cancel-all/re-arm to cut churn (non-blocking).
- **Priority collision note**: with the birthday tier dropped, the overdue cap (+100 → 400) no
  longer ties anything; fire-time remains the deterministic tiebreak.
- Optional Android-only nicety (not required): Android can re-post a delivered notification by id,
  so grouped counts *could* be refreshed on check-in — iOS can't; asymmetry documented.

Out of scope (unchanged): onboarding permission flow, `notification_settings` table, Settings
deep-link behavior.

## 7. Resolved decisions

- **D1**: Fixed daily digest hour (profile `default_reminder_time`) for day-granular items;
  time-specific custom reminders stay standalone at their own clock time. Earliest-item-time
  rejected (it regresses time-bound reminders).
- **D2**: (a′) — no birthday tier in v1; no invented semantics. Revisit with real data.
- **D3**: Static copy accepted, with the mandatory **last-item-clears ⇒ `cancelDay` + set-drop**
  rule (tested).
- **Exact alarm**: decide `USE_EXACT_ALARM` vs inexact digest vs request flow **before P2**.

## 8. Risks

- **iOS tap routing** remains the riskiest piece (delegate + Kotlin bridge + cold-start
  buffering); P3/P4 ordering contains it, and the same cold-start care applies to Android.
- **Boot/update/timezone snapshot drift**: the snapshot reflects the last reconcile; acceptable
  (strictly better than today, where reboot loses everything).
- **Static digest copy** within a day: accepted per D3 with the empty-digest cancellation rule.
- **Alarm churn**: reconcile still re-arms on every snapshot, but volume drops sharply
  (≤64 alarms → ≤64 days, typically far fewer); day-diffing can reduce it further.

## 9. Test plan

- Unit (commonTest/androidApp): bucketing across DST/timezone-shift boundaries; digest-hour +
  delivered-days rules table (§3.3) including quiet-cutoff rollover and foreground no-buzz;
  scoring order; custom-reminder recurrence expansion (each recurrence value) + elapsed-time
  catch-up; overdue fire-time synthesis; cap = distinct days; single-vs-group copy selection;
  last-item-clears ⇒ day cancelled.
- Receiver tests: grouped copy with plurals, content-intent extras, boot/update/timezone re-arm
  from snapshot, snapshot-written-before-cancel ordering.
- Manual matrix: reboot → alarms restored; app update → alarms restored; timezone change →
  re-bucketed; notifications off at OS level; >64 due days; exact-alarm denied (Android 14);
  cold-start tap → DayAgenda / profile; EN/ES locales with plural cases (1, 2, many); overdue-only
  day schedules a digest.

---

*Companion audit: `docs/notifications-current-state.md`. Approved with changes — implementation
starts at P1; exact-alarm strategy decision lands before P2.*
