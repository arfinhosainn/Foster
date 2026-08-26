import SwiftUI
import UserNotifications

@main
struct iOSApp: App {
    init() {
        let delegate = NotificationDelegate.shared
        delegate.registerCategories()
        UNUserNotificationCenter.current().delegate = delegate
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
