import SwiftUI
import TuulitCore

/*
 Tuulit illustration system, ported shape for shape from Android `Illustrations.kt`: flat,
 rounded shapes from the palette, no outlines (except the mascot), at most three colours each.
 */

/// A soft organic blob used behind heroes.
struct Blob: Shape {
    var seed = 0
    func path(in r: CGRect) -> Path {
        let base: [CGFloat] = [0.06, -0.04, 0.05, -0.06, 0.03, -0.05]
        let k = seed % base.count
        let wob = Array(base[k...] + base[..<k])
        let n = 6
        let pts = (0..<n).map { i -> CGPoint in
            let a = CGFloat(i) / CGFloat(n) * 2 * .pi
            let rr = 0.5 + wob[i]
            return CGPoint(x: r.midX + cos(a) * r.width * rr, y: r.midY + sin(a) * r.height * rr)
        }
        var p = Path()
        p.move(to: CGPoint(x: (pts[0].x + pts[n - 1].x) / 2, y: (pts[0].y + pts[n - 1].y) / 2))
        for i in 0..<n {
            let cur = pts[i], nxt = pts[(i + 1) % n]
            p.addQuadCurve(to: CGPoint(x: (cur.x + nxt.x) / 2, y: (cur.y + nxt.y) / 2), control: cur)
        }
        p.closeSubpath()
        return p
    }
}

/// The cycle ring: one arc per day of the cycle, tinted by phase; today is a banana knob.
struct CycleRing<Content: View>: View {
    let prediction: CyclePrediction
    let cycleStart: Int?
    let today: Int
    var size: CGFloat = 236
    @ViewBuilder var content: () -> Content
    @Environment(\.palette) private var c

    var body: some View {
        let length = prediction.averageCycleLength.clamped(21, 45)
        let start = cycleStart ?? (today - ((prediction.currentCycleDay ?? 1) - 1))
        ZStack {
            Canvas { ctx, sz in
                let stroke = min(sz.width, sz.height) * 0.085
                let radius = min(sz.width, sz.height) / 2 - stroke
                let center = CGPoint(x: sz.width / 2, y: sz.height / 2)
                let gap = 1.6, sweep = 360.0 / Double(length)
                for i in 0..<length {
                    let day = start + i
                    let color: Color
                    if prediction.loggedPeriodDays.contains(day) { color = c.period }
                    else if prediction.predictedPeriodDays.contains(day) { color = c.periodSoft }
                    else if day == prediction.estimatedOvulationDay { color = c.fertile }
                    else if prediction.estimatedFertileDays.contains(day) { color = c.fertileSoft }
                    else if day < today { color = c.tan }
                    else { color = c.sand }
                    var p = Path()
                    p.addArc(center: center, radius: radius, startAngle: .degrees(-90 + Double(i) * sweep + gap / 2),
                             endAngle: .degrees(-90 + Double(i + 1) * sweep - gap / 2), clockwise: false)
                    ctx.stroke(p, with: .color(color), style: StrokeStyle(lineWidth: stroke, lineCap: .butt))
                }
                let idx = (today - start).clamped(0, length - 1)
                let ang = (-90 + (Double(idx) + 0.5) * sweep) * .pi / 180
                let knob = CGPoint(x: center.x + cos(ang) * radius, y: center.y + sin(ang) * radius)
                let r1 = stroke * 0.95, r2 = stroke * 0.72
                ctx.fill(Path(ellipseIn: CGRect(x: knob.x - r1, y: knob.y - r1, width: r1 * 2, height: r1 * 2)), with: .color(c.oat))
                ctx.fill(Path(ellipseIn: CGRect(x: knob.x - r2, y: knob.y - r2, width: r2 * 2, height: r2 * 2)), with: .color(c.banana))
            }
            content()
        }
        .frame(width: size, height: size)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Cycle day \(prediction.currentCycleDay.map(String.init) ?? "unknown") of about \(length)")
    }
}

/// Simple filled face for mood blobs and Mood Match art. score 1 (rough) ... 5 (great).
func drawFace(_ ctx: inout GraphicsContext, center: CGPoint, r: CGFloat, score: Int, ink: Color) {
    for dx in [-0.38, 0.38] {
        let e = CGRect(x: center.x + r * dx - r * 0.14, y: center.y - r * 0.18 - r * 0.14, width: r * 0.28, height: r * 0.28)
        ctx.fill(Path(ellipseIn: e), with: .color(ink))
    }
    let curve: CGFloat = [-0.30, -0.14, 0, 0.18, 0.34][(score - 1).clamped(0, 4)]
    var p = Path()
    p.move(to: CGPoint(x: center.x - r * 0.42, y: center.y + r * 0.30))
    p.addQuadCurve(to: CGPoint(x: center.x + r * 0.42, y: center.y + r * 0.30), control: CGPoint(x: center.x, y: center.y + r * (0.30 + curve * 1.6)))
    ctx.stroke(p, with: .color(ink), style: StrokeStyle(lineWidth: r * 0.13, lineCap: .round))
    if score == 5 {
        for dx in [-0.62, 0.62] {
            let e = CGRect(x: center.x + r * dx - r * 0.16, y: center.y + r * 0.10 - r * 0.16, width: r * 0.32, height: r * 0.32)
            ctx.fill(Path(ellipseIn: e), with: .color(Color(hex: 0xF49A8C, alpha: 0.33)))
        }
    }
}

/// Pastel mood blob with a face, rough to great.
struct MoodBlob: View {
    let score: Int
    var selected = false
    var size: CGFloat = 54
    @Environment(\.palette) private var c

    var body: some View {
        Canvas { ctx, sz in
            let r = min(sz.width, sz.height) / 2
            let center = CGPoint(x: sz.width / 2, y: sz.height / 2)
            let rr = r * (selected ? 1 : 0.86)
            ctx.fill(Path(ellipseIn: CGRect(x: center.x - rr, y: center.y - rr, width: rr * 2, height: rr * 2)),
                     with: .color(selected ? c.moodColorStrong(score) : c.moodColor(score)))
            if selected {
                let rs = r * 0.95
                ctx.stroke(Path(ellipseIn: CGRect(x: center.x - rs, y: center.y - rs, width: rs * 2, height: rs * 2)), with: .color(c.ink), lineWidth: r * 0.06)
            }
            drawFace(&ctx, center: center, r: r * 0.55, score: score, ink: c.ink)
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }
}

// MARK: - Spot illustrations

enum Illo { case heat, breath, stretch, moon, heart, sun, leafCup, spark }

func illoFor(_ icon: String) -> Illo {
    switch icon {
    case "heat": return .heat
    case "breath": return .breath
    case "stretch": return .stretch
    case "moon": return .moon
    case "care": return .heart
    case "food": return .leafCup
    case "spark": return .spark
    default: return .sun
    }
}

private func circle(_ c: CGPoint, _ r: CGFloat) -> Path { Path(ellipseIn: CGRect(x: c.x - r, y: c.y - r, width: r * 2, height: r * 2)) }

func heartPath(_ c: CGPoint, _ r: CGFloat) -> Path {
    var p = Path()
    p.move(to: CGPoint(x: c.x, y: c.y + r * 0.9))
    p.addCurve(to: CGPoint(x: c.x, y: c.y - r * 0.4), control1: CGPoint(x: c.x - r * 1.4, y: c.y + r * 0.1), control2: CGPoint(x: c.x - r * 0.9, y: c.y - r * 1.1))
    p.addCurve(to: CGPoint(x: c.x, y: c.y + r * 0.9), control1: CGPoint(x: c.x + r * 0.9, y: c.y - r * 1.1), control2: CGPoint(x: c.x + r * 1.4, y: c.y + r * 0.1))
    p.closeSubpath()
    return p
}

func sparkPath(_ c: CGPoint, _ r: CGFloat) -> Path {
    var p = Path()
    p.move(to: CGPoint(x: c.x, y: c.y - r))
    p.addQuadCurve(to: CGPoint(x: c.x + r, y: c.y), control: CGPoint(x: c.x + r * 0.12, y: c.y - r * 0.12))
    p.addQuadCurve(to: CGPoint(x: c.x, y: c.y + r), control: CGPoint(x: c.x + r * 0.12, y: c.y + r * 0.12))
    p.addQuadCurve(to: CGPoint(x: c.x - r, y: c.y), control: CGPoint(x: c.x - r * 0.12, y: c.y + r * 0.12))
    p.addQuadCurve(to: CGPoint(x: c.x, y: c.y - r), control: CGPoint(x: c.x - r * 0.12, y: c.y - r * 0.12))
    p.closeSubpath()
    return p
}

private func wave(_ ctx: inout GraphicsContext, _ x: CGFloat, _ y: CGFloat, _ w: CGFloat, _ color: Color, _ s: CGFloat) {
    var p = Path()
    p.move(to: CGPoint(x: x, y: y))
    p.addQuadCurve(to: CGPoint(x: x + w * 0.5, y: y), control: CGPoint(x: x + w * 0.25, y: y - s * 0.04))
    p.addQuadCurve(to: CGPoint(x: x + w, y: y), control: CGPoint(x: x + w * 0.75, y: y + s * 0.04))
    ctx.stroke(p, with: .color(color), style: StrokeStyle(lineWidth: s * 0.035, lineCap: .round))
}

/// Small flat illustrations for Care and empty states. `bg` is the circle behind the picture.
struct Illustration: View {
    let kind: Illo
    var size: CGFloat = 72
    var bg: Color? = nil
    @Environment(\.palette) private var c

    var body: some View {
        Canvas { ctx, sz in
            let s = min(sz.width, sz.height)
            let ctr = CGPoint(x: sz.width / 2, y: sz.height / 2)
            let back = bg ?? c.peachSoft
            ctx.fill(circle(ctr, s * 0.46), with: .color(back))
            switch kind {
            case .heat:
                ctx.fill(Path(roundedRect: CGRect(x: s * 0.30, y: s * 0.30, width: s * 0.40, height: s * 0.46), cornerRadius: s * 0.14), with: .color(c.period))
                ctx.fill(Path(roundedRect: CGRect(x: s * 0.42, y: s * 0.20, width: s * 0.16, height: s * 0.12), cornerRadius: s * 0.04), with: .color(c.periodDeep))
                wave(&ctx, s * 0.36, s * 0.48, s * 0.28, c.periodSoft, s)
                wave(&ctx, s * 0.36, s * 0.60, s * 0.28, c.periodSoft, s)
            case .breath:
                ctx.fill(circle(ctr, s * 0.30), with: .color(c.fertileSoft))
                ctx.fill(circle(ctr, s * 0.20), with: .color(c.fertile))
                ctx.fill(circle(ctr, s * 0.09), with: .color(c.oat))
            case .stretch:
                // A soft lotus: three petals and a sun drop.
                for angle in [-35.0, 35.0] {
                    var petal = ctx
                    petal.translateBy(x: ctr.x, y: ctr.y)
                    petal.rotate(by: .degrees(angle))
                    petal.fill(Path(ellipseIn: CGRect(x: -s * 0.09, y: -s * 0.30, width: s * 0.18, height: s * 0.36)), with: .color(c.peach))
                }
                ctx.fill(Path(ellipseIn: CGRect(x: ctr.x - s * 0.10, y: ctr.y - s * 0.32, width: s * 0.20, height: s * 0.40)), with: .color(c.rose))
                ctx.fill(Path(roundedRect: CGRect(x: s * 0.24, y: s * 0.70, width: s * 0.52, height: s * 0.05), cornerRadius: s * 0.03), with: .color(c.toffee.opacity(0.35)))
                ctx.fill(circle(CGPoint(x: ctr.x, y: s * 0.18), s * 0.05), with: .color(c.banana))
            case .moon:
                ctx.fill(circle(ctr, s * 0.24), with: .color(c.banana))
                ctx.fill(circle(CGPoint(x: ctr.x + s * 0.11, y: ctr.y - s * 0.08), s * 0.20), with: .color(back))
                ctx.fill(circle(CGPoint(x: s * 0.70, y: s * 0.30), s * 0.025), with: .color(c.oat))
                ctx.fill(circle(CGPoint(x: s * 0.30, y: s * 0.70), s * 0.018), with: .color(c.oat))
            case .heart:
                ctx.fill(heartPath(ctr, s * 0.26), with: .color(c.rose))
            case .sun:
                for i in 0..<8 {
                    var ray = ctx
                    ray.translateBy(x: ctr.x, y: ctr.y)
                    ray.rotate(by: .degrees(Double(i) * 45))
                    ray.fill(Path(roundedRect: CGRect(x: -s * 0.025, y: s * 0.12 - ctr.y, width: s * 0.05, height: s * 0.12), cornerRadius: s * 0.03), with: .color(c.banana))
                }
                ctx.fill(circle(ctr, s * 0.18), with: .color(c.banana))
            case .leafCup:
                ctx.fill(Path(roundedRect: CGRect(x: s * 0.28, y: s * 0.42, width: s * 0.40, height: s * 0.30), cornerRadius: s * 0.12), with: .color(c.peach))
                var handle = Path()
                handle.addArc(center: CGPoint(x: s * 0.68, y: s * 0.54), radius: s * 0.08, startAngle: .degrees(-90), endAngle: .degrees(90), clockwise: false)
                ctx.stroke(handle, with: .color(c.peach), lineWidth: s * 0.05)
                wave(&ctx, s * 0.38, s * 0.30, s * 0.14, c.rose, s)
            case .spark:
                ctx.fill(sparkPath(ctr, s * 0.26), with: .color(c.banana))
            }
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }
}

// MARK: - Game artwork

/// Each game has its own colour and artwork, so Play never looks like a grid of identical tiles.
struct GameLook { let bg: Color; let ink: Color }

extension Palette {
    func gameLook(_ id: String) -> GameLook {
        switch id {
        case "banana_catch": return GameLook(bg: banana, ink: onBanana)
        case "mood_match": return GameLook(bg: peach, ink: Palette.light.ink)
        case "memory_flip": return GameLook(bg: fertile, ink: Palette.light.ink)
        case "brain_break": return GameLook(bg: bananaSoft, ink: ink)
        case "word_puzzle": return GameLook(bg: peachSoft, ink: ink)
        case "calm_tap": return GameLook(bg: fertileSoft, ink: ink)
        case "focus_game": return GameLook(bg: roseSoft, ink: ink)
        case "stress_smash": return GameLook(bg: period, ink: onPeriod)
        default: return GameLook(bg: sand, ink: ink)
        }
    }
}

/// Fills a banana crescent with a toffee tip (shared by the art and the Banana Catch game).
func drawBanana(_ ctx: inout GraphicsContext, at p: CGPoint, scale u: CGFloat, outline: Color, tip: Color, fill: Color = Color(hex: 0xFFE27A)) {
    var path = Path()
    path.move(to: CGPoint(x: p.x - 19 * u, y: p.y - 8 * u))
    path.addQuadCurve(to: CGPoint(x: p.x + 20 * u, y: p.y - 6 * u), control: CGPoint(x: p.x - 2 * u, y: p.y + 22 * u))
    path.addQuadCurve(to: CGPoint(x: p.x - 19 * u, y: p.y - 8 * u), control: CGPoint(x: p.x, y: p.y + 6 * u))
    path.closeSubpath()
    ctx.fill(path, with: .color(fill))
    ctx.stroke(path, with: .color(outline), lineWidth: 1.5 * u)
    ctx.fill(circle(CGPoint(x: p.x + 20 * u, y: p.y - 6 * u), 2.6 * u), with: .color(tip))
}

struct GameArt: View {
    let id: String
    @Environment(\.palette) private var c

    var body: some View {
        Canvas { ctx, sz in
            let w = sz.width, h = sz.height, s = min(w, h)
            switch id {
            case "banana_catch":
                for (x, y, rot, sc) in [(0.28, 0.30, -20.0, 1.1), (0.66, 0.22, 25.0, 0.9), (0.52, 0.52, 10.0, 1.0)] {
                    var b = ctx
                    b.translateBy(x: w * x, y: h * y); b.rotate(by: .degrees(rot))
                    drawBanana(&b, at: .zero, scale: s * 0.0068 * sc, outline: c.ink.opacity(0.8), tip: c.toffee)
                }
                // A woven basket: the lower half of an oval, with a coral rim.
                var bowl = ctx
                bowl.clip(to: Path(CGRect(x: 0, y: h * 0.77, width: w, height: h)))
                bowl.fill(Path(ellipseIn: CGRect(x: w * 0.30, y: h * 0.62, width: w * 0.40, height: h * 0.30)), with: .color(c.toffee))
                ctx.fill(Path(roundedRect: CGRect(x: w * 0.28, y: h * 0.74, width: w * 0.44, height: h * 0.05), cornerRadius: h * 0.025), with: .color(c.periodDeep))
            case "mood_match":
                let faces = [5, 1, 4, 2]
                for (i, score) in faces.enumerated() {
                    let x = w * (0.22 + CGFloat(i % 2) * 0.32) + CGFloat(i / 2) * w * 0.12
                    let y = h * (0.30 + CGFloat(i / 2) * 0.38)
                    ctx.fill(Path(roundedRect: CGRect(x: x - s * 0.13, y: y - s * 0.16, width: s * 0.26, height: s * 0.32), cornerRadius: s * 0.05),
                             with: .color(i % 2 == 0 ? c.oat : c.peachSoft))
                    drawFace(&ctx, center: CGPoint(x: x, y: y), r: s * 0.08, score: score, ink: Palette.light.ink)
                }
            case "memory_flip":
                for i in 0..<3 { for j in 0..<3 {
                    let lit = (i + j) % 3 == 0
                    ctx.fill(Path(roundedRect: CGRect(x: w * 0.18 + CGFloat(i) * w * 0.23, y: h * 0.16 + CGFloat(j) * h * 0.24, width: w * 0.19, height: h * 0.20),
                                  cornerRadius: s * 0.04), with: .color(lit ? c.banana : c.fertileSoft))
                } }
            case "brain_break":
                ctx.fill(circle(CGPoint(x: w * 0.5, y: h * 0.5), s * 0.30), with: .color(c.banana))
                var plus = Path()
                plus.move(to: CGPoint(x: w * 0.5 - s * 0.12, y: h * 0.5)); plus.addLine(to: CGPoint(x: w * 0.5 + s * 0.12, y: h * 0.5))
                plus.move(to: CGPoint(x: w * 0.5, y: h * 0.5 - s * 0.12)); plus.addLine(to: CGPoint(x: w * 0.5, y: h * 0.5 + s * 0.12))
                ctx.stroke(plus, with: .color(Palette.light.ink), style: StrokeStyle(lineWidth: s * 0.035, lineCap: .round))
                ctx.fill(circle(CGPoint(x: w * 0.20, y: h * 0.22), s * 0.08), with: .color(c.peach))
                ctx.fill(circle(CGPoint(x: w * 0.82, y: h * 0.78), s * 0.06), with: .color(c.rose))
            case "word_puzzle":
                for (i, col) in [c.peach, c.banana, c.fertileSoft, c.roseSoft].enumerated() {
                    ctx.fill(Path(roundedRect: CGRect(x: w * 0.12 + CGFloat(i) * w * 0.2, y: h * 0.36 + (i % 2 == 0 ? -h * 0.05 : h * 0.05),
                                                      width: w * 0.17, height: w * 0.17), cornerRadius: s * 0.04), with: .color(col))
                }
            case "calm_tap":
                let ctr = CGPoint(x: w / 2, y: h / 2)
                ctx.stroke(circle(ctr, s * 0.38), with: .color(c.oat), lineWidth: s * 0.02)
                ctx.fill(circle(ctr, s * 0.30), with: .color(c.fertile.opacity(0.45)))
                ctx.fill(circle(ctr, s * 0.18), with: .color(c.fertile))
            case "focus_game":
                ctx.fill(circle(CGPoint(x: w * 0.22, y: h * 0.28), s * 0.08), with: .color(c.rose))
                ctx.fill(circle(CGPoint(x: w * 0.78, y: h * 0.70), s * 0.07), with: .color(c.peach))
                ctx.fill(Path(roundedRect: CGRect(x: w * 0.68, y: h * 0.18, width: s * 0.14, height: s * 0.14), cornerRadius: s * 0.03), with: .color(c.fertile))
                ctx.fill(circle(CGPoint(x: w * 0.46, y: h * 0.52), s * 0.20), with: .color(c.oat))
                ctx.fill(circle(CGPoint(x: w * 0.46, y: h * 0.52), s * 0.12), with: .color(c.banana))
            case "stress_smash":
                for i in 0..<3 { for j in 0..<2 {
                    let crushed = (i + j) % 2 == 1
                    ctx.fill(Path(roundedRect: CGRect(x: w * 0.16 + CGFloat(i) * w * 0.24, y: h * 0.24 + CGFloat(j) * h * 0.30, width: w * 0.20, height: h * 0.24),
                                  cornerRadius: s * 0.05), with: .color(crushed ? c.periodSoft : c.oat))
                } }
                ctx.fill(sparkPath(CGPoint(x: w * 0.78, y: h * 0.18), s * 0.09), with: .color(c.banana))
            default: break
            }
        }
        .accessibilityHidden(true)
    }
}
