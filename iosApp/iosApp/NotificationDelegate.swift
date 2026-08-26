import UIKit
import UserNotifications
import Onboarding

/// Routes notification taps into the Kotlin navigation bridge and registers
/// the per-category notification categories (plan §3.6). Installed as the
/// UNUserNotificationCenter delegate at app init.
final class NotificationDelegate: NSObject, UNUserNotificationCenterDelegate {
    static let shared = NotificationDelegate()

    func registerCategories() {
        let identifiers = [
            "check_ins",
            "custom_reminders",
            "missed_check_ins",
        ]
        let categories = identifiers.map { identifier in
            UNNotificationCategory(
                identifier: identifier,
                actions: [],
                intentIdentifiers: [],
                options: []
            )
        }
        UNUserNotificationCenter.current().setNotificationCategories(Set(categories))
    }

    // Foreground presentation: show banners even while the app is open.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound])
    }

    // Tap routing — buffered into the Kotlin router. Cold-start taps arrive
    // here before the Compose UI exists and are drained once the navigation
    // graph is ready (plan §4).
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        // Kotlin `Long?` is exported to Swift as `KotlinLong?`, so read the
        // raw NSNumber out of userInfo and box it explicitly.
        let dayKey = (userInfo["dayKey"] as? NSNumber)?.int64Value
        let contactId = userInfo["contactId"] as? String
        if dayKey != nil || contactId != nil {
            NotificationTapRouter.shared.post(
                target: NotificationTarget(
                    dayKey: dayKey.map { KotlinLong(value: $0) },
                    contactId: contactId
                )
            )
        }
        completionHandler()
    }
}
