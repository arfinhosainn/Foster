package app.usefoster.shared.notifications

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSBundle
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

/**
 * iOS actual. No authorization is requested here — [ReminderScheduler.Companion.requestAuthorization]
 * is only invoked from the onboarding "Turn on Notification" action. Scheduling
 * is a no-op when notifications aren't authorized.
 *
 * iOS local notifications are immutable and cannot self-report delivery to the
 * app, so a scheduled day/key is marked delivered AT SCHEDULE TIME (a scheduled
 * iOS local notification always fires unless explicitly cancelled, and
 * cancellation flows through the same reconciler that owns the delivered set).
 * Copy is resolved from the main bundle so it follows the device language
 * (EN/ES, plurals via .stringsdict).
 */
actual class ReminderScheduler : NotificationSchedulingOps {

    actual override suspend fun scheduleDay(plan: DayPlan) {
        val authorized = isAuthorized()
        if (!authorized) return

        val identifier = "day:${plan.dayKey}"
        val content = if (plan.itemCount <= 1) {
            content(
                title = localized("notif_checkin_title", plan.headline),
                body = localized("notif_checkin_body", plan.headline),
                category = plan.category,
                dayKey = plan.dayKey,
                contactId = plan.singleTargetId,
            )
        } else {
            content(
                title = localizedPlural("notif_digest_title", plan.itemCount),
                body = localized("notif_digest_body", plan.headline, plan.itemCount - 1L),
                category = plan.category,
                dayKey = plan.dayKey,
                contactId = null,
            )
        }

        addRequest(identifier, content, plan.fireAtEpochMillis)

        // iOS delivers what it schedules — mark delivered immediately so
        // reconciles never double-buzz (plan §3.3).
        val store = defaultNotificationPlanStore()
        val state = store.load()
        store.save(state.copy(deliveredDays = state.deliveredDays + plan.dayKey))
    }

    actual override suspend fun scheduleStandalone(plan: StandalonePlan) {
        val authorized = isAuthorized()
        if (!authorized) return

        val content = content(
            title = localized("notif_custom_reminder_title", plan.title),
            body = localized("notif_custom_reminder_body"),
            category = plan.category,
            dayKey = null,
            contactId = plan.targetId?.takeIf { plan.targetIsContact },
        )

        addRequest(identifier = "custom:${plan.key}", content = content, fireAt = plan.fireAtEpochMillis)

        val store = defaultNotificationPlanStore()
        val state = store.load()
        store.save(state.copy(deliveredKeys = state.deliveredKeys + plan.key))
    }

    actual override suspend fun cancelDay(dayKey: Long) {
        val identifier = "day:$dayKey"
        center.removePendingNotificationRequestsWithIdentifiers(listOf(identifier))
        center.removeDeliveredNotificationsWithIdentifiers(listOf(identifier))
    }

    actual override suspend fun cancelStandalone(key: String) {
        val identifier = "custom:$key"
        center.removePendingNotificationRequestsWithIdentifiers(listOf(identifier))
        center.removeDeliveredNotificationsWithIdentifiers(listOf(identifier))
    }

    actual suspend fun isEnabled(): Boolean = isAuthorized()

    actual suspend fun openSettings() {
        val url = NSURL(string = UIApplicationOpenSettingsURLString)
        val app = UIApplication.sharedApplication
        if (app.canOpenURL(url)) {
            app.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
        }
    }

    private val center: UNUserNotificationCenter
        get() = UNUserNotificationCenter.currentNotificationCenter()

    private fun addRequest(identifier: String, content: UNMutableNotificationContent, fireAt: Long) {
        // NSDate uses a reference date (2001-01-01); convert the Unix epoch.
        val fireAtReference = fireAt / 1000.0 - 978307200.0
        if (fireAtReference <= NSDate().timeIntervalSinceReferenceDate) return // in the past

        val date = NSDate(timeIntervalSinceReferenceDate = fireAtReference)
        val components = dateComponents(date) ?: return
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            components,
            repeats = false,
        )
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = identifier,
            content = content,
            trigger = trigger,
        )
        center.addNotificationRequest(request, withCompletionHandler = null)
    }

    private fun content(
        title: String,
        body: String,
        category: String,
        dayKey: Long?,
        contactId: String?,
    ): UNMutableNotificationContent = UNMutableNotificationContent().apply {
        setTitle(title)
        setBody(body)
        setSound(UNNotificationSound.defaultSound)
        setCategoryIdentifier(category)
        val userInfo = mutableMapOf<Any?, Any>()
        dayKey?.let { userInfo[NotificationTapKeys.DAY_KEY] = it }
        contactId?.let { userInfo[NotificationTapKeys.CONTACT_ID] = it }
        setUserInfo(userInfo)
    }

    private suspend fun isAuthorized(): Boolean = suspendCancellableCoroutine { continuation ->
        center.getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus
            val authorized = status == UNAuthorizationStatusAuthorized ||
                status == UNAuthorizationStatusProvisional
            continuation.resume(authorized)
        }
    }

    private fun dateComponents(date: NSDate): NSDateComponents? {
        val calendar = NSCalendar.currentCalendar
        val unit = NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or
            NSCalendarUnitHour or NSCalendarUnitMinute or NSCalendarUnitSecond
        return calendar.components(unit, fromDate = date)
    }

    companion object {
        /**
         * Requests notification authorization. Never called automatically — only
         * from the onboarding "Turn on Notification" button.
         */
        suspend fun requestAuthorization(): Boolean = suspendCancellableCoroutine { continuation ->
            val center = UNUserNotificationCenter.currentNotificationCenter()
            center.requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
            ) { granted, _ ->
                continuation.resume(granted)
            }
        }
    }
}

/** userInfo keys the Swift notification delegate reads to route taps. */
object NotificationTapKeys {
    const val DAY_KEY = "dayKey"
    const val CONTACT_ID = "contactId"
}

/** Main-bundle localized lookup with positional %1$@ / %1$ld substitution. */
internal fun localized(key: String, vararg args: Any?): String {
    val template = NSBundle.mainBundle.localizedStringForKey(key, value = key, table = null)
    if (args.isEmpty()) return template
    var result = template
    args.forEachIndexed { index, arg ->
        val position = index + 1
        result = result.replace("%" + position + "$@", arg.toString())
            .replace("%" + position + "\$ld", arg.toString())
    }
    return result
}

/** Plural lookup via explicit one/other keys (no .stringsdict needed). */
internal fun localizedPlural(key: String, count: Int): String {
    val suffix = if (count == 1) "_one" else "_other"
    val template = NSBundle.mainBundle.localizedStringForKey(
        key + suffix,
        value = key + suffix,
        table = null,
    )
    return template.replace("%d", count.toString())
}

/**
 * iOS actual of the plan store, backed by NSUserDefaults. The snapshot is
 * small (a few KB) — defaults are appropriate for this size.
 */
actual fun defaultNotificationPlanStore(): NotificationPlanStore = IosNotificationPlanStore

private object IosNotificationPlanStore : NotificationPlanStore {

    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
    private const val KEY = "notification_plan_state"

    private val defaults: NSUserDefaults
        get() = NSUserDefaults.standardUserDefaults

    override suspend fun load(): NotificationPlanState {
        val raw = defaults.stringForKey(KEY) ?: return NotificationPlanState()
        return runCatching { json.decodeFromString<NotificationPlanState>(raw) }
            .getOrDefault(NotificationPlanState())
    }

    override suspend fun save(state: NotificationPlanState) {
        defaults.setObject(
            json.encodeToString(NotificationPlanState.serializer(), state),
            forKey = KEY,
        )
    }
}
