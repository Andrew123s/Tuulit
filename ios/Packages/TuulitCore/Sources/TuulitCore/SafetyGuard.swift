import Foundation

/// How urgent a red flag is. EMERGENCY means "call emergency services now".
public enum UrgencyLevel: String, Codable, Sendable, Comparable {
    case NONE, SEE_CLINICIAN_SOON, URGENT, EMERGENCY

    var rank: Int { [.NONE: 0, .SEE_CLINICIAN_SOON: 1, .URGENT: 2, .EMERGENCY: 3][self]! }
    public static func < (a: UrgencyLevel, b: UrgencyLevel) -> Bool { a.rank < b.rank }
}

public struct SafetyAssessment: Equatable, Sendable {
    public let level: UrgencyLevel
    public let headline: String
    public let message: String
    public var isRedFlag: Bool { level != .NONE }
}

/// Port of Android `MedicalSafetyGuard`: the same deterministic red-flag patterns, run on the
/// device before anything is sent to the AI. The Worker runs the same rules again server-side.
public enum MedicalSafetyGuard {
    private struct Rule { let level: UrgencyLevel; let patterns: [NSRegularExpression] }

    private static func r(_ p: String...) -> [NSRegularExpression] {
        // The Kotlin source uses Java regex; these patterns are compatible with ICU (NSRegularExpression).
        p.map { try! NSRegularExpression(pattern: $0, options: [.caseInsensitive]) }
    }

    private static let rules: [Rule] = [
        Rule(level: .EMERGENCY, patterns: r(
            #"\b(suicid\w*|kill (my|him|her)self|end my life|hurting myself|self[- ]?harm)\b"#,
            #"\b(chest pain|can'?t breathe|shortness of breath|trouble breathing)\b"#,
            #"\b(faint(ed|ing)?|pass(ed|ing) out|lost consciousness)\b"#,
            #"\bpregnan\w*\b.*\b(bleed\w*|pain)\b|\b(bleed\w*|pain)\b.*\bpregnan\w*\b"#,
            #"\bsharp\b.*\bone side\b|\bone[- ]sided\b.*\bpain\b"#
        )),
        Rule(level: .URGENT, patterns: r(
            #"\b(soak(ing|ed)? through|pad every hour|tampon every hour|every hour)\b"#,
            #"\bfever\b|\b(10[1-4]|3[89]([.,]\d)?) ?(°|degrees)?\s?(f|c)\b"#,
            #"\b(worst pain|unbearable|sudden severe|can'?t (stand|walk))\b"#,
            #"\bvomit\w*\b.*\b(non ?stop|can'?t stop|all day)\b"#,
            #"\bclots?\b.*\b(large|huge|golf|plum)\b"#
        )),
        Rule(level: .SEE_CLINICIAN_SOON, patterns: r(
            #"\b(no period|missed (my )?period)\b.*\b(3|three|4|four|5|five|6|six) months\b"#,
            #"\bbleed\w*\b.*\b(after sex|between periods|after menopause)\b"#
        )),
    ]

    public static func assess(_ input: String) -> SafetyAssessment {
        let text = input.trimmed
        if text.isEmpty { return none }
        let range = NSRange(text.startIndex..., in: text)
        guard let level = rules.first(where: { rule in rule.patterns.contains { $0.firstMatch(in: text, range: range) != nil } })?.level
        else { return none }
        switch level {
        case .EMERGENCY:
            return SafetyAssessment(level: level, headline: "Please get medical help now",
                                    message: "What you describe can be serious. Call your local emergency number or go to urgent care. If you are thinking about harming yourself, contact a crisis line or emergency services right away.")
        case .URGENT:
            return SafetyAssessment(level: level, headline: "Please see a doctor today",
                                    message: "Very heavy bleeding, fever, or severe pain that stops you from moving deserves in-person care today.")
        case .SEE_CLINICIAN_SOON:
            return SafetyAssessment(level: level, headline: "Worth booking an appointment",
                                    message: "This is worth checking with a clinician soon. Appointment prep in Ask can help you get ready.")
        case .NONE:
            return none
        }
    }

    /// Returns the warning text when anything urgent or emergency is found.
    public static func inspectForUrgentWarning(_ input: String) -> String? {
        let a = assess(input)
        return a.level == .URGENT || a.level == .EMERGENCY ? "\(a.headline). \(a.message)" : nil
    }

    private static let none = SafetyAssessment(level: .NONE, headline: "", message: "")
}
