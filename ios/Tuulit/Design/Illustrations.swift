import SwiftUI
import TuulitCore

/*
 Tuulit illustration system, ported shape for shape from Android `Illustrations.kt`: flat,
 rounded shapes from the palette, no outlines (except the mascot), at most three colours each.
 Drawing lives in small render functions with explicit CGFloat maths so the compiler checks
 each statement on its own.
 */

// MARK: - Drawing helpers

func circlePath(_ c: CGPoint, _ r: CGFloat) -> Path { Path(ellipseIn: CGRect(x: c.x - r, y: c.y - r, width: r * 2, height: r * 2)) }

func fillCircle(_ ctx: inout GraphicsContext, _ c: CGPoint, _ r: CGFloat, _ color: Color) {
    ctx.fill(circlePath(c, r), with: .color(color))
}

func fillRound(_ ctx: inout GraphicsContext, _ rect: CGRect, _ radius: CGFloat, _ color: Color) {
    ctx.fill(Path(roundedRect: rect, cornerRadius: radius), with: .color(color))
}

/// A soft organic blob used behind heroes.
struct Blob: Shape {
    var seed = 0
    func path(in r: CGRect) -> Path {
        let base: [CGFloat] = [0.06, -0.04, 0.05, -0.06, 0.03, -0.05]
        let k: Int = seed % base.count
        let wob: [CGFloat] = Array(base[k...]) + Array(base[..<k])
        let n = 6
        var pts: [CGPoint] = []
        for i in 0..<n {
            let a: CGFloat = CGFloat(i) / CGFloat(n) * 2 * CGFloat.pi
            let rr: CGFloat = 0.5 + wob[i]
            let x: CGFloat = r.midX + cos(a) * r.width * rr
            let y: CGFloat = r.midY + sin(a) * r.height * rr
            pts.append(CGPoint(x: x, y: y))
        }
        var p = Path()
        p.move(to: CGPoint(x: (pts[0].x + pts[n - 1].x) / 2, y: (pts[0].y + pts[n - 1].y) / 2))
        for i in 0..<n {
            let cur = pts[i]
            let nxt = pts[(i + 1) % n]
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

    private var length: Int { prediction.averageCycleLength.clamped(21, 45) }
    private var start: Int { cycleStart ?? (today - ((prediction.currentCycleDay ?? 1) - 1)) }

    private func color(for day: Int) -> Color {
        if prediction.loggedPeriodDays.contains(day) { return c.period }
        if prediction.predictedPeriodDays.contains(day) { return c.periodSoft }
        if day == prediction.estimatedOvulationDay { return c.fertile }
        if prediction.estimatedFertileDays.contains(day) { return c.fertileSoft }
        if day < today { return c.tan }
        return c.sand
    }

    private func render(_ ctx: inout GraphicsContext, _ sz: CGSize) {
        let side: CGFloat = min(sz.width, sz.height)
        let stroke: CGFloat = side * 0.085
        let radius: CGFloat = side / 2 - stroke
        let center = CGPoint(x: sz.width / 2, y: sz.height / 2)
        let gap: Double = 1.6
        let sweep: Double = 360.0 / Double(length)
        for i in 0..<length {
            let startDeg: Double = -90 + Double(i) * sweep + gap / 2
            let endDeg: Double = -90 + Double(i + 1) * sweep - gap / 2
            var p = Path()
            p.addArc(center: center, radius: radius, startAngle: .degrees(startDeg), endAngle: .degrees(endDeg), clockwise: false)
            ctx.stroke(p, with: .color(color(for: start + i)), style: StrokeStyle(lineWidth: stroke, lineCap: .butt))
        }
        let idx: Int = (today - start).clamped(0, length - 1)
        let angDeg: Double = -90 + (Double(idx) + 0.5) * sweep
        let ang: CGFloat = CGFloat(angDeg * Double.pi / 180)
        let knob = CGPoint(x: center.x + cos(ang) * radius, y: center.y + sin(ang) * radius)
        fillCircle(&ctx, knob, stroke * 0.95, c.oat)
        fillCircle(&ctx, knob, stroke * 0.72, c.banana)
    }

    var body: some View {
        ZStack {
            Canvas { ctx, sz in render(&ctx, sz) }
            content()
        }
        .frame(width: size, height: size)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Cycle day \(prediction.currentCycleDay.map(String.init) ?? "unknown") of about \(length)")
    }
}

/// Simple filled face for mood blobs and Mood Match art. score 1 (rough) ... 5 (great).
func drawFace(_ ctx: inout GraphicsContext, center: CGPoint, r: CGFloat, score: Int, ink: Color) {
    let eyeR: CGFloat = r * 0.14
    let eyeY: CGFloat = center.y - r * 0.18
    fillCircle(&ctx, CGPoint(x: center.x - r * 0.38, y: eyeY), eyeR, ink)
    fillCircle(&ctx, CGPoint(x: center.x + r * 0.38, y: eyeY), eyeR, ink)
    let curves: [CGFloat] = [-0.30, -0.14, 0, 0.18, 0.34]
    let curve: CGFloat = curves[(score - 1).clamped(0, 4)]
    let mouthY: CGFloat = center.y + r * 0.30
    let controlY: CGFloat = center.y + r * (0.30 + curve * 1.6)
    var p = Path()
    p.move(to: CGPoint(x: center.x - r * 0.42, y: mouthY))
    p.addQuadCurve(to: CGPoint(x: center.x + r * 0.42, y: mouthY), control: CGPoint(x: center.x, y: controlY))
    ctx.stroke(p, with: .color(ink), style: StrokeStyle(lineWidth: r * 0.13, lineCap: .round))
    if score == 5 {
        let blush = Color(hex: 0xF49A8C, alpha: 0.33)
        let blushY: CGFloat = center.y + r * 0.10
        fillCircle(&ctx, CGPoint(x: center.x - r * 0.62, y: blushY), r * 0.16, blush)
        fillCircle(&ctx, CGPoint(x: center.x + r * 0.62, y: blushY), r * 0.16, blush)
    }
}

/// Pastel mood blob with a face, rough to great.
struct MoodBlob: View {
    let score: Int
    var selected = false
    var size: CGFloat = 54
    @Environment(\.palette) private var c

    private func render(_ ctx: inout GraphicsContext, _ sz: CGSize) {
        let r: CGFloat = min(sz.width, sz.height) / 2
        let center = CGPoint(x: sz.width / 2, y: sz.height / 2)
        let rr: CGFloat = selected ? r : r * 0.86
        fillCircle(&ctx, center, rr, selected ? c.moodColorStrong(score) : c.moodColor(score))
        if selected { ctx.stroke(circlePath(center, r * 0.95), with: .color(c.ink), lineWidth: r * 0.06) }
        drawFace(&ctx, center: center, r: r * 0.55, score: score, ink: c.ink)
    }

    var body: some View {
        Canvas { ctx, sz in render(&ctx, sz) }
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

func heartPath(_ c: CGPoint, _ r: CGFloat) -> Path {
    var p = Path()
    p.move(to: CGPoint(x: c.x, y: c.y + r * 0.9))
    p.addCurve(to: CGPoint(x: c.x, y: c.y - r * 0.4), control1: CGPoint(x: c.x - r * 1.4, y: c.y + r * 0.1), control2: CGPoint(x: c.x - r * 0.9, y: c.y - r * 1.1))
    p.addCurve(to: CGPoint(x: c.x, y: c.y + r * 0.9), control1: CGPoint(x: c.x + r * 0.9, y: c.y - r * 1.1), control2: CGPoint(x: c.x + r * 1.4, y: c.y + r * 0.1))
    p.closeSubpath()
    return p
}

func sparkPath(_ c: CGPoint, _ r: CGFloat) -> Path {
    let k: CGFloat = r * 0.12
    var p = Path()
    p.move(to: CGPoint(x: c.x, y: c.y - r))
    p.addQuadCurve(to: CGPoint(x: c.x + r, y: c.y), control: CGPoint(x: c.x + k, y: c.y - k))
    p.addQuadCurve(to: CGPoint(x: c.x, y: c.y + r), control: CGPoint(x: c.x + k, y: c.y + k))
    p.addQuadCurve(to: CGPoint(x: c.x - r, y: c.y), control: CGPoint(x: c.x - k, y: c.y + k))
    p.addQuadCurve(to: CGPoint(x: c.x, y: c.y - r), control: CGPoint(x: c.x - k, y: c.y - k))
    p.closeSubpath()
    return p
}

private func wave(_ ctx: inout GraphicsContext, _ x: CGFloat, _ y: CGFloat, _ w: CGFloat, _ color: Color, _ s: CGFloat) {
    let d: CGFloat = s * 0.04
    var p = Path()
    p.move(to: CGPoint(x: x, y: y))
    p.addQuadCurve(to: CGPoint(x: x + w * 0.5, y: y), control: CGPoint(x: x + w * 0.25, y: y - d))
    p.addQuadCurve(to: CGPoint(x: x + w, y: y), control: CGPoint(x: x + w * 0.75, y: y + d))
    ctx.stroke(p, with: .color(color), style: StrokeStyle(lineWidth: s * 0.035, lineCap: .round))
}

/// Small flat illustrations for Care and empty states. `bg` is the circle behind the picture.
struct Illustration: View {
    let kind: Illo
    var size: CGFloat = 72
    var bg: Color? = nil
    @Environment(\.palette) private var c

    private func render(_ ctx: inout GraphicsContext, _ sz: CGSize) {
        let s: CGFloat = min(sz.width, sz.height)
        let ctr = CGPoint(x: sz.width / 2, y: sz.height / 2)
        let back: Color = bg ?? c.peachSoft
        fillCircle(&ctx, ctr, s * 0.46, back)
        switch kind {
        case .heat:
            fillRound(&ctx, CGRect(x: s * 0.30, y: s * 0.30, width: s * 0.40, height: s * 0.46), s * 0.14, c.period)
            fillRound(&ctx, CGRect(x: s * 0.42, y: s * 0.20, width: s * 0.16, height: s * 0.12), s * 0.04, c.periodDeep)
            wave(&ctx, s * 0.36, s * 0.48, s * 0.28, c.periodSoft, s)
            wave(&ctx, s * 0.36, s * 0.60, s * 0.28, c.periodSoft, s)
        case .breath:
            fillCircle(&ctx, ctr, s * 0.30, c.fertileSoft)
            fillCircle(&ctx, ctr, s * 0.20, c.fertile)
            fillCircle(&ctx, ctr, s * 0.09, c.oat)
        case .stretch:
            // A soft lotus: three petals and a sun drop.
            let petal = CGRect(x: -s * 0.09, y: -s * 0.30, width: s * 0.18, height: s * 0.36)
            for angle in [-35.0, 35.0] {
                var leaf = ctx
                leaf.translateBy(x: ctr.x, y: ctr.y)
                leaf.rotate(by: .degrees(angle))
                leaf.fill(Path(ellipseIn: petal), with: .color(c.peach))
            }
            ctx.fill(Path(ellipseIn: CGRect(x: ctr.x - s * 0.10, y: ctr.y - s * 0.32, width: s * 0.20, height: s * 0.40)), with: .color(c.rose))
            fillRound(&ctx, CGRect(x: s * 0.24, y: s * 0.70, width: s * 0.52, height: s * 0.05), s * 0.03, c.toffee.opacity(0.35))
            fillCircle(&ctx, CGPoint(x: ctr.x, y: s * 0.18), s * 0.05, c.banana)
        case .moon:
            fillCircle(&ctx, ctr, s * 0.24, c.banana)
            fillCircle(&ctx, CGPoint(x: ctr.x + s * 0.11, y: ctr.y - s * 0.08), s * 0.20, back)
            fillCircle(&ctx, CGPoint(x: s * 0.70, y: s * 0.30), s * 0.025, c.oat)
            fillCircle(&ctx, CGPoint(x: s * 0.30, y: s * 0.70), s * 0.018, c.oat)
        case .heart:
            ctx.fill(heartPath(ctr, s * 0.26), with: .color(c.rose))
        case .sun:
            let ray = CGRect(x: -s * 0.025, y: s * 0.12 - ctr.y, width: s * 0.05, height: s * 0.12)
            for i in 0..<8 {
                var r = ctx
                r.translateBy(x: ctr.x, y: ctr.y)
                r.rotate(by: .degrees(Double(i) * 45))
                r.fill(Path(roundedRect: ray, cornerRadius: s * 0.03), with: .color(c.banana))
            }
            fillCircle(&ctx, ctr, s * 0.18, c.banana)
        case .leafCup:
            fillRound(&ctx, CGRect(x: s * 0.28, y: s * 0.42, width: s * 0.40, height: s * 0.30), s * 0.12, c.peach)
            var handle = Path()
            handle.addArc(center: CGPoint(x: s * 0.68, y: s * 0.54), radius: s * 0.08, startAngle: .degrees(-90), endAngle: .degrees(90), clockwise: false)
            ctx.stroke(handle, with: .color(c.peach), lineWidth: s * 0.05)
            wave(&ctx, s * 0.38, s * 0.30, s * 0.14, c.rose, s)
        case .spark:
            ctx.fill(sparkPath(ctr, s * 0.26), with: .color(c.banana))
        }
    }

    var body: some View {
        Canvas { ctx, sz in render(&ctx, sz) }
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
    let start = CGPoint(x: p.x - 19 * u, y: p.y - 8 * u)
    let end = CGPoint(x: p.x + 20 * u, y: p.y - 6 * u)
    var path = Path()
    path.move(to: start)
    path.addQuadCurve(to: end, control: CGPoint(x: p.x - 2 * u, y: p.y + 22 * u))
    path.addQuadCurve(to: start, control: CGPoint(x: p.x, y: p.y + 6 * u))
    path.closeSubpath()
    ctx.fill(path, with: .color(fill))
    ctx.stroke(path, with: .color(outline), lineWidth: 1.5 * u)
    fillCircle(&ctx, end, 2.6 * u, tip)
}

struct GameArt: View {
    let id: String
    @Environment(\.palette) private var c

    private func render(_ ctx: inout GraphicsContext, _ sz: CGSize) {
        let w: CGFloat = sz.width
        let h: CGFloat = sz.height
        let s: CGFloat = min(w, h)
        switch id {
        case "banana_catch":
            let bananas: [(CGFloat, CGFloat, Double, CGFloat)] = [(0.28, 0.30, -20, 1.1), (0.66, 0.22, 25, 0.9), (0.52, 0.52, 10, 1.0)]
            for (x, y, rot, sc) in bananas {
                var b = ctx
                b.translateBy(x: w * x, y: h * y)
                b.rotate(by: .degrees(rot))
                drawBanana(&b, at: .zero, scale: s * 0.0068 * sc, outline: c.ink.opacity(0.8), tip: c.toffee)
            }
            // A woven basket: the lower half of an oval, with a coral rim.
            var bowl = ctx
            bowl.clip(to: Path(CGRect(x: 0, y: h * 0.77, width: w, height: h)))
            bowl.fill(Path(ellipseIn: CGRect(x: w * 0.30, y: h * 0.62, width: w * 0.40, height: h * 0.30)), with: .color(c.toffee))
            fillRound(&ctx, CGRect(x: w * 0.28, y: h * 0.74, width: w * 0.44, height: h * 0.05), h * 0.025, c.periodDeep)
        case "mood_match":
            let faces = [5, 1, 4, 2]
            for i in 0..<faces.count {
                let col: CGFloat = CGFloat(i % 2)
                let row: CGFloat = CGFloat(i / 2)
                let x: CGFloat = w * (0.22 + col * 0.32) + row * w * 0.12
                let y: CGFloat = h * (0.30 + row * 0.38)
                fillRound(&ctx, CGRect(x: x - s * 0.13, y: y - s * 0.16, width: s * 0.26, height: s * 0.32), s * 0.05, i % 2 == 0 ? c.oat : c.peachSoft)
                drawFace(&ctx, center: CGPoint(x: x, y: y), r: s * 0.08, score: faces[i], ink: Palette.light.ink)
            }
        case "memory_flip":
            for i in 0..<3 {
                for j in 0..<3 {
                    let rect = CGRect(x: w * 0.18 + CGFloat(i) * w * 0.23, y: h * 0.16 + CGFloat(j) * h * 0.24, width: w * 0.19, height: h * 0.20)
                    fillRound(&ctx, rect, s * 0.04, (i + j) % 3 == 0 ? c.banana : c.fertileSoft)
                }
            }
        case "brain_break":
            let ctr = CGPoint(x: w * 0.5, y: h * 0.5)
            let arm: CGFloat = s * 0.12
            fillCircle(&ctx, ctr, s * 0.30, c.banana)
            var plus = Path()
            plus.move(to: CGPoint(x: ctr.x - arm, y: ctr.y)); plus.addLine(to: CGPoint(x: ctr.x + arm, y: ctr.y))
            plus.move(to: CGPoint(x: ctr.x, y: ctr.y - arm)); plus.addLine(to: CGPoint(x: ctr.x, y: ctr.y + arm))
            ctx.stroke(plus, with: .color(Palette.light.ink), style: StrokeStyle(lineWidth: s * 0.035, lineCap: .round))
            fillCircle(&ctx, CGPoint(x: w * 0.20, y: h * 0.22), s * 0.08, c.peach)
            fillCircle(&ctx, CGPoint(x: w * 0.82, y: h * 0.78), s * 0.06, c.rose)
        case "word_puzzle":
            let colors = [c.peach, c.banana, c.fertileSoft, c.roseSoft]
            for i in 0..<colors.count {
                let dy: CGFloat = i % 2 == 0 ? -h * 0.05 : h * 0.05
                fillRound(&ctx, CGRect(x: w * 0.12 + CGFloat(i) * w * 0.2, y: h * 0.36 + dy, width: w * 0.17, height: w * 0.17), s * 0.04, colors[i])
            }
        case "calm_tap":
            let ctr = CGPoint(x: w / 2, y: h / 2)
            ctx.stroke(circlePath(ctr, s * 0.38), with: .color(c.oat), lineWidth: s * 0.02)
            fillCircle(&ctx, ctr, s * 0.30, c.fertile.opacity(0.45))
            fillCircle(&ctx, ctr, s * 0.18, c.fertile)
        case "focus_game":
            fillCircle(&ctx, CGPoint(x: w * 0.22, y: h * 0.28), s * 0.08, c.rose)
            fillCircle(&ctx, CGPoint(x: w * 0.78, y: h * 0.70), s * 0.07, c.peach)
            fillRound(&ctx, CGRect(x: w * 0.68, y: h * 0.18, width: s * 0.14, height: s * 0.14), s * 0.03, c.fertile)
            fillCircle(&ctx, CGPoint(x: w * 0.46, y: h * 0.52), s * 0.20, c.oat)
            fillCircle(&ctx, CGPoint(x: w * 0.46, y: h * 0.52), s * 0.12, c.banana)
        case "stress_smash":
            for i in 0..<3 {
                for j in 0..<2 {
                    let rect = CGRect(x: w * 0.16 + CGFloat(i) * w * 0.24, y: h * 0.24 + CGFloat(j) * h * 0.30, width: w * 0.20, height: h * 0.24)
                    fillRound(&ctx, rect, s * 0.05, (i + j) % 2 == 1 ? c.periodSoft : c.oat)
                }
            }
            ctx.fill(sparkPath(CGPoint(x: w * 0.78, y: h * 0.18), s * 0.09), with: .color(c.banana))
        default:
            break
        }
    }

    var body: some View {
        Canvas { ctx, sz in render(&ctx, sz) }
            .accessibilityHidden(true)
    }
}
