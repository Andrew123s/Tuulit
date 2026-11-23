import Foundation

enum Tab: Hashable, CaseIterable { case today, cycle, care, ask }

enum AuthMode: Hashable { case signIn, register, forgot }

/// Every pushed screen. `Route.parse` understands the Android route strings used in shared
/// content (suggestions, notifications), so both apps navigate from the same data.
enum Route: Hashable {
    case insights, pain, history
    case session(String), game(String), quiz(String)
    case askChat(tool: String, topic: String?)
    case you, youCycle, youBanana, youPrivacy, youNotifications, youAppearance, youAccount, youHelp
    case auth(AuthMode)
    case connect, connectInvite, connectAccept, connectPerson(String), partner(String)

    /// Result of following an Android-style route string.
    enum Target: Equatable {
        case push(Route)
        case tab(Tab, careSegment: Int?)
        case openLog
    }

    static func parse(_ raw: String) -> Target? {
        let (path, query) = raw.split(separator: "?", maxSplits: 1).map(String.init).splitPair()
        let parts = path.split(separator: "/").map(String.init)
        let topic = query.flatMap { q in
            q.split(separator: "&").map(String.init).first { $0.hasPrefix("topic=") }
                .map { String($0.dropFirst(6)).removingPercentEncoding ?? String($0.dropFirst(6)) }
        }
        switch parts.first {
        case "today": return .tab(.today, careSegment: nil)
        case "cycle": return .tab(.cycle, careSegment: nil)
        case "log": return .openLog
        case "care":
            let seg = parts.count > 1 ? ["play": 1, "library": 2][parts[1]] ?? 0 : 0
            return .tab(.care, careSegment: seg)
        case "ask":
            if parts.count >= 3, parts[1] == "chat" { return .push(.askChat(tool: parts[2], topic: topic)) }
            return .tab(.ask, careSegment: nil)
        case "insights": return .push(.insights)
        case "pain": return .push(.pain)
        case "history": return .push(.history)
        case "session": return parts.count > 1 ? .push(.session(parts[1])) : nil
        case "game": return parts.count > 1 ? .push(.game(parts[1])) : nil
        case "quiz": return parts.count > 1 ? .push(.quiz(parts[1])) : nil
        case "you":
            guard parts.count > 1 else { return .push(.you) }
            switch parts[1] {
            case "cycle": return .push(.youCycle)
            case "banana": return .push(.youBanana)
            case "privacy": return .push(.youPrivacy)
            case "notifications": return .push(.youNotifications)
            case "appearance": return .push(.youAppearance)
            case "account": return .push(.youAccount)
            case "help": return .push(.youHelp)
            default: return .push(.you)
            }
        case "auth":
            let mode: AuthMode = parts.count > 1 ? (parts[1] == "register" ? .register : parts[1] == "forgot" ? .forgot : .signIn) : .signIn
            return .push(.auth(mode))
        case "connect":
            guard parts.count > 1 else { return .push(.connect) }
            switch parts[1] {
            case "invite": return .push(.connectInvite)
            case "accept": return .push(.connectAccept)
            case "person": return parts.count > 2 ? .push(.connectPerson(parts[2])) : .push(.connect)
            default: return .push(.connect)
            }
        case "partner": return parts.count > 1 ? .push(.partner(parts[1])) : nil
        default: return nil
        }
    }
}

private extension Array where Element == String {
    func splitPair() -> (String, String?) { (first ?? "", count > 1 ? self[1] : nil) }
}

/// Sheets the shell can show over any tab.
enum ShellSheet: String, Identifiable {
    case needSomething, sos, bananaMode
    var id: String { rawValue }
}
