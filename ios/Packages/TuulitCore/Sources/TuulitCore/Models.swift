import Foundation

// Ported from Android `domain/CycleModels.kt`. Raw values are the Kotlin enum names, because
// they are what is stored in Firestore and must read the same on both platforms.

public enum FlowIntensity: String, CaseIterable, Codable, Sendable {
    case NONE, SPOTTING, LIGHT, MEDIUM, HEAVY

    public var label: String {
        switch self {
        case .NONE: return "No flow"
        case .SPOTTING: return "Spotting"
        case .LIGHT: return "Light"
        case .MEDIUM: return "Medium"
        case .HEAVY: return "Heavy"
        }
    }

    public var level: Int {
        switch self {
        case .NONE: return 0
        case .SPOTTING: return 1
        case .LIGHT: return 2
        case .MEDIUM: return 3
        case .HEAVY: return 4
        }
    }

    public static func fromName(_ raw: String?) -> FlowIntensity { FlowIntensity(rawValue: raw ?? "") ?? .NONE }
}

public enum SymptomSeverity: String, CaseIterable, Codable, Sendable {
    case MILD, MODERATE, SEVERE

    public var label: String {
        switch self { case .MILD: return "Mild"; case .MODERATE: return "Moderate"; case .SEVERE: return "Severe" }
    }

    public var level: Int {
        switch self { case .MILD: return 1; case .MODERATE: return 2; case .SEVERE: return 3 }
    }

    public static func fromLevel(_ level: Int) -> SymptomSeverity { allCases.first { $0.level == level } ?? .MILD }
}

public enum CyclePhase: String, CaseIterable, Codable, Sendable {
    case MENSTRUAL, FOLLICULAR, ESTIMATED_OVULATION, LUTEAL, UNKNOWN

    public var title: String {
        switch self {
        case .MENSTRUAL: return "Menstrual Phase"
        case .FOLLICULAR: return "Follicular Phase"
        case .ESTIMATED_OVULATION: return "Estimated Ovulation Window"
        case .LUTEAL: return "Luteal Phase"
        case .UNKNOWN: return "Getting to Know Your Cycle"
        }
    }

    public var shortSummary: String {
        switch self {
        case .MENSTRUAL: return "Your body is shedding the uterine lining. Comfort, rest, and warmth often feel welcome."
        case .FOLLICULAR: return "Following your period, estrogen typically begins rising. Experiences vary from person to person."
        case .ESTIMATED_OVULATION: return "An approximate mid-cycle window based on your logs. Estimates cannot be used as contraception."
        case .LUTEAL: return "The time between estimated ovulation and your next period. Some people notice shifts in energy or sleep."
        case .UNKNOWN: return "Log your period dates whenever you are ready so Tuulit can calculate personalized estimates."
        }
    }
}

public enum BananaModePersona: String, CaseIterable, Codable, Sendable {
    case NEUTRAL, CHILL, CRAMPS, SLEEPY, DRAMA, GYM, HAPPY

    public var title: String {
        switch self {
        case .NEUTRAL: return "Warm"; case .CHILL: return "Chill"; case .CRAMPS: return "Cramps"; case .SLEEPY: return "Sleepy"
        case .DRAMA: return "Drama"; case .GYM: return "Gym"; case .HAPPY: return "Happy"
        }
    }

    public var tagline: String {
        switch self {
        case .NEUTRAL: return "Calm, balanced everyday companion"
        case .CHILL: return "Slow down, breathe, and soften the day"
        case .CRAMPS: return "Cozy warmth and gentle pain support"
        case .SLEEPY: return "Low-energy rest and quiet winding down"
        case .DRAMA: return "Witty distractions and lighthearted play"
        case .GYM: return "Gentle stretching and comfortable movement"
        case .HAPPY: return "Celebrating small wins without pressure"
        }
    }

    public static func fromName(_ raw: String?) -> BananaModePersona { BananaModePersona(rawValue: raw ?? "") ?? .NEUTRAL }
}

public enum CycleRegularity: String, CaseIterable, Codable, Sendable {
    case REGULAR, SOMEWHAT_VARIABLE, IRREGULAR, NOT_SURE

    public var label: String {
        switch self {
        case .REGULAR: return "Mostly regular"
        case .SOMEWHAT_VARIABLE: return "Somewhat variable"
        case .IRREGULAR: return "Irregular / unpredictable"
        case .NOT_SURE: return "Not sure yet"
        }
    }

    public static func fromName(_ raw: String?) -> CycleRegularity { CycleRegularity(rawValue: raw ?? "") ?? .NOT_SURE }
}

public struct CycleRecord: Equatable, Hashable, Codable, Sendable, Identifiable {
    public var id: String
    public var startEpochDay: Int
    public var endEpochDay: Int?
    public init(id: String = "", startEpochDay: Int, endEpochDay: Int? = nil) {
        self.id = id; self.startEpochDay = startEpochDay; self.endEpochDay = endEpochDay
    }
}

public struct DailyLog: Equatable, Codable, Sendable {
    public var epochDay: Int
    public var flow: FlowIntensity = .NONE
    public var symptoms: [String: SymptomSeverity] = [:]
    public var moods: [String] = []
    /// 0 = unset, 1...5 (rough ... great)
    public var moodScore: Int = 0
    public var energyLevel: Int = 0
    public var sleepQuality: Int = 0
    public var waterGlasses: Int = 0
    public var exerciseNote: String = ""
    public var medicationNote: String = ""
    public var journalText: String = ""
    public var voiceNoteKey: String? = nil
    public var updatedAtMillis: Int64 = 0

    public init(epochDay: Int, flow: FlowIntensity = .NONE, symptoms: [String: SymptomSeverity] = [:], moods: [String] = [],
                moodScore: Int = 0, energyLevel: Int = 0, sleepQuality: Int = 0, waterGlasses: Int = 0,
                exerciseNote: String = "", medicationNote: String = "", journalText: String = "",
                voiceNoteKey: String? = nil, updatedAtMillis: Int64 = 0) {
        self.epochDay = epochDay; self.flow = flow; self.symptoms = symptoms; self.moods = moods; self.moodScore = moodScore
        self.energyLevel = energyLevel; self.sleepQuality = sleepQuality; self.waterGlasses = waterGlasses
        self.exerciseNote = exerciseNote; self.medicationNote = medicationNote; self.journalText = journalText
        self.voiceNoteKey = voiceNoteKey; self.updatedAtMillis = updatedAtMillis
    }

    /// Same rule as Android's `DailyLog.isEmpty()`.
    public var isEmpty: Bool {
        flow == .NONE && symptoms.isEmpty && moods.isEmpty && moodScore == 0 && energyLevel == 0 && sleepQuality == 0 &&
            waterGlasses == 0 && exerciseNote.isBlank && medicationNote.isBlank && journalText.isBlank && voiceNoteKey == nil
    }
}

public struct PainLog: Equatable, Codable, Sendable, Identifiable {
    public var id: String
    public var epochDay: Int
    /// 1...10
    public var intensity: Int
    public var location: String
    public var painType: String
    public var durationHours: Int
    public var medicationTaken: String
    public var impactedWorkOrStudy: Bool
    public var impactedExercise: Bool
    public var impactedSleep: Bool
    public var notes: String
    public var createdAtMillis: Int64

    public init(id: String = "", epochDay: Int, intensity: Int, location: String, painType: String, durationHours: Int,
                medicationTaken: String, impactedWorkOrStudy: Bool, impactedExercise: Bool, impactedSleep: Bool,
                notes: String, createdAtMillis: Int64 = 0) {
        self.id = id; self.epochDay = epochDay; self.intensity = intensity; self.location = location; self.painType = painType
        self.durationHours = durationHours; self.medicationTaken = medicationTaken; self.impactedWorkOrStudy = impactedWorkOrStudy
        self.impactedExercise = impactedExercise; self.impactedSleep = impactedSleep; self.notes = notes
        self.createdAtMillis = createdAtMillis
    }
}

public struct CyclePrediction: Equatable, Sendable {
    public var hasRecordedCycles: Bool
    public var isCurrentlyMenstruating: Bool
    public var currentCycleDay: Int?
    public var currentPhase: CyclePhase
    public var averageCycleLength: Int
    public var averagePeriodDuration: Int
    public var variabilityDays: Int
    public var isIrregularEstimate: Bool
    public var nextPeriodStartEpochDay: Int?
    public var nextPeriodRangeMinDays: Int?
    public var nextPeriodRangeMaxDays: Int?
    public var predictedPeriodDays: Set<Int>
    public var estimatedFertileDays: Set<Int>
    public var estimatedOvulationDay: Int?
    public var loggedPeriodDays: Set<Int>
    public var estimateSummary: String
}

public struct PhaseMoodSummary: Equatable, Sendable {
    public var phase: CyclePhase
    public var topMoods: [String]
    public var averageEnergy: Double?
    public var topSymptoms: [String]
    public var sampleDays: Int
}

public struct PainPatternAlert: Equatable, Sendable {
    public var hasRecurringSeverePain: Bool
    public var consecutiveHighPainCycles: Int
    public var averageSevereIntensity: Double
    public var impactedActivitiesCount: Int
    public var summaryMessage: String
}

public struct NamedCount: Equatable, Sendable {
    public var name: String
    public var count: Int
    public init(_ name: String, _ count: Int) { self.name = name; self.count = count }
}

public struct InsightsData: Equatable, Sendable {
    public var completedCyclesCount: Int
    public var averageCycleLength: Int?
    public var averagePeriodDuration: Int?
    public var cycleVariabilityDays: Int?
    public var cycleLengthsHistory: [NamedCount]
    public var symptomFrequency: [NamedCount]
    public var moodFrequency: [NamedCount]
    public var averageEnergy: Double?
    public var averageSleep: Double?
    public var phaseMoodMap: [PhaseMoodSummary]
    public var painPatternAlert: PainPatternAlert?
    public var hasEnoughDataForTrends: Bool
    public var dataDrivenNotes: [String]
}

public enum TrackingCatalog {
    public static let defaultSymptoms = [
        "Cramps", "Headache", "Back pain", "Breast tenderness", "Bloating", "Acne", "Fatigue", "Nausea", "Diarrhea",
        "Constipation", "Dizziness", "Food cravings", "Appetite changes", "Sleep problems", "Mood changes",
    ]
    public static let defaultMoods = ["Happy", "Calm", "Energetic", "Neutral", "Emotional", "Sad", "Irritated", "Anxious", "Stressed", "Low"]
    public static let energyLabels: [(Int, String)] = [(1, "Very low"), (2, "Low"), (3, "Normal"), (4, "High"), (5, "Very high")]
    public static let painLocations = ["Lower abdomen", "Lower back", "Pelvic sides", "Thighs", "Head / temples", "Full body ache"]
    public static let painTypes = ["Cramping", "Dull ache", "Sharp / stabbing", "Throbbing", "Pressure / heaviness"]
    public static let moodFaceLabels = ["Rough", "Low", "Okay", "Good", "Great"]
    public static let sleepLabels = ["Poor", "Restless", "Okay", "Good", "Great"]
}

extension String {
    /// Kotlin's `isBlank()`.
    public var isBlank: Bool { trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
    public var trimmed: String { trimmingCharacters(in: .whitespacesAndNewlines) }
    /// Kotlin's `take(n)` for user text limits.
    public func take(_ n: Int) -> String { String(prefix(max(0, n))) }
}

extension Comparable {
    /// Kotlin's `coerceIn`.
    public func clamped(_ lo: Self, _ hi: Self) -> Self { min(max(self, lo), hi) }
}
