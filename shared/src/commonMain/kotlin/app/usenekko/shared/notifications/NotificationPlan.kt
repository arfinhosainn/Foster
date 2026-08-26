package app.usenekko.shared.notifications

import kotlinx.serialization.Serializable

/** Notification categories, one Android channel / iOS category per value. */
object NotificationCategories {
    const val CHECK_INS = "check_ins"
    const val CUSTOM_REMINDERS = "custom_reminders"
    const val MISSED_CHECK_INS = "missed_check_ins"
}

/**
 * One grouped day-digest alarm: every day-granular item due on [dayKey]
 * (epoch day) collapses into a single notification firing at [fireAtEpochMillis].
 *
 * Copy is baked at schedule time (local notifications are immutable):
 * [itemCount] == 1 renders the per-item copy ("Check in with {headline}"),
 * 2+ renders the grouped copy ("{headline} + N others" / plural digest).
 * [category] selects the Android channel / iOS category; it is the highest
 * priority item's category. [singleTargetId] is set only when [itemCount] == 1
 * and is the deep-link target (contact id or custom reminder id).
 */
@Serializable
data class DayPlan(
    val dayKey: Long,
    val fireAtEpochMillis: Long,
    val itemCount: Int,
    val headline: String,
    val category: String,
    val singleTargetId: String? = null,
    val singleIsContact: Boolean = true,
)

/**
 * A standalone (non-digest) notification for a time-specific item — currently
 * custom reminders, which fire at their own clock time and never fold into the
 * day digest. [key] is a stable unique id (reminder id + occurrence date) used
 * as the alarm/notification identity so re-reconciles never duplicate.
 */
@Serializable
data class StandalonePlan(
    val key: String,
    val fireAtEpochMillis: Long,
    val title: String,
    val category: String,
    val targetId: String?,
    val targetIsContact: Boolean,
)

/**
 * Persisted notification scheduling state. Written on every reconcile (BEFORE
 * cancelling current alarms, so a mid-reconcile process kill never leaves zero
 * alarms) and read by the Android boot/update/timezone receiver to re-arm
 * alarms without network or auth.
 */
@Serializable
data class NotificationPlanState(
    val deliveredDays: Set<Long> = emptySet(),
    val deliveredKeys: Set<String> = emptySet(),
    val pendingDays: Set<Long> = emptySet(),
    val pendingKeys: Set<String> = emptySet(),
    val days: List<DayPlan> = emptyList(),
    val standalones: List<StandalonePlan> = emptyList(),
)

/** Platform persistence for [NotificationPlanState] (DataStore / NSUserDefaults). */
interface NotificationPlanStore {
    suspend fun load(): NotificationPlanState
    suspend fun save(state: NotificationPlanState)
}

/** Where a reconcile was triggered from — drives elapsed-digest behavior. */
enum class ReconcileSource {
    /** App is open (app start, foreground return, in-app check-in). */
    FOREGROUND,

    /** Re-arm from boot/update/timezone receiver; the app is not visible. */
    BACKGROUND,
}

/**
 * Deep-link target extracted from a notification tap. Posted by the platform
 * tap handlers into [NotificationTapRouter] and consumed once the navigation
 * graph is ready (cold start included).
 */
@Serializable
data class NotificationTarget(
    val dayKey: Long? = null,
    val contactId: String? = null,
)

/** Bridge notification taps into the app's navigation, cold-start safe. */
object NotificationTapRouter {
    private val _pending = kotlinx.coroutines.flow.MutableStateFlow<NotificationTarget?>(null)
    val pending: kotlinx.coroutines.flow.StateFlow<NotificationTarget?> = _pending

    fun post(target: NotificationTarget) {
        _pending.value = target
    }

    fun consume(): NotificationTarget? {
        val target = _pending.value
        _pending.value = null
        return target
    }
}

/**
 * Scheduling ceiling on pending notifications. On iOS the OS hard-caps pending
 * local notifications at 64, so the plan budget is COMBINED: digest days are
 * scheduled first (one notification per day), and standalone custom reminders
 * fill whatever budget remains (see buildDuePlan). Without the combined math,
 * recurring standalones silently overflow the cap and iOS drops the oldest.
 */
const val MaxScheduledDays = 64

/** Coalescing delay for catch-up alarms (merges rapid reconciles into one buzz). */
const val CatchUpDelayMillis = 2 * 60 * 1000L

/** Past this local hour an elapsed digest rolls to tomorrow instead of buzzing. */
const val QuietCutoffHour = 21

/**
 * The elapsed-digest decision table (plan §3.3). A day is buzzed at most once:
 * delivered days are skipped; a not-yet-delivered digest whose hour already
 * passed surfaces in-app on a foreground reconcile (the user is literally in
 * the app) but coalesces into a near-term catch-up alarm on the background
 * re-arm path; past the quiet cutoff it rolls into tomorrow's digest instead.
 */
sealed interface DigestDecision {
    data object SkipDelivered : DigestDecision
    data class Schedule(val fireAtEpochMillis: Long, val isCatchUp: Boolean) : DigestDecision
    data object SurfaceInApp : DigestDecision
    data object DeferToTomorrow : DigestDecision
}

fun decideDigestSchedule(
    dayKey: Long,
    fireAtEpochMillis: Long,
    nowEpochMillis: Long,
    quietCutoffEpochMillis: Long,
    deliveredDays: Set<Long>,
    source: ReconcileSource,
    catchUpDelayMillis: Long = CatchUpDelayMillis,
): DigestDecision {
    if (dayKey in deliveredDays) return DigestDecision.SkipDelivered
    if (fireAtEpochMillis > nowEpochMillis) {
        return DigestDecision.Schedule(fireAtEpochMillis, isCatchUp = false)
    }
    if (nowEpochMillis <= quietCutoffEpochMillis) {
        return when (source) {
            ReconcileSource.FOREGROUND -> DigestDecision.SurfaceInApp
            ReconcileSource.BACKGROUND -> DigestDecision.Schedule(
                nowEpochMillis + catchUpDelayMillis,
                isCatchUp = true,
            )
        }
    }
    return DigestDecision.DeferToTomorrow
}
