import SwiftUI

/// Tuulit line icons: 24-unit grid, 1.75 stroke, round caps. Path data copied from Android
/// `TuulitIcons.kt` (generated from the design SVGs) so both apps draw identical icons.
struct IconDef: Hashable {
    let name: String
    /// (path, filled)
    let parts: [Part]
    struct Part: Hashable { let d: String; let filled: Bool }
}

private func icon(_ name: String, _ parts: (String, Bool)...) -> IconDef {
    IconDef(name: name, parts: parts.map { IconDef.Part(d: $0.0, filled: $0.1) })
}
private func s(_ d: String) -> (String, Bool) { (d, false) }
private func f(_ d: String) -> (String, Bool) { (d, true) }

enum Icons {
    static let today = icon("today", s("M8 12a4 4 0 1 0 8 0a4 4 0 1 0 -8 0Z"), s("M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6l1.4 1.4M17 17l1.4 1.4M5.6 18.4L7 17M17 7l1.4-1.4"))
    static let cycle = icon("cycle", s("M6.5 5h11a3 3 0 0 1 3 3v9a3 3 0 0 1 -3 3h-11a3 3 0 0 1 -3 -3v-9a3 3 0 0 1 3 -3Z"), s("M3.5 10h17M8 3v4M16 3v4"), f("M10.6 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0Z"))
    static let plus = icon("plus", s("M12 5v14M5 12h14"))
    static let minus = icon("minus", s("M5 12h14"))
    static let care = icon("care", s("M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z"))
    static let ask = icon("ask", s("M5 18.5V7a3 3 0 0 1 3-3h8a3 3 0 0 1 3 3v6a3 3 0 0 1-3 3H8.5z"), s("M9 9.5h6M9 12.5h4"))
    static let drop = icon("drop", s("M12 3.5s6 6.6 6 11a6 6 0 0 1-12 0c0-4.4 6-11 6-11z"))
    static let chevR = icon("chev-r", s("M9.5 6l6 6-6 6"))
    static let chevL = icon("chev-l", s("M14.5 6l-6 6 6 6"))
    static let chevD = icon("chev-d", s("M6 9.5l6 6 6-6"))
    static let close = icon("close", s("M6 6l12 12M18 6L6 18"))
    static let check = icon("check", s("M5 12.5l4.5 4.5L19 7.5"))
    static let bell = icon("bell", s("M6.5 16.5V11a5.5 5.5 0 0 1 11 0v5.5l1.5 1.5H5z"), s("M10 20.5h4"))
    static let lock = icon("lock", s("M7.5 10.5h9a2.5 2.5 0 0 1 2.5 2.5v5a2.5 2.5 0 0 1 -2.5 2.5h-9a2.5 2.5 0 0 1 -2.5 -2.5v-5a2.5 2.5 0 0 1 2.5 -2.5Z"), s("M8.5 10.5V8a3.5 3.5 0 0 1 7 0v2.5"))
    static let user = icon("user", s("M8.5 8.5a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0 -7 0Z"), s("M5 20a7 7 0 0 1 14 0"))
    static let users = icon("users", s("M6 9a3 3 0 1 0 6 0a3 3 0 1 0 -6 0Z"), s("M3.5 19a5.5 5.5 0 0 1 11 0"), s("M15 6.2a3 3 0 0 1 0 5.6M17 14.2a5.5 5.5 0 0 1 3.5 4.8"))
    static let moon = icon("moon", s("M19 14.5A7.5 7.5 0 0 1 9.5 5a7.5 7.5 0 1 0 9.5 9.5z"))
    static let breath = icon("breath", s("M3.5 9h10a3 3 0 1 0-3-3M3.5 13h14a3 3 0 1 1-3 3M3.5 17h6"))
    static let heat = icon("heat", s("M10 7h4a4 4 0 0 1 4 4v5.5a4 4 0 0 1 -4 4h-4a4 4 0 0 1 -4 -4v-5.5a4 4 0 0 1 4 -4Z"), s("M10 7V4.5h4V7M9.5 12.5c1-.8 2-.8 3 0s2 .8 3 0M9.5 16c1-.8 2-.8 3 0s2 .8 3 0"))
    static let play = icon("play", s("M8 4h8a4 4 0 0 1 4 4v8a4 4 0 0 1 -4 4h-8a4 4 0 0 1 -4 -4v-8a4 4 0 0 1 4 -4Z"), f("M7.7 9a1.3 1.3 0 1 0 2.6 0a1.3 1.3 0 1 0 -2.6 0Z"), f("M13.7 15a1.3 1.3 0 1 0 2.6 0a1.3 1.3 0 1 0 -2.6 0Z"), f("M13.7 9a1.3 1.3 0 1 0 2.6 0a1.3 1.3 0 1 0 -2.6 0Z"), f("M7.7 15a1.3 1.3 0 1 0 2.6 0a1.3 1.3 0 1 0 -2.6 0Z"))
    static let shield = icon("shield", s("M12 3.5l7 2.8v5.4c0 4.3-3 7.6-7 8.8-4-1.2-7-4.5-7-8.8V6.3z"), s("M9 12l2.2 2.2L15 10.4"))
    static let send = icon("send", s("M4.5 12L19.5 4.5 15 19.5l-3-6z"), s("M12 13.5l7.5-9"))
    static let share = icon("share", s("M4 12a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0 -5 0Z"), s("M15 6a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0 -5 0Z"), s("M15 18a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0 -5 0Z"), s("M8.7 10.8l6.6-3.6M8.7 13.2l6.6 3.6"))
    static let info = icon("info", s("M3.5 12a8.5 8.5 0 1 0 17 0a8.5 8.5 0 1 0 -17 0Z"), s("M12 11v5.5M12 7.8v.2"))
    static let alert = icon("alert", s("M12 4l9 15.5H3z"), s("M12 10v4.5M12 17v.2"))
    static let edit = icon("edit", s("M5 19l1-4.2L15.6 5.2a2 2 0 0 1 2.8 0l.4.4a2 2 0 0 1 0 2.8L9.2 18z"))
    static let trash = icon("trash", s("M4.5 7h15M9.5 7V4.5h5V7M6.5 7l1 13h9l1-13"))
    static let spark = icon("spark", s("M12 3.5c.7 4.3 2.2 5.8 6.5 6.5-4.3.7-5.8 2.2-6.5 6.5-.7-4.3-2.2-5.8-6.5-6.5 4.3-.7 5.8-2.2 6.5-6.5z"), s("M18.5 16v4M16.5 18h4"))
    static let book = icon("book", s("M4.5 5.5c2.8-1 5.3-.8 7.5.8 2.2-1.6 4.7-1.8 7.5-.8v13c-2.8-1-5.3-.8-7.5.8-2.2-1.6-4.7-1.8-7.5-.8z"), s("M12 6.3v13"))
    static let chart = icon("chart", s("M4.5 19.5h15"), s("M7 16v-4M11 16V7M15 16v-6M19 16v-9"))
    static let pain = icon("pain", s("M12 3.5l1.6 5 5.2-.4-4.2 3.1 2 4.9-4.6-2.8L7.4 16l2-4.9L5.2 8.1l5.2.4z"))
    static let mic = icon("mic", s("M12 3.5h0a3 3 0 0 1 3 3v5a3 3 0 0 1 -3 3h-0a3 3 0 0 1 -3 -3v-5a3 3 0 0 1 3 -3Z"), s("M6 11.5a6 6 0 0 0 12 0M12 17.5v3"))
    static let arrowUp = icon("arrow-up", s("M12 19V5.5M6.5 11L12 5.5 17.5 11"))
    static let sos = icon("sos", s("M3.5 12a8.5 8.5 0 1 0 17 0a8.5 8.5 0 1 0 -17 0Z"), s("M12 7.5v5M12 15.8v.2"))
    static let kit = icon("kit", s("M7 7.5h10a3 3 0 0 1 3 3v6a3 3 0 0 1 -3 3h-10a3 3 0 0 1 -3 -3v-6a3 3 0 0 1 3 -3Z"), s("M9 7.5V5.5h6v2M12 11v5M9.5 13.5h5"))
    static let music = icon("music", s("M9 18V6l10-2v12"), s("M4 18a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0 -5 0Z"), s("M14 16a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0 -5 0Z"))
    static let stretch = icon("stretch", s("M10.2 5a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0 -3.6 0Z"), s("M5 9.5l7 1.5 7-1.5M12 11v4l-3 5M12 15l3 5"))
    static let food = icon("food", s("M6 11.5h12a6 6 0 0 1-12 0z"), s("M9.5 8c0-1.5 1-1.5 1-3M13.5 8c0-1.5 1-1.5 1-3"))
    static let quiz = icon("quiz", s("M3.5 12a8.5 8.5 0 1 0 17 0a8.5 8.5 0 1 0 -17 0Z"), s("M9.6 9.5a2.5 2.5 0 1 1 3.4 2.3c-.7.3-1 .8-1 1.5v.4M12 16.3v.2"))
    static let memory = icon("memory", s("M5.5 5h3.5a2 2 0 0 1 2 2v6a2 2 0 0 1 -2 2h-3.5a2 2 0 0 1 -2 -2v-6a2 2 0 0 1 2 -2Z"), s("M15 9h3.5a2 2 0 0 1 2 2v6a2 2 0 0 1 -2 2h-3.5a2 2 0 0 1 -2 -2v-6a2 2 0 0 1 2 -2Z"))
    static let eye = icon("eye", s("M2.8 12S6 6 12 6s9.2 6 9.2 6-3.2 6-9.2 6-9.2-6-9.2-6z"), s("M9.4 12a2.6 2.6 0 1 0 5.2 0a2.6 2.6 0 1 0 -5.2 0Z"))
    static let download = icon("download", s("M12 4.5V15M7 10.5l5 5 5-5M5 19.5h14"))
    static let back = icon("back", s("M19 12H5.5M11 6l-6 6 6 6"))
    static let mail = icon("mail", s("M6 5.5h12a2.5 2.5 0 0 1 2.5 2.5v8a2.5 2.5 0 0 1 -2.5 2.5h-12a2.5 2.5 0 0 1 -2.5 -2.5v-8a2.5 2.5 0 0 1 2.5 -2.5Z"), s("M4 7l8 6 8-6"))
    static let phone = icon("phone", s("M9.5 3h5a2.5 2.5 0 0 1 2.5 2.5v13a2.5 2.5 0 0 1 -2.5 2.5h-5a2.5 2.5 0 0 1 -2.5 -2.5v-13a2.5 2.5 0 0 1 2.5 -2.5Z"), s("M11 18h2"))
    static let wifi = icon("wifi", s("M3.5 9a12 12 0 0 1 17 0M6.5 12.2a8 8 0 0 1 11 0M9.5 15.4a4 4 0 0 1 5 0"), f("M11 18.5a1 1 0 1 0 2 0a1 1 0 1 0 -2 0Z"))
    static let refresh = icon("refresh", s("M19 12a7 7 0 1 1-2-4.9M19 4.5v4h-4"))

    /// Lookup by the design system's icon name (used by shared content such as Care sessions).
    static func named(_ name: String) -> IconDef {
        let all: [IconDef] = [today, cycle, plus, minus, care, ask, drop, chevR, chevL, chevD, close, check, bell, lock, user, users, moon,
                              breath, heat, play, shield, send, share, info, alert, edit, trash, spark, book, chart, pain, mic, arrowUp,
                              sos, kit, music, stretch, food, quiz, memory, eye, download, back, mail, phone, wifi, refresh]
        return all.first { $0.name == name } ?? info
    }
}

/// Draws an icon at any size in one colour. Decorative by default (hidden from VoiceOver).
struct TIcon: View {
    let def: IconDef
    var size: CGFloat = 24
    var color: Color? = nil
    @Environment(\.palette) private var c

    init(_ def: IconDef, size: CGFloat = 24, color: Color? = nil) { self.def = def; self.size = size; self.color = color }

    var body: some View {
        let tint = color ?? c.ink
        Canvas { ctx, sz in
            let k = sz.width / 24
            ctx.scaleBy(x: k, y: k)
            for part in def.parts {
                let p = SVGPath.path(part.d)
                if part.filled { ctx.fill(p, with: .color(tint)) }
                else { ctx.stroke(p, with: .color(tint), style: StrokeStyle(lineWidth: 1.75, lineCap: .round, lineJoin: .round)) }
            }
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }
}
