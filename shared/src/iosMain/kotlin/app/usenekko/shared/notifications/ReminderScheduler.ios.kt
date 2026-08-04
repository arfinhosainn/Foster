package app.usenekko.shared.notifications

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSCalendarUnitYear
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
 * iOS actual. No authorization is requested here — [requestAuthorization] is only
 * invoked from the onboarding "Turn on Notification" action. [schedule]/[cancel]
 * are no-ops if notifications aren't authorized.
 */
actual class ReminderScheduler {

    actual suspend fun schedule(
        contactId: String,
        contactName: String,
        fireAtEpochMillis: Long,
    ) {
        val authorized = isAuthorized()
        if (!authorized) return

        // NSDate uses a reference date (2001-01-01); convert the Unix epoch.
        val fireAtReference = fireAtEpochMillis / 1000.0 - 978307200.0
        if (fireAtReference <= NSDate().timeIntervalSinceReferenceDate) return // in the past
        val fireAt = NSDate(timeIntervalSinceReferenceDate = fireAtReference)

        val trigger = dateComponents(fireAt)?.let { components ->
            UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                components,
                repeats = false,
            )
        } ?: return

        val content = UNMutableNotificationContent().apply {
            setTitle("Check in with $contactName")
            setBody("It's time to check in on $contactName")
            setSound(UNNotificationSound.defaultSound)
        }

        // contactId is the request identifier so cancel() can target it.
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = contactId,
            content = content,
            trigger = trigger,
        )

        UNUserNotificationCenter.currentNotificationCenter()
            .addNotificationRequest(request, withCompletionHandler = null)
    }

    actual suspend fun cancel(contactId: String) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.removePendingNotificationRequestsWithIdentifiers(listOf(contactId))
        center.removeDeliveredNotificationsWithIdentifiers(listOf(contactId))
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