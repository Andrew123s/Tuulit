package app.tuulit.ui.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Tuulit line icons: 24px grid, 1.75px stroke, round caps. Generated from the design system's
 * SVG icon set so the app and the design match exactly. Tint them with Icon(tint = ...).
 */
object TuulitIcons {
    private class P(val d: String, val filled: Boolean = false)

    private fun icon(name: String, vararg parts: P): ImageVector {
        val b = ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        parts.forEach { p ->
            val nodes = PathParser().parsePathString(p.d).toNodes()
            if (p.filled) {
                b.addPath(nodes, fill = SolidColor(Color.Black))
            } else {
                b.addPath(nodes, stroke = SolidColor(Color.Black), strokeLineWidth = 1.75f,
                    strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            }
        }
        return b.build()
    }

    val Today = icon("today", P("M8 12a4 4 0 1 0 8 0a4 4 0 1 0 -8 0Z"), P("M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6l1.4 1.4M17 17l1.4 1.4M5.6 18.4L7 17M17 7l1.4-1.4"))
    val Cycle = icon("cycle", P("M6.5 5h11a3 3 0 0 1 3 3v9a3 3 0 0 1 -3 3h-11a3 3 0 0 1 -3 -3v-9a3 3 0 0 1 3 -3Z"), P("M3.5 10h17M8 3v4M16 3v4"), P("M10.6 15a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0 -2.8 0Z", true))
    val Plus = icon("plus", P("M12 5v14M5 12h14"))
    val Minus = icon("minus", P("M5 12h14"))
    val Care = icon("care", P("M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z"))
    val Ask = icon("ask", P("M5 18.5V7a3 3 0 0 1 3-3h8a3 3 0 0 1 3 3v6a3 3 0 0 1-3 3H8.5z"), P("M9 9.5h6M9 12.5h4"))
    val Drop = icon("drop", P("M12 3.5s6 6.6 6 11a6 6 0 0 1-12 0c0-4.4 6-11 6-11z"))
    val ChevR = icon("chev-r", P("M9.5 6l6 6-6 6"))
    val ChevL = icon("chev-l", P("M14.5 6l-6 6 6 6"))
    val ChevD = icon("chev-d", P("M6 9.5l6 6 6-6"))
    val Close = icon("close", P("M6 6l12 12M18 6L6 18"))
    val Check = icon("check", P("M5 12.5l4.5 4.5L19 7.5"))
    val Bell = icon("bell", P("M6.5 16.5V11a5.5 5.5 0 0 1 11 0v5.5l1.5 1.5H5z"), P("M10 20.5h4"))
    val Lock = icon("lock", P("M7.5 10.5h9a2.5 2.5 0 0 1 2.5 2.5v5a2.5 2.5 0 0 1 -2.5 2.5h-9a2.5 2.5 0 0 1 -2.5 -2.5v-5a2.5 2.5 0 0 1 2.5 -2.5Z"), P("M8.5 10.5V8a3.5 3.5 0 0 1 7 0v2.5"))
    val User = icon("user", P("M8.5 8.5a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0 -7 0Z"), P("M5 20a7 7 0 0 1 14 0"))
    val Users = icon("users", P("M6 9a3 3 0 1 0 6 0a3 3 0 1 0 -6 0Z"), P("M3.5 19a5.5 5.5 0 0 1 11 0"), P("M15 6.2a3 3 0 0 1 0 5.6M17 14.2a5.5 5.5 0 0 1 3.5 4.8"))
    val Settings = icon("settings", P("M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0Z"), P("M12 3.5v2.2M12 18.3v2.2M3.5 12h2.2M18.3 12h2.2M6 6l1.6 1.6M16.4 16.4L18 18M6 18l1.6-1.6M16.4 7.6L18 6"))
    val Moon = icon("moon", P("M19 14.5A7.5 7.5 0 0 1 9.5 5a7.5 7.5 0 1 0 9.5 9.5z"))
    val Breath = icon("breath", P("M3.5 9h10a3 3 0 1 0-3-3M3.5 13h14a3 3 0 1 1-3 3M3.5 17h6"))
    val Heat = icon("heat", P("M10 7h4a4 4 0 0 1 4 4v5.5a4 4 0 0 1 -4 4h-4a4 4 0 0 1 -4 -4v-5.5a4 4 0 0 1 4 -4Z"), P("M10 7V4.5h4V7M9.5 12.5c1-.8 2-.8 3 0s2 .8 3 0M9.5 16c1-.8 2-.8 3 0s2 .8 3 0"))
    val Play = icon("play", P("M8 4h8a4 4 0 0 1 4 4v8a4 4 0 0 1 -4 4h-8a4 4 0 0 1 -4 -4v-8a4 4 0 0 1 4 -4Z"), P("M7.7 9a1.3 1.3 0 1 0 2.6 0a1.3 1.3 0 1 0 -2.6 0Z", true), P("M13.7 15a1.3 1.3 0 1 0 2.6 0a1.3 1.3 0 1 0 -2.6 0Z", true), P("M13.7 9a1.3 1.3 0 1 0 2.6 0a1.3 1.3 0 1 0 -2.6 0Z", true), P("M7.7 15a1.3 1.3 0 1 0 2.6 0a1.3 1.3 0 1 0 -2.6 0Z", true))
    val Shield = icon("shield", P("M12 3.5l7 2.8v5.4c0 4.3-3 7.6-7 8.8-4-1.2-7-4.5-7-8.8V6.3z"), P("M9 12l2.2 2.2L15 10.4"))
    val Send = icon("send", P("M4.5 12L19.5 4.5 15 19.5l-3-6z"), P("M12 13.5l7.5-9"))
    val Share = icon("share", P("M4 12a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0 -5 0Z"), P("M15 6a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0 -5 0Z"), P("M15 18a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0 -5 0Z"), P("M8.7 10.8l6.6-3.6M8.7 13.2l6.6 3.6"))
    val Info = icon("info", P("M3.5 12a8.5 8.5 0 1 0 17 0a8.5 8.5 0 1 0 -17 0Z"), P("M12 11v5.5M12 7.8v.2"))
    val Alert = icon("alert", P("M12 4l9 15.5H3z"), P("M12 10v4.5M12 17v.2"))
    val Edit = icon("edit", P("M5 19l1-4.2L15.6 5.2a2 2 0 0 1 2.8 0l.4.4a2 2 0 0 1 0 2.8L9.2 18z"))
    val Trash = icon("trash", P("M4.5 7h15M9.5 7V4.5h5V7M6.5 7l1 13h9l1-13"))
    val Spark = icon("spark", P("M12 3.5c.7 4.3 2.2 5.8 6.5 6.5-4.3.7-5.8 2.2-6.5 6.5-.7-4.3-2.2-5.8-6.5-6.5 4.3-.7 5.8-2.2 6.5-6.5z"), P("M18.5 16v4M16.5 18h4"))
    val Book = icon("book", P("M4.5 5.5c2.8-1 5.3-.8 7.5.8 2.2-1.6 4.7-1.8 7.5-.8v13c-2.8-1-5.3-.8-7.5.8-2.2-1.6-4.7-1.8-7.5-.8z"), P("M12 6.3v13"))
    val Chart = icon("chart", P("M4.5 19.5h15"), P("M7 16v-4M11 16V7M15 16v-6M19 16v-9"))
    val Pain = icon("pain", P("M12 3.5l1.6 5 5.2-.4-4.2 3.1 2 4.9-4.6-2.8L7.4 16l2-4.9L5.2 8.1l5.2.4z"))
    val Pill = icon("pill", P("M7.1 17.6l-0.7-0.7a3.5 3.5 0 0 1 0-4.9l5.6-5.6a3.5 3.5 0 0 1 4.9 0l0.7 0.7a3.5 3.5 0 0 1 0 4.9l-5.6 5.6a3.5 3.5 0 0 1-4.9 0Z"), P("M9.2 9.2l5.6 5.6"))
    val Mic = icon("mic", P("M12 3.5h0a3 3 0 0 1 3 3v5a3 3 0 0 1 -3 3h-0a3 3 0 0 1 -3 -3v-5a3 3 0 0 1 3 -3Z"), P("M6 11.5a6 6 0 0 0 12 0M12 17.5v3"))
    val ArrowUp = icon("arrow-up", P("M12 19V5.5M6.5 11L12 5.5 17.5 11"))
    val Sos = icon("sos", P("M3.5 12a8.5 8.5 0 1 0 17 0a8.5 8.5 0 1 0 -17 0Z"), P("M12 7.5v5M12 15.8v.2"))
    val Kit = icon("kit", P("M7 7.5h10a3 3 0 0 1 3 3v6a3 3 0 0 1 -3 3h-10a3 3 0 0 1 -3 -3v-6a3 3 0 0 1 3 -3Z"), P("M9 7.5V5.5h6v2M12 11v5M9.5 13.5h5"))
    val Music = icon("music", P("M9 18V6l10-2v12"), P("M4 18a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0 -5 0Z"), P("M14 16a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0 -5 0Z"))
    val Stretch = icon("stretch", P("M10.2 5a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0 -3.6 0Z"), P("M5 9.5l7 1.5 7-1.5M12 11v4l-3 5M12 15l3 5"))
    val Food = icon("food", P("M6 11.5h12a6 6 0 0 1-12 0z"), P("M9.5 8c0-1.5 1-1.5 1-3M13.5 8c0-1.5 1-1.5 1-3"))
    val Quiz = icon("quiz", P("M3.5 12a8.5 8.5 0 1 0 17 0a8.5 8.5 0 1 0 -17 0Z"), P("M9.6 9.5a2.5 2.5 0 1 1 3.4 2.3c-.7.3-1 .8-1 1.5v.4M12 16.3v.2"))
    val Memory = icon("memory", P("M5.5 5h3.5a2 2 0 0 1 2 2v6a2 2 0 0 1 -2 2h-3.5a2 2 0 0 1 -2 -2v-6a2 2 0 0 1 2 -2Z"), P("M15 9h3.5a2 2 0 0 1 2 2v6a2 2 0 0 1 -2 2h-3.5a2 2 0 0 1 -2 -2v-6a2 2 0 0 1 2 -2Z"))
    val Eye = icon("eye", P("M2.8 12S6 6 12 6s9.2 6 9.2 6-3.2 6-9.2 6-9.2-6-9.2-6z"), P("M9.4 12a2.6 2.6 0 1 0 5.2 0a2.6 2.6 0 1 0 -5.2 0Z"))
    val Download = icon("download", P("M12 4.5V15M7 10.5l5 5 5-5M5 19.5h14"))
    val Back = icon("back", P("M19 12H5.5M11 6l-6 6 6 6"))
    val More = icon("more", P("M4.7 12a1.3 1.3 0 1 0 2.6 0a1.3 1.3 0 1 0 -2.6 0Z", true), P("M10.7 12a1.3 1.3 0 1 0 2.6 0a1.3 1.3 0 1 0 -2.6 0Z", true), P("M16.7 12a1.3 1.3 0 1 0 2.6 0a1.3 1.3 0 1 0 -2.6 0Z", true))
    val Google = icon("google", P("M3.5 12a8.5 8.5 0 1 0 17 0a8.5 8.5 0 1 0 -17 0Z"))
    val Mail = icon("mail", P("M6 5.5h12a2.5 2.5 0 0 1 2.5 2.5v8a2.5 2.5 0 0 1 -2.5 2.5h-12a2.5 2.5 0 0 1 -2.5 -2.5v-8a2.5 2.5 0 0 1 2.5 -2.5Z"), P("M4 7l8 6 8-6"))
    val Phone = icon("phone", P("M9.5 3h5a2.5 2.5 0 0 1 2.5 2.5v13a2.5 2.5 0 0 1 -2.5 2.5h-5a2.5 2.5 0 0 1 -2.5 -2.5v-13a2.5 2.5 0 0 1 2.5 -2.5Z"), P("M11 18h2"))
    val Wifi = icon("wifi", P("M3.5 9a12 12 0 0 1 17 0M6.5 12.2a8 8 0 0 1 11 0M9.5 15.4a4 4 0 0 1 5 0"), P("M11 18.5a1 1 0 1 0 2 0a1 1 0 1 0 -2 0Z", true))
    val Refresh = icon("refresh", P("M19 12a7 7 0 1 1-2-4.9M19 4.5v4h-4"))

    /** Lookup by the design system's icon name (used by content definitions such as CareContent). */
    fun named(name: String): ImageVector = when (name) {
        "today" -> Today
        "cycle" -> Cycle
        "plus" -> Plus
        "minus" -> Minus
        "care" -> Care
        "ask" -> Ask
        "drop" -> Drop
        "chev-r" -> ChevR
        "chev-l" -> ChevL
        "chev-d" -> ChevD
        "close" -> Close
        "check" -> Check
        "bell" -> Bell
        "lock" -> Lock
        "user" -> User
        "users" -> Users
        "settings" -> Settings
        "moon" -> Moon
        "breath" -> Breath
        "heat" -> Heat
        "play" -> Play
        "shield" -> Shield
        "send" -> Send
        "share" -> Share
        "info" -> Info
        "alert" -> Alert
        "edit" -> Edit
        "trash" -> Trash
        "spark" -> Spark
        "book" -> Book
        "chart" -> Chart
        "pain" -> Pain
        "pill" -> Pill
        "mic" -> Mic
        "arrow-up" -> ArrowUp
        "sos" -> Sos
        "kit" -> Kit
        "music" -> Music
        "stretch" -> Stretch
        "food" -> Food
        "quiz" -> Quiz
        "memory" -> Memory
        "eye" -> Eye
        "download" -> Download
        "back" -> Back
        "more" -> More
        "google" -> Google
        "mail" -> Mail
        "phone" -> Phone
        "wifi" -> Wifi
        "refresh" -> Refresh
        else -> Info
    }
}
