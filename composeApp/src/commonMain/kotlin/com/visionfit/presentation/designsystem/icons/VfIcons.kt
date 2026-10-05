package com.visionfit.presentation.designsystem.icons

/** Which color a sub-path is filled with when the icon is drawn. */
enum class FillRole { None, Body, Accent }

class VfPath(val data: String, val fill: FillRole = FillRole.None)

/**
 * A 24 × 24 stroke icon (round caps and joins) ported from the design's inline SVGs.
 * Paths are kept as SVG path data so they can be compared with the source one to one.
 */
class IconSpec(val name: String, val strokeWidth: Float, val paths: List<VfPath>)

private fun icon(name: String, strokeWidth: Float, vararg paths: VfPath) = IconSpec(name, strokeWidth, paths.toList())

private fun p(data: String, fill: FillRole = FillRole.None) = VfPath(data, fill)

private fun circle(cx: Float, cy: Float, r: Float, fill: FillRole = FillRole.None) =
    VfPath("M${cx - r},${cy}a$r,$r 0 1,0 ${2 * r},0a$r,$r 0 1,0 ${-2 * r},0z", fill)

private fun rect(x: Float, y: Float, w: Float, h: Float, rx: Float) = VfPath(
    "M${x + rx},${y}h${w - 2 * rx}a$rx,$rx 0 0 1 $rx,${rx}v${h - 2 * rx}" +
        "a$rx,$rx 0 0 1 ${-rx},${rx}h${-(w - 2 * rx)}a$rx,$rx 0 0 1 ${-rx},${-rx}" +
        "v${-(h - 2 * rx)}a$rx,$rx 0 0 1 $rx,${-rx}z",
)

object VfIcons {
    val ScanLeaf = icon(
        "scan-leaf", 2.4f,
        p("M4 8V6a2 2 0 0 1 2-2h2M16 4h2a2 2 0 0 1 2 2v2M20 16v2a2 2 0 0 1-2 2h-2M8 20H6a2 2 0 0 1-2-2v-2"),
        p("M8.5 15.5c0-4 3-7 7-7 0 4-3 7-7 7z"),
        p("M8.5 15.5l3.5-3.5"),
    )
    val Camera = icon(
        "camera", 2.2f,
        p("M4 8h3l2-3h6l2 3h3a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1z", FillRole.Body),
        circle(12f, 13f, 4f, FillRole.Accent),
    )
    val Mail = icon("mail", 2.2f, rect(3f, 5f, 18f, 14f, 2f), p("M3 7l9 6 9-6"))
    val Lock = icon("lock", 2.2f, rect(4f, 11f, 16f, 10f, 2f), p("M8 11V7a4 4 0 0 1 8 0v4"))
    val Eye = icon("eye", 2.2f, p("M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"), circle(12f, 12f, 3f))
    val EyeOff = icon(
        "eye-off", 2.2f,
        p("M3 3l18 18"),
        p("M10.6 5.1A10 10 0 0 1 12 5c6.5 0 10 7 10 7a17 17 0 0 1-3.2 4.1M6.6 6.6C3.8 8.4 2 12 2 12s3.5 7 10 7c1.8 0 3.4-.5 4.8-1.3"),
        p("M9.9 9.9a3 3 0 0 0 4.2 4.2"),
    )
    val ShieldCheck = icon("shield-check", 2.2f, p("M12 3l7 3v6c0 4.5-3 7.5-7 9-4-1.5-7-4.5-7-9V6z"), p("M9 12l2 2 4-4"))
    val ArrowRight = icon("arrow-right", 2.6f, p("M5 12h14M13 6l6 6-6 6"))
    val ChevronLeft = icon("chevron-left", 2.6f, p("M15 18l-6-6 6-6"))
    val ChevronRight = icon("chevron-right", 2.4f, p("M9 6l6 6-6 6"))
    val Plus = icon("plus", 3f, p("M12 5v14M5 12h14"))
    val Minus = icon("minus", 3f, p("M5 12h14"))
    val Sofa = icon(
        "sofa", 2.2f,
        p("M4 11V8a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v3"),
        p("M2 13a2 2 0 0 1 4 0v2h12v-2a2 2 0 0 1 4 0v5H2z"),
        p("M5 18v2M19 18v2"),
    )
    val Walk = icon(
        "walk", 2.2f,
        circle(13f, 4f, 2f),
        p("M9 21l2.5-6.5L14 17v4"),
        p("M7 12l3-4 4 1.5 2.5 3.5"),
    )
    val Bike = icon(
        "bike", 2.2f,
        circle(5.5f, 17f, 3.5f),
        circle(18.5f, 17f, 3.5f),
        p("M5.5 17l4-7h5l4 7M9.5 10l-1-3H6.5M13 10l2 7"),
    )
    val Dumbbell = icon("dumbbell", 2.4f, p("M6 6v12M18 6v12M3 9v6M21 9v6M6 12h12"))
    val TrendDown = icon("trend-down", 2.6f, p("M3 7l6 6 4-4 8 8"), p("M21 11v6h-6"))
    val TrendUp = icon("trend-up", 2.6f, p("M3 17l6-6 4 4 8-8"), p("M21 13V7h-6"))
    val Check = icon("check", 3.6f, p("M5 12.5l4.5 4.5L19 7.5"))
    val Pencil = icon("pencil", 2.4f, p("M4 20h4L19 9l-4-4L4 16z"), p("M13.5 6.5l4 4"))
    val Bell = icon(
        "bell", 2.2f,
        p("M6 9a6 6 0 0 1 12 0c0 6 2.5 7.5 2.5 7.5h-17S6 15 6 9z"),
        p("M10 20a2.2 2.2 0 0 0 4 0"),
    )
    val WifiOff = icon(
        "wifi-off", 2.2f,
        p("M3 3l18 18"),
        p("M8 7.2A6 6 0 0 1 17.7 10H18a4 4 0 0 1 2.5 7.1M17 19H7a5 5 0 0 1-2.5-9.3"),
    )
    val Flame = icon(
        "flame", 2f,
        p("M12 3c1 4 5 5.5 5 10a5 5 0 0 1-10 0c0-2.5 1.5-4 2.5-5 .3 2 1.3 3 2.5 3 0-3-1-5.5 0-8z", FillRole.Body),
    )
    val Sparkle = icon("sparkle", 2f, p("M12 2l2.2 6.6L21 11l-6.8 2.4L12 20l-2.2-6.6L3 11l6.8-2.4z", FillRole.Body))
    val Home = icon("home", 2f, p("M3 10.5L12 4l9 6.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z", FillRole.Body))
    val Clock = icon("clock", 2f, circle(12f, 12f, 9f, FillRole.Body), p("M12 7v5l3 2"))
    val Target = icon("target", 2f, circle(12f, 12f, 9f), circle(12f, 12f, 5f), circle(12f, 12f, 1f))
    val User = icon("user", 2f, circle(12f, 8f, 4f), p("M4 21c0-4 4-6 8-6s8 2 8 6"))
    val Close = icon("close", 2.8f, p("M18 6L6 18M6 6l12 12"))
    val Bolt = icon("bolt", 2.2f, p("M13 2L4 14h7l-1 8 9-12h-7z", FillRole.Body))
    val SwitchCamera = icon(
        "switch-camera", 2.4f,
        p("M4 12a8 8 0 0 1 14-5.3L20 9"),
        p("M20 4v5h-5"),
        p("M20 12a8 8 0 0 1-14 5.3L4 15"),
        p("M4 20v-5h5"),
    )
    val Alert = icon("alert", 3f, p("M12 6v8M12 18h.01"))
    val Retry = icon("retry", 2.8f, p("M20 11a8 8 0 1 0-2.3 5.7"), p("M20 4v7h-7"))
    val Trash = icon("trash", 2.4f, p("M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3"))
    val Calendar = icon("calendar", 2.2f, rect(3f, 5f, 18f, 16f, 2f), p("M3 10h18M8 3v4M16 3v4"))
    val CloudCheck = icon(
        "cloud-check", 2.2f,
        p("M7 19a5 5 0 0 1-.6-10A6 6 0 0 1 18 9a4 4 0 0 1 0 10z"),
        p("M9.5 14l2 2 3.5-3.5"),
    )
    val Plate = icon("plate", 2f, circle(12f, 12f, 9f, FillRole.Body), circle(12f, 12f, 5f))
    val Gallery = icon("gallery", 2.2f, rect(3f, 4f, 18f, 16f, 2f), circle(8.5f, 9.5f, 1.5f), p("M21 16l-5-5L5 20"))
}
