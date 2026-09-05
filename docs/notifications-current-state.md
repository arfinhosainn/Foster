# Foster Notifications — Current State Report

> Audit of the notification system as of `ui-fix/app-theme-issue` (Aug 2026).
> Purpose: baseline for planning changes to the notification experience.

## Overview

Notifications are **local-only** — no FCM/APNs, no server push, no server-side scheduling component.
There is exactly **one type of notification**: a per-contact "time to check in" reminder, derived from
`contacts.next_check_in_date` + `contacts.reminder_time` in the device's timezone.

## Delivery mechanism

| Platform | Mechanism | Details |
|---|---|---|
| Android | `AlarmManager.setExactAndAllowWhileIdle` → `CheckInReminderReceiver` broadcast | Falls back to inexact alarm when `canScheduleExactAlarms()` is false (Android 12/12L; `USE_EXACT_ALARM` is auto-granted on 13+, `SCHEDULE_EXACT_ALARM` is not declared in the manifest and is never requested at runtime). High-importance channel `check_in_reminders`, created lazily at fire time. Copy: "Check in with {name}" / "It's time to check in on {name}" (localized EN/ES). |
| iOS | `UNUserNotificationCenter` + one-shot `UNCalendarNotificationTrigger`, request id = contactId | Same copy but hardcoded English (not localized). Permission requested only from the onboarding button. No `UNUserNotificationCenterDelegate` is installed in the Swift host. |

Key files:

- `shared/src/commonMain/kotlin/app/usefoster/shared/notifications/ReminderScheduler.kt` (expect; cap = 64 pending)
- `shared/src/androidMain/.../ReminderScheduler.android.kt`, `CheckInReminderReceiver.kt`
- `shared/src/iosMain/.../ReminderScheduler.ios.kt`
- `home/src/commonMain/kotlin/app/usefoster/home/domain/ReminderSchedule.kt` (fire-time math)

## When reminders are scheduled (3 scenarios)

1. **Home screen load / foreground return / after any check-in** — bulk reconcile
   (`HomeViewModel.reconcileReminders`): cancel ALL alarms, then re-schedule the soonest **64**
   contacts from `computeReminderPlans(contacts)`. Runs on every repository snapshot emission.
2. **Contact created or edited** (Add Contact flow) — new contacts get "today at reminder_time, else
   tomorrow"; edits reschedule via `nextReminder(now) ?: initialReminder(now)`.
3. **Check-in logged from the Contact Profile screen** — cancel + schedule the next occurrence.

Fire time = `next_check_in_date` at `reminder_time`, resolved in the device-local timezone at
scheduling time (DST-safe via kotlinx-datetime).

Onboarding completion does **not** schedule anything directly; the first real scheduling happens in
the Home / Add Contact flows above.

## Permission flow

- Asked **once** during onboarding (step 8 of 10, "Turn on Notification" button).
  - Android 13+: `POST_NOTIFICATIONS` runtime dialog (only request site in the app).
  - Android 12−: deep-link to OS notification settings, re-checked on return.
  - iOS: `UNUserNotificationCenter.requestAuthorization` on explicit tap only.
- Result is stored in `notification_settings` (`permission_asked`, `permission_granted`) via the
  `complete_onboarding` RPC.
- Denying does not block onboarding; recovery happens later via Settings.
- **The table is write-only telemetry** — no client code ever reads or updates it after onboarding.

## Settings screen

- The "Notifications" row is **not an in-app toggle** — it deep-links to the OS notification settings
  page for the app (`ACTION_APP_NOTIFICATION_SETTINGS` / `UIApplicationOpenSettingsURLString`).
- Known bug: the On/Off label only refreshes when the screen is recreated (`LaunchedEffect(Unit)`),
  not when returning from OS settings.
- Turning notifications off at the OS level leaves stale alarms armed (the receiver silently drops
  them at fire time); service resumes only at the next Home-driven reconcile.

## What does NOT trigger notifications

- **Custom reminders** (`custom_reminders` table: title, recurrence, date) — stored in DB and shown
  in-app only. No code ever expands them into OS notifications.
- **Missed check-ins** — computed server-side (`sync_missed_check_ins` RPC), surfaced in-app on the
  Home timeline only. No notification is generated.

## Known gaps / issues (candidate edit list)

1. **No reboot resilience (Android)** — alarms are lost on reboot (no `RECEIVE_BOOT_COMPLETED`
   receiver); reminders silently vanish until the app is reopened and Home reconciles.
2. **Notification tap is inert** — no `setContentIntent` / deep link into the contact profile.
3. **64-reminder cap applied on Android too** — the cap exists for the iOS pending-notification
   limit; Android users with more than 64 due contacts silently lose the rest.
4. **Exact-alarm access is never requested at runtime** — on Android 13+ `USE_EXACT_ALARM` is auto-granted (declared, can't be revoked), and on Android 12/12L users silently fall back to inexact delivery; no `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` flow exists.
5. **iOS notification copy is not localized** (Android supports EN/ES).
6. **iOS foreground presentation undelegated** — no `UNUserNotificationCenterDelegate` in the Swift
   host, so in-app banner behavior is left to OS defaults.
7. **`notification_settings` never read/updated after onboarding** — OS permission changes are never
   reflected server-side; no analytics/segmentation possible off this table.
8. **Settings surface is passive** — stale On/Off label, no in-app toggle, no cancel-all /
   reschedule-all control exposed to the user.
9. Minor: generic `ic_dialog_info` as the notification small icon (no branded drawable);
   `contactId.hashCode()` doubles as alarm request code and notification id (theoretical collision);
   dead "skip" path in the onboarding notification screen; step indicator shows 7 steps vs the
   actual 10-step enum; divergent fallback logic between the two reschedulers
   (`AddContactViewModel` falls back to `initialReminder`, `ContactProfileViewModel` does not).

## One-line summary

One local per-contact daily check-in reminder, scheduled in 3 scenarios, permission asked once
during onboarding, Settings only deep-links to the OS — custom reminders and missed check-ins have
zero notification footprint.
