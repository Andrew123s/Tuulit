import Foundation

// Care sessions, Play games and quizzes, achievements, the Today suggestion engine and the
// hero builder. Ported word for word from Android so both apps show the same content.

public struct SessionStep: Equatable, Sendable {
    public let title: String, detail: String, seconds: Int
}

public struct BreathingPattern: Equatable, Sendable {
    public let inhale: Int, hold: Int, exhale: Int
    public var cycleSeconds: Int { inhale + hold + exhale }
    public var label: String { hold > 0 ? "\(inhale) in, \(hold) hold, \(exhale) out" : "\(inhale) in, \(exhale) out" }
}

public struct CareSession: Equatable, Sendable, Identifiable {
    public let id: String, title: String, category: String, icon: String, summary: String
    public let steps: [SessionStep]
    public let breathing: BreathingPattern?
    public var totalSeconds: Int { steps.reduce(0) { $0 + $1.seconds } }
    public var minutesLabel: String { "\((totalSeconds + 59) / 60) min" }
}

public enum CareContent {
    public static let heatAndEase = CareSession(
        id: "heat_and_ease", title: "Heat and ease", category: "Cramps", icon: "heat",
        summary: "Warmth plus slow breathing relaxes the uterine muscle.",
        steps: [
            SessionStep(title: "Get warm", detail: "Place a hot water bottle or heat pad on your lower belly or lower back. Warm, not hot.", seconds: 60),
            SessionStep(title: "Knees to chest", detail: "Lie on your side and draw your knees gently towards your chest.", seconds: 60),
            SessionStep(title: "Breathe slowly", detail: "Breathe in for 4, out for 6. Let your belly soften on each out-breath.", seconds: 120),
            SessionStep(title: "Sip something warm", detail: "Ginger or peppermint tea, or warm water. Stay still a little longer.", seconds: 60),
        ],
        breathing: BreathingPattern(inhale: 4, hold: 0, exhale: 6))

    public static let slowBreathing = CareSession(
        id: "slow_breathing", title: "Slow breathing", category: "Calm", icon: "breath",
        summary: "A longer out-breath tells your body it is safe to slow down.",
        steps: [SessionStep(title: "Breathe with the circle", detail: "In as it grows, out as it shrinks.", seconds: 180)],
        breathing: BreathingPattern(inhale: 4, hold: 0, exhale: 6))

    public static let boxBreathing = CareSession(
        id: "box_breathing", title: "Box breathing", category: "Calm", icon: "breath",
        summary: "Four even sides. Useful when you feel on edge.",
        steps: [SessionStep(title: "Breathe in a square", detail: "In 4, hold 4, out 4.", seconds: 120)],
        breathing: BreathingPattern(inhale: 4, hold: 4, exhale: 4))

    public static let gentleStretches = CareSession(
        id: "gentle_stretches", title: "Gentle stretches", category: "Cramps", icon: "stretch",
        summary: "Slow movement for cramps, bloating and lower-back tension.",
        steps: [
            SessionStep(title: "Child's pose", detail: "Knees wide, sit back on your heels, arms long. Breathe into your lower back.", seconds: 60),
            SessionStep(title: "Cat and cow", detail: "On hands and knees, slowly round then gently arch your back.", seconds: 90),
            SessionStep(title: "Supine twist", detail: "On your back, drop both knees to one side. Switch halfway.", seconds: 120),
            SessionStep(title: "Legs up the wall", detail: "Rest your legs against a wall, arms relaxed.", seconds: 120),
            SessionStep(title: "Butterfly", detail: "Sit with soles together, knees falling open. Stay soft.", seconds: 90),
        ],
        breathing: nil)

    public static let sleepWindDown = CareSession(
        id: "sleep_wind_down", title: "Sleep wind-down", category: "Sleep", icon: "moon",
        summary: "Ten quiet minutes to help your body switch off.",
        steps: [
            SessionStep(title: "Lights low", detail: "Dim the room and put your phone face-down after this session.", seconds: 60),
            SessionStep(title: "Body scan", detail: "Move your attention slowly from your toes to your head, softening each part.", seconds: 240),
            SessionStep(title: "Breathe 4-7-8", detail: "In for 4, hold for 7, out for 8. Four rounds.", seconds: 180),
            SessionStep(title: "Let thoughts pass", detail: "If a worry comes up, name it and let it drift past.", seconds: 120),
        ],
        breathing: BreathingPattern(inhale: 4, hold: 7, exhale: 8))

    public static let moodReset = CareSession(
        id: "mood_reset", title: "Mood reset", category: "Mood", icon: "care",
        summary: "Two minutes to step out of a low or tense moment.",
        steps: [
            SessionStep(title: "Name it", detail: "Say to yourself what you feel. Naming a feeling softens it.", seconds: 30),
            SessionStep(title: "5-4-3-2-1", detail: "Notice 5 things you see, 4 you can touch, 3 you hear, 2 you smell, 1 you taste.", seconds: 60),
            SessionStep(title: "One kind thing", detail: "Choose one small kind thing to do for yourself next.", seconds: 30),
        ],
        breathing: nil)

    public static let all = [heatAndEase, slowBreathing, boxBreathing, gentleStretches, sleepWindDown, moodReset]
    public static func byId(_ id: String) -> CareSession? { all.first { $0.id == id } }

    private static let lowMoods: Set<String> = ["Sad", "Low", "Emotional"]
    private static let tenseMoods: Set<String> = ["Anxious", "Stressed", "Irritated"]

    /// Picks the hero session for Care from what was logged today.
    public static func heroFor(_ today: DailyLog?, _ phase: CyclePhase) -> CareSession {
        guard let t = today else { return phase == .MENSTRUAL ? heatAndEase : slowBreathing }
        if t.symptoms["Cramps"] != nil || t.symptoms["Back pain"] != nil { return heatAndEase }
        if t.symptoms["Sleep problems"] != nil || (1...2).contains(t.sleepQuality) { return sleepWindDown }
        if t.moods.contains(where: { lowMoods.contains($0) }) { return moodReset }
        if t.moods.contains(where: { tenseMoods.contains($0) }) { return boxBreathing }
        if t.symptoms["Bloating"] != nil { return gentleStretches }
        return phase == .MENSTRUAL ? heatAndEase : slowBreathing
    }

    public static func heroHeadline(_ today: DailyLog?) -> String {
        guard let t = today else { return "A little care goes a long way." }
        if t.symptoms["Cramps"] != nil { return "Let's ease those cramps." }
        if t.symptoms["Sleep problems"] != nil || (1...2).contains(t.sleepQuality) { return "Let's help you rest." }
        if t.moods.contains(where: { lowMoods.contains($0) }) { return "Be gentle with yourself today." }
        if t.moods.contains(where: { tenseMoods.contains($0) }) { return "Let's take the edge off." }
        return "A little care goes a long way."
    }

    public static let redFlags = [
        "Soaking through a pad or tampon every hour for several hours",
        "Pain that painkillers don't touch, or that stops you walking",
        "Fever, fainting or vomiting with your period",
        "Sudden sharp pain on one side",
        "Bleeding when you could be pregnant",
        "Periods that stop for 3+ months when not expected",
    ]
}

// MARK: - Play

public struct QuizQuestion: Equatable, Sendable {
    public let prompt: String
    public let options: [String]
    /// Index of the right answer, or -1 for reflective questions.
    public let correctIndex: Int
    public let explanation: String
    public let isStatement: Bool
    init(_ prompt: String, _ options: [String], _ correctIndex: Int, _ explanation: String, isStatement: Bool = false) {
        self.prompt = prompt; self.options = options; self.correctIndex = correctIndex; self.explanation = explanation; self.isStatement = isStatement
    }
}

public struct Quiz: Equatable, Sendable, Identifiable {
    public let id: String, title: String, subtitle: String, icon: String
    public let questions: [QuizQuestion]
    public let forPartner: Bool
    public var isReflective: Bool { questions.allSatisfy { $0.correctIndex < 0 } }
}

public struct GameInfo: Equatable, Sendable, Identifiable {
    public let id: String, title: String, kind: String, icon: String, blurb: String
}

public enum PlayContent {
    public static let games = [
        GameInfo(id: "banana_catch", title: "Banana Catch", kind: "Arcade", icon: "play", blurb: "Catch bananas, dodge rocks, three lives."),
        GameInfo(id: "mood_match", title: "Mood Match", kind: "Memory", icon: "memory", blurb: "Find the matching pairs of comfort symbols."),
        GameInfo(id: "memory_flip", title: "Memory Flip", kind: "Sequence", icon: "memory", blurb: "Watch the tiles light up, then repeat the order."),
        GameInfo(id: "brain_break", title: "Brain Break", kind: "Quick sums", icon: "quiz", blurb: "Gentle mental arithmetic against a soft timer."),
        GameInfo(id: "word_puzzle", title: "Word Puzzle", kind: "Words", icon: "book", blurb: "Unscramble calm, cosy words."),
        GameInfo(id: "calm_tap", title: "Calm Tap", kind: "Rhythm", icon: "breath", blurb: "Tap as the circle reaches the ring."),
        GameInfo(id: "focus_game", title: "Focus Game", kind: "Attention", icon: "eye", blurb: "Tap only the banana among the shapes."),
        GameInfo(id: "stress_smash", title: "Stress Smash", kind: "Let it out", icon: "pain", blurb: "Crush tiles. No timer, no losing."),
    ]
    public static func game(_ id: String) -> GameInfo? { games.first { $0.id == id } }

    /// Same daily pick as Android: today's epoch day modulo the number of games.
    public static func dailyPick(today: Int) -> GameInfo { games[abs(today % games.count)] }

    public static let words: [(word: String, hint: String)] = [
        ("CALM", "How you feel after slow breathing"), ("COSY", "Blanket, tea and rain outside"), ("REST", "Valid on any day"),
        ("WARMTH", "What a hot water bottle gives"), ("GINGER", "A tea that can settle cramps"), ("BREATHE", "In for four, out for six"),
        ("SLEEP", "What a wind-down helps with"), ("BANANA", "Tuulit's favourite fruit"), ("GENTLE", "How to treat yourself today"),
        ("STRETCH", "Cat and cow is one"), ("COMFORT", "What Care is for"), ("PEACE", "Quiet in your mind"),
    ]

    public static let quizzes: [Quiz] = [
        Quiz(id: "myths_facts", title: "Period myths or facts?", subtitle: "4 statements, no judgement", icon: "quiz", questions: [
            QuizQuestion("Everyone with a healthy cycle has a 28-day cycle.", ["Fact", "Myth"], 1,
                         "Healthy adult cycles usually run 21 to 35 days, and they can shift by a few days from month to month.", isStatement: true),
            QuizQuestion("Pain that stops you working or sleeping is just something to put up with.", ["Fact", "Myth"], 1,
                         "Disruptive pain deserves a clinician's attention. It can have treatable causes such as endometriosis.", isStatement: true),
            QuizQuestion("Gentle movement during your period is unsafe.", ["Fact", "Myth"], 1,
                         "Gentle movement is safe if it feels comfortable, and rest is equally valid.", isStatement: true),
            QuizQuestion("Calendar estimates of your fertile window are reliable contraception.", ["Fact", "Myth"], 1,
                         "Ovulation timing shifts easily. Calendar estimates should never be used as birth control.", isStatement: true),
        ], forPartner: false),
        Quiz(id: "cycle_knowledge", title: "Know your cycle", subtitle: "3 questions on how your body works", icon: "book", questions: [
            QuizQuestion("Which chemical messengers trigger period cramps?", ["Prostaglandins", "Melatonin", "Adrenaline"], 0,
                         "Prostaglandins make the uterus contract to shed its lining. Higher levels often mean stronger cramps."),
            QuizQuestion("Which phase usually comes between ovulation and your next period?", ["Follicular phase", "Luteal phase", "Menstrual phase"], 1,
                         "The luteal phase follows ovulation and lasts roughly 11 to 16 days for most people."),
            QuizQuestion("Can stress, travel or illness shift when your period arrives?", ["Yes, they can", "No, never"], 0,
                         "The brain and ovaries talk through hormones that respond to stress, illness and sleep changes."),
        ], forPartner: false),
        Quiz(id: "self_care", title: "What do you need today?", subtitle: "A check-in with no right answers", icon: "care", questions: [
            QuizQuestion("How does your social battery feel right now?", ["Quiet blanket mode", "A cosy one-on-one chat", "Up for a distraction"], -1,
                         "Honour your real capacity today. There is no right answer."),
            QuizQuestion("Which comfort sounds best in the next 10 minutes?", ["Warm drink and breathing", "A quick game", "Phone down and rest"], -1,
                         "Add your favourite to your Comfort Library so it's one tap away next time."),
        ], forPartner: false),
        Quiz(id: "couple_support", title: "Couple quiz", subtitle: "How well does your person know periods?", icon: "users", questions: [
            QuizQuestion("Your person says their cramps are rough today. What's the best first move?",
                         ["Ask what would help right now", "Give medical advice", "Say it can't be that bad"], 0,
                         "Asking rather than assuming respects their comfort and builds trust."),
            QuizQuestion("Are two people's cycles and symptoms basically the same?", ["Yes, mostly", "No, they vary widely"], 1,
                         "Symptoms, flow, energy and cycle length differ a lot from person to person."),
            QuizQuestion("If your partner is frustrated in their luteal phase, should you blame hormones?",
                         ["No, listen to the actual concern first", "Yes, mention their cycle"], 0,
                         "Dismissing real feelings as 'just hormones' is invalidating. Listen first."),
        ], forPartner: true),
    ]
    public static func quiz(_ id: String) -> Quiz? { quizzes.first { $0.id == id } }
}

/// Earned by real actions. Never streaks, never loss. Raw values match Android's enum names (synced).
public enum Achievement: String, CaseIterable, Codable, Sendable {
    case FIRST_LOG, FIRST_PERIOD, FIRST_SESSION, FIVE_SESSIONS, FIRST_GAME, FIRST_QUIZ, INSIGHTS_EXPLORED, PAIN_JOURNAL, CONNECTED

    public var title: String {
        switch self {
        case .FIRST_LOG: return "First check-in"
        case .FIRST_PERIOD: return "Cycle started"
        case .FIRST_SESSION: return "Took a moment"
        case .FIVE_SESSIONS: return "Comfort habit"
        case .FIRST_GAME: return "Two minutes off"
        case .FIRST_QUIZ: return "Curious mind"
        case .INSIGHTS_EXPLORED: return "Pattern spotter"
        case .PAIN_JOURNAL: return "Keeping track"
        case .CONNECTED: return "Let someone in"
        }
    }

    public var description: String {
        switch self {
        case .FIRST_LOG: return "You logged how you felt for the first time."
        case .FIRST_PERIOD: return "You logged your first period in Tuulit."
        case .FIRST_SESSION: return "You finished your first Care session."
        case .FIVE_SESSIONS: return "You finished five Care sessions."
        case .FIRST_GAME: return "You finished your first game."
        case .FIRST_QUIZ: return "You completed a quiz."
        case .INSIGHTS_EXPLORED: return "You explored your insights."
        case .PAIN_JOURNAL: return "You recorded a pain journal entry."
        case .CONNECTED: return "You connected with a trusted person."
        }
    }
}

// MARK: - Today

public struct Suggestion: Equatable, Sendable {
    public let kicker: String, title: String, body: String
    public var sessionId: String? = nil
    public var route: String? = nil
}

public enum SuggestionEngine {
    public static func forToday(_ prediction: CyclePrediction, _ today: DailyLog?, hourOfDay: Int) -> Suggestion {
        let symptoms = today?.symptoms ?? [:]
        let evening = hourOfDay >= 19
        if symptoms["Cramps"] != nil {
            return Suggestion(kicker: "You logged cramps", title: "Heat and slow breathing", body: "Five minutes that can ease the squeeze.", sessionId: CareContent.heatAndEase.id)
        }
        if evening && (symptoms["Sleep problems"] != nil || (1...2).contains(today?.sleepQuality ?? 0)) {
            return Suggestion(kicker: "For tonight", title: "A 10-minute wind-down", body: "You logged poor sleep recently.", sessionId: CareContent.sleepWindDown.id)
        }
        if (today?.moods ?? []).contains(where: { ["Anxious", "Stressed", "Irritated"].contains($0) }) {
            return Suggestion(kicker: "For right now", title: "Box breathing", body: "Two minutes to take the edge off.", sessionId: CareContent.boxBreathing.id)
        }
        if (1...2).contains(today?.energyLevel ?? 0) {
            return Suggestion(kicker: "Low energy today", title: "Keep it light", body: "A short distraction might help more than pushing through.", route: "care/play")
        }
        if prediction.isCurrentlyMenstruating {
            return Suggestion(kicker: "During your period", title: "Heat and ease", body: "Warmth and rest are often welcome now.", sessionId: CareContent.heatAndEase.id)
        }
        switch prediction.currentPhase {
        case .LUTEAL:
            return Suggestion(kicker: "For the luteal phase", title: "Ten minutes of gentle stretching", body: "Can ease bloating and back tension.", sessionId: CareContent.gentleStretches.id)
        case .FOLLICULAR:
            return Suggestion(kicker: "For the follicular phase", title: "Learn what's happening", body: "Energy often rises after your period. Here's why.", route: "ask/chat/explain?topic=follicular")
        case .ESTIMATED_OVULATION:
            return Suggestion(kicker: "Mid-cycle", title: "What ovulation feels like", body: "Some people notice a twinge or a shift in energy.", route: "ask/chat/explain?topic=ovulation")
        default:
            return Suggestion(kicker: "Getting started", title: "Log your period when it comes", body: "Predictions appear once Tuulit knows one start date.", route: "log")
        }
    }
}

public struct TodayHero: Equatable, Sendable {
    public let lead: String, number: String?, unit: String, caption: String
}

public enum TodayHeroBuilder {
    /// First day of the period that includes `today` (walks back over consecutive logged days).
    public static func periodStart(loggedPeriodDays: Set<Int>, today: Int) -> Int {
        var d = today
        while loggedPeriodDays.contains(d - 1) { d -= 1 }
        return d
    }

    public static func build(_ p: CyclePrediction, today: Int, format: (Int) -> String) -> TodayHero {
        if !p.hasRecordedCycles {
            return TodayHero(lead: "Let's get to know your cycle.", number: nil, unit: "", caption: "Log your next period and predictions will appear here.")
        }
        let phaseLine = "day \(p.currentCycleDay.map(String.init) ?? "–"), \(p.currentPhase.title.lowercased())"
        if p.isCurrentlyMenstruating {
            let start = periodStart(loggedPeriodDays: Set(p.loggedPeriodDays.filter { $0 <= today }), today: today)
            let dayOfPeriod = today - start + 1
            return TodayHero(lead: "Your period", number: String(dayOfPeriod), unit: dayOfPeriod == 1 ? "st day" : ordinal(dayOfPeriod) + " day",
                             caption: "Day \(p.currentCycleDay ?? 1) of your cycle · logged")
        }
        guard let next = p.nextPeriodStartEpochDay else { return TodayHero(lead: "", number: nil, unit: "", caption: phaseLine) }
        let days = next - today
        let rangeText = p.isIrregularEstimate
            ? "\(p.nextPeriodRangeMinDays ?? 0)–\(p.nextPeriodRangeMaxDays ?? 0) days · estimate · \(phaseLine)"
            : "\(format(next))–\(format(next + p.averagePeriodDuration - 1)) · estimate · \(phaseLine)"
        if days < 0 {
            return TodayHero(lead: "Your period may be late by", number: String(-days), unit: days == -1 ? " day" : " days",
                             caption: "Cycles shift. Log it when it starts · \(phaseLine)")
        }
        if days == 0 { return TodayHero(lead: "Your period is likely", number: nil, unit: "today", caption: rangeText) }
        return TodayHero(lead: "Your period is likely in", number: String(days), unit: days == 1 ? " day" : " days", caption: rangeText)
    }

    private static func ordinal(_ n: Int) -> String {
        if (11...13).contains(n % 100) { return "th" }
        switch n % 10 { case 1: return "st"; case 2: return "nd"; case 3: return "rd"; default: return "th" }
    }
}

/// Pure scheduling rules for reminders (Android `ReminderRules`).
public enum ReminderRules {
    public static func inQuietHours(minuteOfDay: Int, start: Int, end: Int) -> Bool {
        if start == end { return false }
        return start < end ? (minuteOfDay >= start && minuteOfDay < end) : (minuteOfDay >= start || minuteOfDay < end)
    }

    /// Check-in minute for a day, pushed out of quiet hours to the quiet-hours end if needed.
    /// Returns (dayOffset, minuteOfDay) relative to the day it was asked for.
    public static func checkInSlot(checkInMinute: Int, quietStart: Int, quietEnd: Int) -> (dayOffset: Int, minute: Int) {
        guard inQuietHours(minuteOfDay: checkInMinute, start: quietStart, end: quietEnd) else { return (0, checkInMinute) }
        return quietEnd > checkInMinute ? (0, quietEnd) : (1, quietEnd)
    }

    /// Android `nextRun`: minutes are counted from local midnight of `today`.
    public static func nextRun(nowMinuteOfDay: Int, checkInMinute: Int, quietStart: Int, quietEnd: Int) -> (dayOffset: Int, minute: Int) {
        let day = checkInMinute > nowMinuteOfDay ? 0 : 1
        let slot = checkInSlot(checkInMinute: checkInMinute, quietStart: quietStart, quietEnd: quietEnd)
        return (day + slot.dayOffset, slot.minute)
    }

    public static func periodHeadsUpDue(nextStart: Int?, today: Int, leadDays: Int) -> Bool {
        guard let n = nextStart else { return false }
        return n - today == leadDays
    }
}
