import SwiftUI
import UIKit
import TuulitCore

extension Color {
    init(hex: UInt32, alpha: Double = 1) {
        self.init(.sRGB, red: Double((hex >> 16) & 0xff) / 255, green: Double((hex >> 8) & 0xff) / 255,
                  blue: Double(hex & 0xff) / 255, opacity: alpha)
    }
}

/// Colour tokens, one-to-one with Android `Theme.kt` (v2 light-first palette and Warm dark).
struct Palette {
    let cream, oat, sand, tan, hairline: Color
    let ink, inkMuted, inkFaint, onInk: Color
    let toffee, banana, onBanana, bananaSoft: Color
    let period, periodSoft, periodDeep, onPeriod: Color
    let fertile, fertileSoft, lilacDeep: Color
    let peach, peachSoft, rose, roseSoft: Color
    let alert, alertSoft, focus, scrim: Color
    let isDark: Bool

    static let light = Palette(
        cream: Color(hex: 0xFFF8F0), oat: Color(hex: 0xFFFDF9), sand: Color(hex: 0xFBEEDD), tan: Color(hex: 0xF2D9BE), hairline: Color(hex: 0xF0E2D0),
        ink: Color(hex: 0x2B2320), inkMuted: Color(hex: 0x6B5A50), inkFaint: Color(hex: 0x7D6857), onInk: Color(hex: 0xFFF8F0),
        toffee: Color(hex: 0x9A5B3C), banana: Color(hex: 0xF7C948), onBanana: Color(hex: 0x2B2320), bananaSoft: Color(hex: 0xFFF0C2),
        period: Color(hex: 0xE2725B), periodSoft: Color(hex: 0xFDDDD3), periodDeep: Color(hex: 0xB5482F), onPeriod: Color(hex: 0x2B2320),
        fertile: Color(hex: 0x9C88C9), fertileSoft: Color(hex: 0xECE5F7), lilacDeep: Color(hex: 0x6F5AA8),
        peach: Color(hex: 0xFFC9AE), peachSoft: Color(hex: 0xFFE7DA), rose: Color(hex: 0xE7A6B0), roseSoft: Color(hex: 0xF9E1E4),
        alert: Color(hex: 0xB3261E), alertSoft: Color(hex: 0xFCE3DF), focus: Color(hex: 0x2B2320), scrim: Color(hex: 0x2B2320, alpha: 0.32),
        isDark: false)

    static let dark = Palette(
        cream: Color(hex: 0x1C1517), oat: Color(hex: 0x261D20), sand: Color(hex: 0x33272A), tan: Color(hex: 0x4B3A37), hairline: Color(hex: 0x3D2F31),
        ink: Color(hex: 0xF8EEE6), inkMuted: Color(hex: 0xCDBAB0), inkFaint: Color(hex: 0xA8958B), onInk: Color(hex: 0x1C1517),
        toffee: Color(hex: 0xF0B48C), banana: Color(hex: 0xF7CF5C), onBanana: Color(hex: 0x2B2320), bananaSoft: Color(hex: 0x3E3320),
        period: Color(hex: 0xF08A73), periodSoft: Color(hex: 0x4A2A26), periodDeep: Color(hex: 0xF5A08C), onPeriod: Color(hex: 0x2B2320),
        fertile: Color(hex: 0xB7A6E0), fertileSoft: Color(hex: 0x332C45), lilacDeep: Color(hex: 0xC9BCEB),
        peach: Color(hex: 0xF4B69A), peachSoft: Color(hex: 0x45302A), rose: Color(hex: 0xE7A6B0), roseSoft: Color(hex: 0x452E34),
        alert: Color(hex: 0xF2998F), alertSoft: Color(hex: 0x4A2522), focus: Color(hex: 0xF7CF5C), scrim: Color(hex: 0x000000, alpha: 0.6),
        isDark: true)
}

/// Mascot art colours are fixed in both themes; the mark is a picture, not UI.
enum MascotColors {
    static let banana = Color(hex: 0xF7C948)
    static let tan = Color(hex: 0xF2CFA8)
    static let flesh = Color(hex: 0xFFFBF3)
    static let outline = Color(hex: 0x3A2A24)
    static let prop = Color(hex: 0xE2725B)
    static let blush = Color(hex: 0xF49A8C, alpha: 0.4)
}

private struct PaletteKey: EnvironmentKey { static let defaultValue = Palette.light }
private struct ReducedMotionKey: EnvironmentKey { static let defaultValue = false }

extension EnvironmentValues {
    var palette: Palette { get { self[PaletteKey.self] } set { self[PaletteKey.self] = newValue } }
    /// True when the system *or* the in-app setting asks for reduced motion.
    var tuulitReducedMotion: Bool { get { self[ReducedMotionKey.self] } set { self[ReducedMotionKey.self] = newValue } }
}

enum Radius { static let sm: CGFloat = 10, md: CGFloat = 16, lg: CGFloat = 28, xl: CGFloat = 32 }

// MARK: - Typography (Fraunces + Plus Jakarta Sans variable fonts, scaled with Dynamic Type)

enum TStyle: CaseIterable {
    case numeral, display, headline, title, subtitle, body, bodySm, label, overline, caption, tab

    fileprivate var spec: (serif: Bool, size: CGFloat, weight: CGFloat, opsz: CGFloat, tracking: CGFloat, line: CGFloat, text: UIFont.TextStyle) {
        switch self {
        case .numeral: return (true, 64, 380, 96, -1.5, 64, .largeTitle)
        case .display: return (true, 34, 400, 72, -0.6, 38, .largeTitle)
        case .headline: return (true, 26, 450, 48, -0.3, 31, .title1)
        case .title: return (true, 20, 500, 24, -0.1, 26, .title3)
        case .subtitle: return (false, 17, 600, 0, 0, 24, .headline)
        case .body: return (false, 16, 400, 0, 0, 24, .body)
        case .bodySm: return (false, 14, 400, 0, 0, 20, .subheadline)
        case .label: return (false, 14, 600, 0, 0.1, 18, .subheadline)
        case .overline: return (false, 12, 600, 0, 0.8, 16, .caption1)
        case .caption: return (false, 12, 500, 0, 0, 16, .caption1)
        case .tab: return (false, 11, 600, 0, 0, 14, .caption2)
        }
    }

    var tracking: CGFloat { spec.tracking }
    var lineSpacing: CGFloat { max(0, spec.line - spec.size) }

    private static func tag(_ s: String) -> NSNumber {
        NSNumber(value: s.unicodeScalars.reduce(UInt32(0)) { ($0 << 8) | $1.value })
    }

    private static var cache: [String: UIFont] = [:]

    /// The UIFont for this style at the given content size, with the variable axes set like Android.
    func uiFont(_ category: UIContentSizeCategory = UIApplication.shared.preferredContentSizeCategory) -> UIFont {
        let key = "\(self)-\(category.rawValue)"
        if let f = TStyle.cache[key] { return f }
        let s = spec
        var variations: [NSNumber: CGFloat] = [TStyle.tag("wght"): s.weight]
        if s.serif { variations[TStyle.tag("opsz")] = s.opsz; variations[TStyle.tag("SOFT")] = 50 }
        let desc = UIFontDescriptor(fontAttributes: [
            .family: s.serif ? "Fraunces" : "Plus Jakarta Sans",
            UIFontDescriptor.AttributeName(rawValue: kCTFontVariationAttribute as String): variations,
        ])
        let base = UIFont(descriptor: desc, size: s.size)
        let traits = UITraitCollection(preferredContentSizeCategory: category)
        let scaled = UIFontMetrics(forTextStyle: s.text).scaledFont(for: base, maximumPointSize: s.size * 2.2, compatibleWith: traits)
        TStyle.cache[key] = scaled
        return scaled
    }
}

private extension DynamicTypeSize {
    var category: UIContentSizeCategory {
        switch self {
        case .xSmall: return .extraSmall
        case .small: return .small
        case .medium: return .medium
        case .large: return .large
        case .xLarge: return .extraLarge
        case .xxLarge: return .extraExtraLarge
        case .xxxLarge: return .extraExtraExtraLarge
        case .accessibility1: return .accessibilityMedium
        case .accessibility2: return .accessibilityLarge
        case .accessibility3: return .accessibilityExtraLarge
        case .accessibility4: return .accessibilityExtraExtraLarge
        case .accessibility5: return .accessibilityExtraExtraExtraLarge
        @unknown default: return .large
        }
    }
}

private struct TStyleModifier: ViewModifier {
    let style: TStyle
    let color: Color?
    @Environment(\.dynamicTypeSize) private var dts
    @Environment(\.palette) private var c

    func body(content: Content) -> some View {
        content
            .font(Font(style.uiFont(dts.category) as CTFont))
            .tracking(style.tracking)
            .lineSpacing(style.lineSpacing)
            .foregroundStyle(color ?? c.ink)
    }
}

extension View {
    /// Applies a Tuulit type style and colour (ink by default).
    func tStyle(_ style: TStyle, _ color: Color? = nil) -> some View { modifier(TStyleModifier(style: style, color: color)) }
}

/// Shorthand for styled text, the equivalent of Android's `T(...)`.
struct T: View {
    let text: String
    var style: TStyle = .body
    var color: Color? = nil
    var align: TextAlignment = .leading
    var lines: Int? = nil

    init(_ text: String, _ style: TStyle = .body, _ color: Color? = nil, align: TextAlignment = .leading, lines: Int? = nil) {
        self.text = text; self.style = style; self.color = color; self.align = align; self.lines = lines
    }

    var body: some View {
        Text(style == .overline ? text.uppercased() : text)
            .tStyle(style, color)
            .multilineTextAlignment(align)
            .lineLimit(lines)
            .fixedSize(horizontal: false, vertical: lines == nil)
    }
}

/// Phase colours: menstrual coral, follicular peach, ovulation lilac, luteal dusty rose.
struct PhaseStyle { let fill: Color; let soft: Color; let label: String }

extension Palette {
    func phase(_ p: CyclePhase) -> PhaseStyle {
        switch p {
        case .MENSTRUAL: return PhaseStyle(fill: period, soft: periodSoft, label: "Period")
        case .FOLLICULAR: return PhaseStyle(fill: peach, soft: peachSoft, label: "Follicular")
        case .ESTIMATED_OVULATION: return PhaseStyle(fill: fertile, soft: fertileSoft, label: "Ovulation")
        case .LUTEAL: return PhaseStyle(fill: rose, soft: roseSoft, label: "Luteal")
        case .UNKNOWN: return PhaseStyle(fill: banana, soft: bananaSoft, label: "Getting started")
        }
    }

    func moodColor(_ score: Int) -> Color { [fertileSoft, roseSoft, sand, peachSoft, bananaSoft][(score - 1).clamped(0, 4)] }
    func moodColorStrong(_ score: Int) -> Color { [fertile, rose, tan, peach, banana][(score - 1).clamped(0, 4)] }
}
