import SwiftUI

/// Parses SVG path data (M L H V C S Q T A Z, absolute and relative) into a SwiftUI Path.
/// Lets the iOS icons and mascot use the exact path strings of the Android design system.
enum SVGPath {
    private static var cache: [String: Path] = [:]

    static func path(_ d: String) -> Path {
        if let p = cache[d] { return p }
        let p = parse(d)
        cache[d] = p
        return p
    }

    private static func tokenize(_ d: String) -> [String] {
        var out: [String] = []
        var cur = ""
        func flush() { if !cur.isEmpty { out.append(cur); cur = "" } }
        for ch in d {
            if ch.isLetter && ch != "e" && ch != "E" {
                flush(); out.append(String(ch))
            } else if ch == "-" {
                if let last = cur.last, last == "e" || last == "E" { cur.append(ch) } else { flush(); cur.append(ch) }
            } else if ch == "." {
                if cur.contains(".") && !cur.contains("e") { flush() }
                cur.append(ch)
            } else if ch.isNumber || ch == "e" || ch == "E" {
                cur.append(ch)
            } else {
                flush()
            }
        }
        flush()
        return out
    }

    private static func parse(_ d: String) -> Path {
        let t = tokenize(d)
        var i = 0
        var cmd: Character = "M"
        var p = Path()
        var cur = CGPoint.zero, start = CGPoint.zero
        var lastControl: CGPoint? = nil
        var lastCmd: Character = " "

        func isCommand(_ s: String) -> Bool { s.count == 1 && s.first!.isLetter && s != "e" && s != "E" }
        func num() -> CGFloat { defer { i += 1 }; return CGFloat(Double(t[i]) ?? 0) }
        func flag() -> Bool { num() != 0 }

        while i < t.count {
            if isCommand(t[i]) { cmd = Character(t[i]); i += 1 }
            let rel = cmd.isLowercase
            let C = Character(cmd.uppercased())
            func pt(_ x: CGFloat, _ y: CGFloat) -> CGPoint { rel ? CGPoint(x: cur.x + x, y: cur.y + y) : CGPoint(x: x, y: y) }
            switch C {
            case "Z":
                p.closeSubpath(); cur = start; lastControl = nil
            case "M":
                cur = pt(num(), num()); start = cur; p.move(to: cur)
                cmd = rel ? "l" : "L"; lastControl = nil
            case "L":
                cur = pt(num(), num()); p.addLine(to: cur); lastControl = nil
            case "H":
                let x = num(); cur = CGPoint(x: rel ? cur.x + x : x, y: cur.y); p.addLine(to: cur); lastControl = nil
            case "V":
                let y = num(); cur = CGPoint(x: cur.x, y: rel ? cur.y + y : y); p.addLine(to: cur); lastControl = nil
            case "C":
                let c1 = pt(num(), num()), c2 = pt(num(), num()), e = pt(num(), num())
                p.addCurve(to: e, control1: c1, control2: c2); lastControl = c2; cur = e
            case "S":
                let c1 = (lastControl != nil && "CS".contains(Character(String(lastCmd).uppercased())))
                    ? CGPoint(x: 2 * cur.x - lastControl!.x, y: 2 * cur.y - lastControl!.y) : cur
                let c2 = pt(num(), num()), e = pt(num(), num())
                p.addCurve(to: e, control1: c1, control2: c2); lastControl = c2; cur = e
            case "Q":
                let c1 = pt(num(), num()), e = pt(num(), num())
                p.addQuadCurve(to: e, control: c1); lastControl = c1; cur = e
            case "T":
                let c1 = (lastControl != nil && "QT".contains(Character(String(lastCmd).uppercased())))
                    ? CGPoint(x: 2 * cur.x - lastControl!.x, y: 2 * cur.y - lastControl!.y) : cur
                let e = pt(num(), num())
                p.addQuadCurve(to: e, control: c1); lastControl = c1; cur = e
            case "A":
                let rx = num(), ry = num(), rot = num(), large = flag(), sweep = flag()
                let e = pt(num(), num())
                for q in arcPoints(from: cur, rx: rx, ry: ry, rotation: rot, large: large, sweep: sweep, to: e) { p.addLine(to: q) }
                cur = e; lastControl = nil
            default:
                i += 1
            }
            lastCmd = cmd
        }
        return p
    }

    /// SVG elliptical arc flattened to points (endpoint to centre parameterisation, SVG spec F.6.5).
    private static func arcPoints(from p1: CGPoint, rx rxIn: CGFloat, ry ryIn: CGFloat, rotation: CGFloat, large: Bool, sweep: Bool, to p2: CGPoint) -> [CGPoint] {
        var rx = abs(rxIn), ry = abs(ryIn)
        if rx == 0 || ry == 0 { return [p2] }
        let phi = rotation * .pi / 180
        let cp = cos(phi), sp = sin(phi)
        let dx = (p1.x - p2.x) / 2, dy = (p1.y - p2.y) / 2
        let x1p = cp * dx + sp * dy, y1p = -sp * dx + cp * dy
        let lam = x1p * x1p / (rx * rx) + y1p * y1p / (ry * ry)
        if lam > 1 { rx *= sqrt(lam); ry *= sqrt(lam) }
        let num = rx * rx * ry * ry - rx * rx * y1p * y1p - ry * ry * x1p * x1p
        let den = rx * rx * y1p * y1p + ry * ry * x1p * x1p
        var coef = den == 0 ? 0 : sqrt(max(0, num / den))
        if large == sweep { coef = -coef }
        let cxp = coef * rx * y1p / ry, cyp = -coef * ry * x1p / rx
        let cx = cp * cxp - sp * cyp + (p1.x + p2.x) / 2
        let cy = sp * cxp + cp * cyp + (p1.y + p2.y) / 2
        func angle(_ ux: CGFloat, _ uy: CGFloat, _ vx: CGFloat, _ vy: CGFloat) -> CGFloat { atan2(ux * vy - uy * vx, ux * vx + uy * vy) }
        let t1 = angle(1, 0, (x1p - cxp) / rx, (y1p - cyp) / ry)
        var dt = angle((x1p - cxp) / rx, (y1p - cyp) / ry, (-x1p - cxp) / rx, (-y1p - cyp) / ry)
        if !sweep && dt > 0 { dt -= 2 * .pi } else if sweep && dt < 0 { dt += 2 * .pi }
        let n = 20
        return (1...n).map { k in
            let t = t1 + dt * CGFloat(k) / CGFloat(n)
            let x = rx * cos(t), y = ry * sin(t)
            return CGPoint(x: cp * x - sp * y + cx, y: sp * x + cp * y + cy)
        }
    }
}
