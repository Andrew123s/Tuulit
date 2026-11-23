import Foundation

/// Calendar helpers. Every record is stored as an epoch day: the *local calendar date* counted
/// from 1970-01-01, identical to Java's `LocalDate.toEpochDay()` used on Android. The conversion
/// goes through year/month/day (never through UTC timestamps) so both phones agree.
public enum Dates {
    /// Replaceable for tests.
    public static var now: () -> Date = { Date() }
    public static var calendar: Calendar = {
        var c = Calendar(identifier: .gregorian)
        c.timeZone = .current
        return c
    }()

    // MARK: civil date <-> epoch day (Howard Hinnant's algorithms, exact for the proleptic Gregorian calendar)

    public static func epochDay(year: Int, month: Int, day: Int) -> Int {
        let y = month <= 2 ? year - 1 : year
        let era = (y >= 0 ? y : y - 399) / 400
        let yoe = y - era * 400
        let mp = (month + 9) % 12
        let doy = (153 * mp + 2) / 5 + day - 1
        let doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146_097 + doe - 719_468
    }

    public static func civil(_ epochDay: Int) -> (year: Int, month: Int, day: Int) {
        let z = epochDay + 719_468
        let era = (z >= 0 ? z : z - 146_096) / 146_097
        let doe = z - era * 146_097
        let yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
        let doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        let mp = (5 * doy + 2) / 153
        let d = doy - (153 * mp + 2) / 5 + 1
        let m = mp < 10 ? mp + 3 : mp - 9
        return (yoe + era * 400 + (m <= 2 ? 1 : 0), m, d)
    }

    public static func epochDay(of date: Date) -> Int {
        let c = calendar.dateComponents([.year, .month, .day], from: date)
        return epochDay(year: c.year!, month: c.month!, day: c.day!)
    }

    /// Local midnight of an epoch day.
    public static func date(_ epochDay: Int) -> Date {
        let (y, m, d) = civil(epochDay)
        return calendar.date(from: DateComponents(year: y, month: m, day: d))!
    }

    public static func today() -> Int { epochDay(of: now()) }
    public static func hourNow() -> Int { calendar.component(.hour, from: now()) }
    public static func nowMillis() -> Int64 { Int64((now().timeIntervalSince1970 * 1000).rounded()) }
    public static func epochDay(ofMillis millis: Int64) -> Int { epochDay(of: Date(timeIntervalSince1970: Double(millis) / 1000)) }

    /// 0 = Monday ... 6 = Sunday (ISO), matching java.time.DayOfWeek - 1.
    public static func isoWeekdayIndex(_ epochDay: Int) -> Int { ((epochDay % 7) + 7 + 3) % 7 }

    // MARK: formatting (same patterns as Android)

    private static func formatter(_ pattern: String) -> DateFormatter {
        let f = DateFormatter()
        f.locale = .current
        f.calendar = calendar
        f.timeZone = calendar.timeZone
        f.dateFormat = pattern
        return f
    }

    public static func short(_ d: Int) -> String { formatter("d MMM").string(from: date(d)) }
    public static func day(_ d: Int) -> String { formatter("EEE d MMM").string(from: date(d)) }
    public static func long(_ d: Int) -> String { formatter("EEEE d MMMM").string(from: date(d)) }
    public static func iso(_ d: Int) -> String {
        let (y, m, dd) = civil(d)
        return String(format: "%04d-%02d-%02d", y, m, dd)
    }
    public static func monthTitle(year: Int, month: Int) -> String {
        formatter("MMMM yyyy").string(from: date(epochDay(year: year, month: month, day: 1)))
    }
}

/// A calendar month, the Swift equivalent of java.time.YearMonth used by the calendar screens.
public struct YearMonth: Hashable, Comparable, Sendable {
    public var year: Int
    public var month: Int
    public init(year: Int, month: Int) { self.year = year; self.month = month }

    public static func of(epochDay: Int) -> YearMonth { let c = Dates.civil(epochDay); return YearMonth(year: c.year, month: c.month) }
    public static func now() -> YearMonth { of(epochDay: Dates.today()) }

    public func plus(months n: Int) -> YearMonth {
        let total = year * 12 + (month - 1) + n
        let y = total >= 0 ? total / 12 : (total - 11) / 12
        return YearMonth(year: y, month: total - y * 12 + 1)
    }

    public var firstDay: Int { Dates.epochDay(year: year, month: month, day: 1) }
    public var length: Int { plus(months: 1).firstDay - firstDay }
    public var title: String { Dates.monthTitle(year: year, month: month) }

    public static func < (a: YearMonth, b: YearMonth) -> Bool { (a.year, a.month) < (b.year, b.month) }
}
