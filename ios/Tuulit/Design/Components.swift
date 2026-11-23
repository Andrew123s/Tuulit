import SwiftUI
import UIKit
import TuulitCore

// Tuulit components, ported from Android `Components.kt` and adapted to iOS conventions
// (44 pt touch targets, native toggles tinted coral, haptics on key actions, Dynamic Type).

enum ButtonKind { case primary, banana, quiet, text, danger }

struct TButton: View {
    let title: String
    var kind: ButtonKind = .primary
    var icon: IconDef? = nil
    var small = false
    var loading = false
    var enabled = true
    var fullWidth = false
    let action: () -> Void
    @Environment(\.palette) private var c

    init(_ title: String, kind: ButtonKind = .primary, icon: IconDef? = nil, small: Bool = false, loading: Bool = false,
         enabled: Bool = true, fullWidth: Bool = false, action: @escaping () -> Void) {
        self.title = title; self.kind = kind; self.icon = icon; self.small = small; self.loading = loading
        self.enabled = enabled; self.fullWidth = fullWidth; self.action = action
    }

    private var colors: (bg: Color, fg: Color) {
        switch kind {
        case .primary: return (c.ink, c.onInk)
        case .banana: return (c.banana, c.onBanana)
        case .quiet: return (c.sand, c.ink)
        case .text: return (.clear, c.toffee)
        case .danger: return (c.alert, c.onInk)
        }
    }

    var body: some View {
        Button(action: { if enabled && !loading { action() } }) {
            HStack(spacing: 8) {
                if loading { ProgressView().tint(colors.fg) }
                else if let icon { TIcon(icon, size: small ? 16 : 18, color: colors.fg) }
                T(title, .label, colors.fg, lines: 2)
            }
            .padding(.horizontal, kind == .text ? 6 : (small ? 14 : 22))
            .frame(minHeight: small ? 40 : (kind == .text ? 44 : 52))
            .frame(maxWidth: fullWidth ? .infinity : nil)
            .background(colors.bg, in: Capsule())
            .contentShape(Capsule())
        }
        .buttonStyle(PressScale())
        .opacity(enabled ? 1 : 0.38)
        .disabled(!enabled || loading)
        .accessibilityLabel(title)
    }
}

/// Gentle press feedback; respects Reduce Motion.
struct PressScale: ButtonStyle {
    @Environment(\.tuulitReducedMotion) private var reduced
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed && !reduced ? 0.96 : 1)
            .animation(reduced ? nil : .easeOut(duration: 0.12), value: configuration.isPressed)
    }
}

struct IconBtn: View {
    let icon: IconDef
    let label: String
    var filled = false
    var enabled = true
    let action: () -> Void
    @Environment(\.palette) private var c

    init(_ icon: IconDef, _ label: String, filled: Bool = false, enabled: Bool = true, action: @escaping () -> Void) {
        self.icon = icon; self.label = label; self.filled = filled; self.enabled = enabled; self.action = action
    }

    var body: some View {
        Button(action: action) {
            TIcon(icon, size: 20, color: filled ? c.onInk : c.ink)
                .frame(width: 44, height: 44)
                .background(filled ? c.ink : c.sand, in: Circle())
        }
        .buttonStyle(PressScale())
        .opacity(enabled ? 1 : 0.38)
        .disabled(!enabled)
        .accessibilityLabel(label)
    }
}

struct Chip: View {
    let label: String
    var selected = false
    let action: () -> Void
    @Environment(\.palette) private var c

    init(_ label: String, selected: Bool = false, action: @escaping () -> Void) { self.label = label; self.selected = selected; self.action = action }

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                if selected { TIcon(Icons.check, size: 14, color: c.ink) }
                T(label, .label, c.ink, lines: 1)
            }
            .padding(.horizontal, 16)
            .frame(minHeight: 40)
            .background(selected ? c.peach : c.sand, in: Capsule())
        }
        .buttonStyle(PressScale())
        .accessibilityAddTraits(selected ? [.isSelected] : [])
    }
}

/// Wrapping row, the equivalent of Compose FlowRow.
struct FlowLayout: Layout {
    var spacing: CGFloat = 8
    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxW = proposal.width ?? .infinity
        var x: CGFloat = 0, y: CGFloat = 0, rowH: CGFloat = 0, widest: CGFloat = 0
        for v in subviews {
            let s = v.sizeThatFits(.unspecified)
            if x > 0 && x + s.width > maxW { y += rowH + spacing; x = 0; rowH = 0 }
            x += s.width + spacing; rowH = max(rowH, s.height); widest = max(widest, x - spacing)
        }
        return CGSize(width: proposal.width ?? widest, height: y + rowH)
    }
    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX, y = bounds.minY, rowH: CGFloat = 0
        for v in subviews {
            let s = v.sizeThatFits(.unspecified)
            if x > bounds.minX && x + s.width > bounds.maxX { y += rowH + spacing; x = bounds.minX; rowH = 0 }
            v.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(s))
            x += s.width + spacing; rowH = max(rowH, s.height)
        }
    }
}

struct Segmented: View {
    let options: [String]
    @Binding var selection: Int
    @Environment(\.palette) private var c

    var body: some View {
        HStack(spacing: 4) {
            ForEach(options.indices, id: \.self) { i in
                Button { selection = i } label: {
                    T(options[i], .label, selection == i ? c.ink : c.inkMuted, lines: 1)
                        .frame(maxWidth: .infinity, minHeight: 40)
                        .background(selection == i ? c.oat : .clear, in: Capsule())
                        .contentShape(Capsule())
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(selection == i ? [.isSelected] : [])
            }
        }
        .padding(4)
        .background(c.sand, in: Capsule())
        .animation(.easeOut(duration: 0.15), value: selection)
    }
}

struct Hairline: View {
    @Environment(\.palette) private var c
    var body: some View { Rectangle().fill(c.hairline).frame(height: 1) }
}

struct Overline: View {
    let text: String
    var color: Color? = nil
    @Environment(\.palette) private var c
    init(_ text: String, _ color: Color? = nil) { self.text = text; self.color = color }
    var body: some View { T(text, .overline, color ?? c.inkMuted) }
}

struct GroupTitle: View {
    let text: String
    @Environment(\.palette) private var c
    init(_ text: String) { self.text = text }
    var body: some View { T(text, .overline, c.inkMuted).padding(.top, 8).padding(.bottom, 4).accessibilityAddTraits(.isHeader) }
}

struct SettingRow: View {
    let title: String
    var sub: String? = nil
    let isOn: Bool
    var last = false
    let onChange: (Bool) -> Void
    @Environment(\.palette) private var c

    var body: some View {
        VStack(spacing: 0) {
            Toggle(isOn: Binding(get: { isOn }, set: onChange)) {
                VStack(alignment: .leading, spacing: 2) {
                    T(title, .subtitle)
                    if let sub { T(sub, .bodySm, c.inkMuted) }
                }
            }
            .tint(c.period)
            .padding(.vertical, 12)
            if !last { Hairline() }
        }
    }
}

struct ListRow<Leading: View, Trailing: View>: View {
    let title: String
    var sub: String? = nil
    var icon: IconDef? = nil
    var trailingText: String? = nil
    var chevron = false
    var last = false
    var titleColor: Color? = nil
    var action: (() -> Void)? = nil
    @ViewBuilder var leading: () -> Leading
    @ViewBuilder var trailing: () -> Trailing
    @Environment(\.palette) private var c

    private var tile: Color {
        let tiles = [c.peachSoft, c.fertileSoft, c.bananaSoft, c.roseSoft, c.periodSoft]
        let h = title.unicodeScalars.reduce(0) { ($0 &* 31 &+ Int($1.value)) & 0x7fffffff }
        return tiles[h % tiles.count]
    }

    var content: some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                leading()
                if let icon {
                    TIcon(icon, size: 20, color: c.ink)
                        .frame(width: 40, height: 40)
                        .background(tile, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
                VStack(alignment: .leading, spacing: 2) {
                    T(title, .body, titleColor ?? c.ink).fontWeight(.semibold)
                    if let sub { T(sub, .bodySm, c.inkMuted) }
                }
                Spacer(minLength: 8)
                if let trailingText { T(trailingText, .bodySm, c.inkMuted, lines: 1) }
                trailing()
                if chevron { TIcon(Icons.chevR, size: 18, color: c.inkFaint) }
            }
            .padding(.vertical, 12)
            .frame(minHeight: 56)
            .contentShape(Rectangle())
            if !last { Hairline() }
        }
    }

    var body: some View {
        if let action {
            Button(action: action) { content }.buttonStyle(.plain)
        } else {
            content.accessibilityElement(children: .combine)
        }
    }
}

extension ListRow where Leading == EmptyView, Trailing == EmptyView {
    init(_ title: String, sub: String? = nil, icon: IconDef? = nil, trailingText: String? = nil, chevron: Bool = false, last: Bool = false,
         titleColor: Color? = nil, action: (() -> Void)? = nil) {
        self.init(title: title, sub: sub, icon: icon, trailingText: trailingText, chevron: chevron, last: last, titleColor: titleColor,
                  action: action, leading: { EmptyView() }, trailing: { EmptyView() })
    }
}

extension ListRow where Trailing == EmptyView {
    init(_ title: String, sub: String? = nil, chevron: Bool = false, last: Bool = false, action: (() -> Void)? = nil,
         @ViewBuilder leading: @escaping () -> Leading) {
        self.init(title: title, sub: sub, chevron: chevron, last: last, action: action, leading: leading, trailing: { EmptyView() })
    }
}

extension ListRow where Leading == EmptyView {
    init(_ title: String, sub: String? = nil, icon: IconDef? = nil, last: Bool = false, action: (() -> Void)? = nil,
         @ViewBuilder trailing: @escaping () -> Trailing) {
        self.init(title: title, sub: sub, icon: icon, last: last, action: action, leading: { EmptyView() }, trailing: trailing)
    }
}

struct Panel<Content: View>: View {
    var padding: CGFloat = 20
    var color: Color? = nil
    @ViewBuilder var content: () -> Content
    @Environment(\.palette) private var c
    var body: some View {
        VStack(alignment: .leading, spacing: 0) { content() }
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(color ?? c.oat, in: RoundedRectangle(cornerRadius: Radius.lg, style: .continuous))
    }
}

enum NoticeKind { case info, medical }

struct Notice<Action: View>: View {
    let title: String
    let message: String
    var kind: NoticeKind = .info
    var icon: IconDef? = nil
    @ViewBuilder var action: () -> Action
    @Environment(\.palette) private var c

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            TIcon(icon ?? (kind == .medical ? Icons.alert : Icons.info), size: 22, color: kind == .medical ? c.alert : c.toffee)
            VStack(alignment: .leading, spacing: 4) {
                T(title, .subtitle)
                T(message, .bodySm, c.inkMuted)
                action()
            }
            Spacer(minLength: 0)
        }
        .padding(16)
        .background(kind == .medical ? c.alertSoft : c.sand, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .accessibilityElement(children: .contain)
    }
}

extension Notice where Action == EmptyView {
    init(_ title: String, _ message: String, kind: NoticeKind = .info, icon: IconDef? = nil) {
        self.init(title: title, message: message, kind: kind, icon: icon, action: { EmptyView() })
    }
}

struct Field: View {
    let label: String
    @Binding var text: String
    var placeholder = ""
    var multiline = false
    var secure = false
    var error: String? = nil
    var helper: String? = nil
    var keyboard: UIKeyboardType = .default
    var contentType: UITextContentType? = nil
    var bare = false
    var submitLabel: SubmitLabel = .done
    var onSubmit: () -> Void = {}
    @State private var reveal = false
    @Environment(\.palette) private var c

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            if !label.isEmpty { T(label, .label, c.inkMuted) }
            HStack {
                Group {
                    if secure && !reveal {
                        SecureField(placeholder, text: $text)
                    } else if multiline {
                        TextField(placeholder, text: $text, axis: .vertical).lineLimit(3...8)
                    } else {
                        TextField(placeholder, text: $text)
                    }
                }
                .tStyle(.body)
                .keyboardType(keyboard)
                .textContentType(contentType)
                .textInputAutocapitalization(keyboard == .emailAddress || secure ? .never : .sentences)
                .autocorrectionDisabled(keyboard == .emailAddress || secure)
                .submitLabel(submitLabel)
                .onSubmit(onSubmit)
                .accessibilityLabel(label.isEmpty ? placeholder : label)
                if secure {
                    Button { reveal.toggle() } label: { TIcon(Icons.eye, size: 20, color: c.inkMuted).frame(width: 44, height: 44) }
                        .accessibilityLabel(reveal ? "Hide password" : "Show password")
                }
            }
            .padding(.horizontal, bare ? 12 : 16)
            .padding(.vertical, multiline ? 12 : 0)
            .frame(minHeight: 52)
            .background(bare ? Color.clear : c.sand, in: RoundedRectangle(cornerRadius: Radius.md, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: Radius.md).stroke(error == nil ? .clear : c.alert, lineWidth: 1.5))
            if let error { T(error, .caption, c.alert) }
            else if let helper { T(helper, .caption, c.inkFaint) }
        }
    }
}

struct Avatar: View {
    let name: String
    var size: CGFloat = 40
    var action: (() -> Void)? = nil
    @Environment(\.palette) private var c

    private var initials: String {
        let parts = name.split(separator: " ").prefix(2)
        return parts.compactMap { $0.first }.map(String.init).joined().uppercased()
    }

    var body: some View {
        let face = ZStack {
            Circle().fill(c.peachSoft)
            if name.isBlank || name == "You" { TIcon(Icons.user, size: size * 0.48, color: c.periodDeep) }
            else { T(initials, size > 48 ? .title : .label, c.periodDeep) }
        }
        .frame(width: size, height: size)
        if let action {
            Button(action: action) { face }.buttonStyle(PressScale()).accessibilityLabel("Profile and settings")
        } else {
            face.accessibilityHidden(true)
        }
    }
}

/// Segmented progress (onboarding steps, quiz progress, session steps).
struct StepProgress: View {
    let steps: Int
    let done: Int
    @Environment(\.palette) private var c
    var body: some View {
        HStack(spacing: 6) {
            ForEach(0..<steps, id: \.self) { i in
                Capsule().fill(i < done ? c.period : c.sand).frame(height: 4)
            }
        }
        .accessibilityElement()
        .accessibilityLabel("Step \(done) of \(steps)")
    }
}

struct EmptyState: View {
    let title: String
    let message: String
    var mood: MascotMood? = .chill
    var icon: IconDef? = nil
    @Environment(\.palette) private var c
    var body: some View {
        VStack(spacing: 10) {
            if let mood { Mascot(mood, size: 88) }
            else if let icon { TIcon(icon, size: 32, color: c.inkMuted).frame(width: 64, height: 64).background(c.sand, in: Circle()) }
            T(title, .title, align: .center)
            T(message, .bodySm, c.inkMuted, align: .center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 28)
    }
}

// MARK: - Calendar

struct DayMarks: Equatable {
    var period = false, predicted = false, fertile = false, ovulation = false, logged = false, today = false

    static func of(_ day: Int, _ p: CyclePrediction, logged: Set<Int>, today: Int) -> DayMarks {
        DayMarks(period: p.loggedPeriodDays.contains(day), predicted: p.predictedPeriodDays.contains(day),
                 fertile: p.estimatedFertileDays.contains(day) || day == p.estimatedOvulationDay,
                 ovulation: day == p.estimatedOvulationDay, logged: logged.contains(day), today: day == today)
    }

    var describe: String {
        [today ? "today" : nil, period ? "period" : nil, predicted && !period ? "predicted period" : nil,
         ovulation ? "estimated ovulation" : nil, fertile && !ovulation ? "fertile window estimate" : nil, logged ? "has a log" : nil]
            .compactMap { $0 }.joined(separator: ", ")
    }
}

struct MonthHeader: View {
    let month: YearMonth
    var nextEnabled = true
    let onPrev: () -> Void
    let onNext: () -> Void
    var body: some View {
        HStack {
            T(month.title, .title).accessibilityAddTraits(.isHeader)
            Spacer()
            IconBtn(Icons.chevL, "Previous month", action: onPrev)
            IconBtn(Icons.chevR, "Next month", enabled: nextEnabled, action: onNext)
        }
        .padding(.bottom, 8)
    }
}

/// Month grid with round, soft day cells (Android `MonthGrid`). Long-press logs a day.
struct MonthGrid: View {
    let month: YearMonth
    let marks: (Int) -> DayMarks
    var selected: Int? = nil
    var disableAfter: Int? = nil
    let onSelect: (Int) -> Void
    var onLongPress: ((Int) -> Void)? = nil
    @Environment(\.palette) private var c

    var body: some View {
        let first = month.firstDay
        let lead = Dates.isoWeekdayIndex(first)
        let cells = lead + month.length
        let rows = (cells + 6) / 7
        VStack(spacing: 6) {
            HStack(spacing: 0) {
                ForEach(["M", "T", "W", "T", "F", "S", "S"].indices, id: \.self) { i in
                    T(["M", "T", "W", "T", "F", "S", "S"][i], .caption, c.inkFaint, align: .center).frame(maxWidth: .infinity).accessibilityHidden(true)
                }
            }
            ForEach(0..<rows, id: \.self) { r in
                HStack(spacing: 0) {
                    ForEach(0..<7, id: \.self) { col in
                        let idx = r * 7 + col - lead
                        if idx >= 0 && idx < month.length { dayCell(first + idx).frame(maxWidth: .infinity) }
                        else { Color.clear.frame(maxWidth: .infinity, minHeight: 44) }
                    }
                }
            }
        }
    }

    @ViewBuilder private func dayCell(_ day: Int) -> some View {
        let m = marks(day)
        let disabled = disableAfter.map { day > $0 } ?? false
        let fill: Color = m.period ? c.period : m.ovulation ? c.fertile : m.predicted ? c.periodSoft : m.fertile ? c.fertileSoft : (selected == day ? c.tan : .clear)
        let number = Dates.civil(day).day
        ZStack {
            Circle().fill(fill)
            if m.today { Circle().stroke(c.banana, lineWidth: 3) }
            if selected == day && fill != c.tan { Circle().stroke(c.ink, lineWidth: 1.5).padding(2) }
            VStack(spacing: 1) {
                T("\(number)", .label, m.period ? c.onPeriod : (disabled ? c.inkFaint.opacity(0.5) : c.ink))
                Circle().fill(m.logged ? c.toffee : .clear).frame(width: 4, height: 4)
            }
        }
        .frame(width: 42, height: 42)
        .frame(minHeight: 44)
        .contentShape(Rectangle())
        .onTapGesture { if !disabled { onSelect(day) } }
        .onLongPressGesture { if !disabled { onLongPress?(day) } }
        .accessibilityElement()
        .accessibilityLabel("\(Dates.long(day))\(m.describe.isEmpty ? "" : ", " + m.describe)")
        .accessibilityAddTraits(selected == day ? [.isButton, .isSelected] : .isButton)
        .accessibilityAction { if !disabled { onSelect(day) } }
        .accessibilityAction(named: "Log this day") { if !disabled { onLongPress?(day) } }
    }
}

struct Legend: View {
    @Environment(\.palette) private var c
    var body: some View {
        FlowLayout(spacing: 8) {
            item("Period", c.period)
            item("Predicted", c.periodSoft)
            item("Fertile (estimate)", c.fertileSoft)
            item("Ovulation", c.fertile)
            item("Today", .clear, ring: c.banana)
        }
    }
    private func item(_ label: String, _ fill: Color, ring: Color? = nil) -> some View {
        HStack(spacing: 6) {
            Circle().fill(fill).frame(width: 12, height: 12).overlay(Circle().stroke(ring ?? .clear, lineWidth: 2))
            T(label, .caption, c.inkMuted)
        }
        .padding(.horizontal, 10).padding(.vertical, 6)
        .background(c.oat, in: Capsule())
    }
}

// MARK: - PIN

struct PinDots: View {
    let filled: Int
    let total: Int
    @Environment(\.palette) private var c
    var body: some View {
        HStack(spacing: 14) {
            ForEach(0..<total, id: \.self) { i in
                Circle().fill(i < filled ? c.period : .clear).overlay(Circle().stroke(i < filled ? .clear : c.tan, lineWidth: 1.5)).frame(width: 14, height: 14)
            }
        }
        .accessibilityElement()
        .accessibilityLabel("\(filled) digits entered")
    }
}

struct Keypad: View {
    let onDigit: (String) -> Void
    let onDelete: () -> Void
    let onSubmit: () -> Void
    @Environment(\.palette) private var c

    var body: some View {
        let keys = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "OK", "0", "DEL"]
        VStack(spacing: 14) {
            ForEach(0..<4, id: \.self) { r in
                HStack {
                    ForEach(0..<3, id: \.self) { col in
                        let k = keys[r * 3 + col]
                        Spacer()
                        switch k {
                        case "OK": TButton("Enter", kind: .text, action: onSubmit).frame(width: 72, height: 72)
                        case "DEL": TButton("Delete", kind: .text, action: onDelete).frame(width: 72, height: 72)
                        default:
                            Button { onDigit(k) } label: { T(k, .headline).frame(width: 72, height: 72).background(c.sand, in: Circle()) }
                                .buttonStyle(PressScale())
                                .accessibilityLabel(k)
                        }
                        Spacer()
                    }
                }
            }
        }
    }
}

// MARK: - Screen scaffolds

/// Scrolling screen with the standard 20 pt gutters and a width cap for large phones.
struct ScreenScroll<Content: View>: View {
    var bottomPad: CGFloat = 120
    @ViewBuilder var content: () -> Content
    @Environment(\.palette) private var c
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) { content() }
                .padding(.horizontal, 20)
                .padding(.bottom, bottomPad)
                .frame(maxWidth: 600)
                .frame(maxWidth: .infinity)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(c.cream.ignoresSafeArea())
    }
}

/// Top bar for pushed screens (the system back swipe still works).
struct AppBar<Action: View>: View {
    var title = ""
    var close = false
    let onBack: (() -> Void)?
    @ViewBuilder var action: () -> Action

    var body: some View {
        HStack {
            if let onBack { IconBtn(close ? Icons.close : Icons.back, close ? "Close" : "Back", action: onBack) }
            else { Color.clear.frame(width: 44, height: 44) }
            Spacer()
            T(title, .subtitle, lines: 1).accessibilityAddTraits(.isHeader)
            Spacer()
            action().frame(minWidth: 44)
        }
        .frame(height: 56)
    }
}

extension AppBar where Action == EmptyView {
    init(_ title: String = "", close: Bool = false, onBack: (() -> Void)?) {
        self.init(title: title, close: close, onBack: onBack, action: { EmptyView() })
    }
}
