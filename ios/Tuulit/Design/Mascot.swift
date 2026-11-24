import SwiftUI
import TuulitCore

/// The seven Banana Mode moods. The silhouette never changes; only the face and one prop do.
enum MascotMood: String, CaseIterable { case warm, chill, cramps, sleepy, drama, gym, happy }

extension BananaModePersona {
    var mood: MascotMood {
        switch self {
        case .NEUTRAL: return .warm
        case .CHILL: return .chill
        case .CRAMPS: return .cramps
        case .SLEEPY: return .sleepy
        case .DRAMA: return .drama
        case .GYM: return .gym
        case .HAPPY: return .happy
        }
    }
}

/// The half-peeled banana, drawn from the same 120×120 paths as Android `Mascot.kt`.
struct Mascot: View {
    let mood: MascotMood
    var size: CGFloat = 64

    init(_ mood: MascotMood, size: CGFloat = 64) { self.mood = mood; self.size = size }

    private enum Shape {
        static let backLeft = "M54 63C40 55 28 63 26 86c7 0 12-12 26-15z"
        static let backRight = "M66 63c12-9 24-6 27 7-7 0-12-4-25 1z"
        static let flesh = "M51 66C47 44 50 22 62 13c6-4 10 0 8 9-2 14-4 30-4 44z"
        static let body = "M49 65c-1 22 13 38 37 41l4-7c-17-6-24-18-24-34z"
        static let stem = "M86 99l5 1.5-2 7-5-1.5z"
        static let peelLeft = "M57 64c-14 0-22 14-17 32 7 1 9-13 18-22z"
        static let crease1 = "M45 76c-1 5-1 9 0 13"
        static let peelRight = "M62 64c12-6 25 0 26 16-7 1-9-8-23-8z"
        static let crease2 = "M72 66c4 0 8 2 10 6"
        static let headband = "M51.5 28.5l20-3.6.8 5-20 3.6z"
        static let wrap = "M51 58h18a5 5 0 0 1 0 10h-18a5 5 0 0 1 0-10z"
    }

    var body: some View {
        Canvas { ctx, sz in
            let k = min(sz.width, sz.height) / 120
            ctx.scaleBy(x: k, y: k)
            let outline = StrokeStyle(lineWidth: 2.6, lineCap: .round, lineJoin: .round)
            func shape(_ d: String, _ fill: Color) {
                let p = SVGPath.path(d)
                ctx.fill(p, with: .color(fill)); ctx.stroke(p, with: .color(MascotColors.outline), style: outline)
            }
            func line(_ d: String, _ w: CGFloat = 2) {
                ctx.stroke(SVGPath.path(d), with: .color(MascotColors.outline), style: StrokeStyle(lineWidth: w, lineCap: .round, lineJoin: .round))
            }
            func dot(_ x: CGFloat, _ y: CGFloat, _ r: CGFloat, _ color: Color = MascotColors.outline) {
                ctx.fill(Path(ellipseIn: CGRect(x: x - r, y: y - r, width: r * 2, height: r * 2)), with: .color(color))
            }
            shape(Shape.backLeft, MascotColors.tan)
            shape(Shape.backRight, MascotColors.tan)
            shape(Shape.flesh, MascotColors.flesh)
            dot(66.5, 13.5, 3)
            shape(Shape.body, MascotColors.banana)
            shape(Shape.stem, MascotColors.outline)
            shape(Shape.peelLeft, MascotColors.banana)
            line(Shape.crease1, 1.6)
            shape(Shape.peelRight, MascotColors.banana)
            line(Shape.crease2, 1.6)
            if mood == .gym { shape(Shape.headband, MascotColors.prop) }
            if mood == .cramps { shape(Shape.wrap, MascotColors.prop) }
            switch mood {
            case .warm, .gym:
                let dy: CGFloat = mood == .gym ? 1 : 0
                dot(57, 37 + dy, 2.1); dot(64.5, 36 + dy, 2.1)
                line(mood == .gym ? "M57.5 43q3.8 3.6 7.4-.6" : "M57.5 42.5q3.8 3.4 7.4-.6")
            case .chill:
                line("M54.6 37q2.4 2.4 4.8 0M62 36q2.4 2.4 4.8 0"); line("M58.5 43q2.5 1.8 5 0")
            case .cramps:
                line("M55 38l4-2M62.5 36l4 2"); line("M57 44q1.7-2 3.4 0t3.4 0t3 0")
            case .sleepy:
                line("M54.8 37.5h4.4M62.2 36.5h4.4")
                ctx.stroke(Path(ellipseIn: CGRect(x: 59.2, y: 41.7, width: 3.6, height: 3.6)), with: .color(MascotColors.outline), lineWidth: 1.8)
                line("M80 14h8l-8 9h8M90 5h5l-5 6h5")
            case .drama:
                dot(57, 37.5, 2.2); dot(64.5, 36.5, 2.2)
                line("M62 31.5l6-1.6"); line("M57.5 43.5q4.5 1 7.5-2.6")
            case .happy:
                line("M54.6 38q2.4-3 4.8 0M62 37q2.4-3 4.8 0")
                let mouth = SVGPath.path("M56.5 42q4.5 5 9 0")
                ctx.fill(mouth, with: .color(MascotColors.prop))
                ctx.stroke(mouth, with: .color(MascotColors.outline), style: StrokeStyle(lineWidth: 2, lineCap: .round, lineJoin: .round))
                ctx.stroke(SVGPath.path("M26 18v10M21 23h10"), with: .color(MascotColors.banana), style: StrokeStyle(lineWidth: 2.6, lineCap: .round))
            }
            if mood != .sleepy {
                dot(53.5, 41.5, 3.4, MascotColors.blush); dot(68, 40.5, 3.4, MascotColors.blush)
            }
        }
        .frame(width: size, height: size)
        .accessibilityElement()
        .accessibilityLabel("Tuulit banana, \(mood.rawValue) mood")
    }
}
