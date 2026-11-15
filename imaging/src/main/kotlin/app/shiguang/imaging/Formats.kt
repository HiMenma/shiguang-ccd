package app.shiguang.imaging

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.serialization.Serializable

@Serializable
enum class TimestampStyle {
    NONE,
    ORANGE_LED,
    WHITE_DIGITAL,
}

@Serializable
enum class BorderStyle {
    NONE,
    PAPER_WHITE,
}

enum class AspectRatioOption(val label: String, val ratio: Float) {
    RATIO_4_3("4:3", 4f / 3f),
    RATIO_1_1("1:1", 1f),
    RATIO_16_9("16:9", 16f / 9f),
}

object TimestampText {

    private val orange = DateTimeFormatter.ofPattern("yyyy MM dd")
    private val white = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")

    fun format(style: TimestampStyle, epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        when (style) {
            TimestampStyle.NONE -> ""
            TimestampStyle.ORANGE_LED -> orange.format(Instant.ofEpochMilli(epochMillis).atZone(zone))
            TimestampStyle.WHITE_DIGITAL -> white.format(Instant.ofEpochMilli(epochMillis).atZone(zone))
        }
}
