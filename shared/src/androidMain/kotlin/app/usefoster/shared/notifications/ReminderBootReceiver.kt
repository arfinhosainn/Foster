package app.usefoster.shared.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * Re-arms notification alarms from the persisted plan snapshot after the three
 * events that wipe `AlarmManager` state or move day boundaries:
 *
 *  - [Intent.ACTION_BOOT_COMPLETED] — device reboot
 *  - [Intent.ACTION_MY_PACKAGE_REPLACED] — app update
 *  - [Intent.ACTION_TIMEZONE_CHANGED] / [Intent.ACTION_TIME_CHANGED] — day
 *    buckets are local-calendar boundaries, so a clock/timezone shift moves
 *    every fire time and warrants a full re-reconcile
 *
 * Runs entirely from the persisted snapshot: no network, no auth. Elapsed
 * digests follow the BACKGROUND decision table (coalesced catch-up within the
 * quiet window, overnight rollover past it).
 */
class ReminderBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            -> Unit

            else -> return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                ReminderReArm.reArmFromSnapshot(
                    nowEpochMillis = Clock.System.now().toEpochMilliseconds(),
                    timeZoneTodayKey = today.toEpochDays(),
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
