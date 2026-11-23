import UserNotifications

/// Connect pushes arrive with discreet text ("Tuulit · Take a quick moment for yourself."),
/// which is what a locked phone shows by default. When the person has turned discreet
/// notifications off, this extension rewrites the text from the push data, using the same copy
/// as Android's TuulitMessagingService.
final class NotificationService: UNNotificationServiceExtension {
    override func didReceive(_ request: UNNotificationRequest, withContentHandler contentHandler: @escaping (UNNotificationContent) -> Void) {
        guard let content = request.content.mutableCopy() as? UNMutableNotificationContent else { contentHandler(request.content); return }
        let discreet = UserDefaults(suiteName: "group.app.tuulit")?.object(forKey: "discreet") as? Bool ?? true
        let info = content.userInfo
        let from = (info["fromName"] as? String).flatMap { $0.isEmpty ? nil : $0 } ?? "Your person"
        let message = info["message"] as? String ?? ""
        content.userInfo["route"] = "connect"
        if !discreet {
            switch info["type"] as? String {
            case "sos":
                content.title = "\(from) could use some support"
                content.body = message.isEmpty ? "Open Tuulit to see how to help." : message
            case "reply":
                content.title = "\(from) sent you something"
                content.body = message
            case "accepted":
                content.title = "\(from) accepted your invite"
                content.body = "Choose what they can see in Connect."
            case "status":
                content.title = "An update from \(from)"
                content.body = "Open Tuulit to see it."
            default: break
            }
        }
        contentHandler(content)
    }
}
