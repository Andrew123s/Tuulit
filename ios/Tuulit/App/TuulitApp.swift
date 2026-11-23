import SwiftUI
import UIKit
import UserNotifications
import FirebaseCore
import FirebaseMessaging
import TuulitCore

@main
struct TuulitApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var delegate
    @State private var model = AppModel()
    @Environment(\.scenePhase) private var phase

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(model)
                .onAppear { delegate.model = model }
                .task(id: model.auth.state.uid) { await model.onAuthOrForeground() }
                .onChange(of: phase) { _, p in
                    switch p {
                    case .background: model.didEnterBackground()
                    case .active:
                        model.willEnterForeground()
                        Task { await model.onAuthOrForeground() }
                    default: break
                    }
                }
        }
    }
}

/// Push notifications (FCM over APNs) and notification taps.
final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    @MainActor weak var model: AppModel?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        if FirebaseApp.app() != nil {
            Messaging.messaging().delegate = self
            application.registerForRemoteNotifications()
        }
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        if FirebaseApp.app() != nil { Messaging.messaging().apnsToken = deviceToken }
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let fcmToken else { return }
        Task { @MainActor in
            PushTokens.fcmToken = fcmToken
            if let model, model.auth.state.isSignedIn { await model.connect.registerDevice(token: fcmToken) }
        }
    }

    /// Shows Connect pushes while Tuulit is open (as a banner) and keeps reminders quiet in-app.
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification) async -> UNNotificationPresentationOptions {
        notification.request.content.userInfo["type"] != nil ? [.banner, .sound] : []
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse) async {
        let info = response.notification.request.content.userInfo
        let route = info["route"] as? String ?? (info["type"] != nil ? "connect" : "today")
        await MainActor.run { model?.go(route) }
    }
}
