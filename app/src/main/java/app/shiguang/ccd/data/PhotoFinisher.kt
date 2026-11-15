package app.shiguang.ccd.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import app.shiguang.imaging.BorderStyle
import app.shiguang.imaging.LookParams
import app.shiguang.imaging.TimestampStyle
import app.shiguang.imaging.TimestampText
import kotlin.math.max

/**
 * Draws everything that is not part of the pixel pipeline: the paper border and
 * the date stamp.  Kept separate so the renderer stays resolution-agnostic.
 */
object PhotoFinisher {

    fun finish(source: Bitmap, params: LookParams, timeMillis: Long): Bitmap {
        var current = source
        if (params.borderStyle == BorderStyle.PAPER_WHITE) {
            current = addPaperBorder(current)
        }
        val text = TimestampText.format(params.timestampStyle, timeMillis)
        if (text.isNotEmpty()) {
            current = drawTimestamp(current, text, params.timestampStyle)
        }
        return current
    }

    private fun addPaperBorder(source: Bitmap): Bitmap {
        val margin = max(10, (source.width * 0.05f).toInt())
        val bottom = margin * 2
        val width = source.width + margin * 2
        val height = source.height + margin + bottom
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.parseColor("#F7F1E3"))
        canvas.drawBitmap(source, margin.toFloat(), margin.toFloat(), null)

        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(1f, width * 0.002f)
            color = Color.parseColor("#D9CFBB")
        }
        canvas.drawRect(
            Rect(margin, margin, margin + source.width, margin + source.height),
            border,
        )
        return out
    }

    private fun drawTimestamp(source: Bitmap, text: String, style: TimestampStyle): Bitmap {
        val out = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        val size = max(14f, out.width * 0.042f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = size
            color = when (style) {
                TimestampStyle.ORANGE_LED -> Color.parseColor("#FF9A2E")
                TimestampStyle.WHITE_DIGITAL -> Color.parseColor("#FFF3E0")
                TimestampStyle.NONE -> Color.WHITE
            }
            setShadowLayer(max(1f, size * 0.08f), 0f, 0f, Color.argb(140, 0, 0, 0))
        }
        val padding = out.width * 0.035f
        val y = out.height - padding
        val x = out.width - padding - paint.measureText(text)
        canvas.drawText(text, x, y, paint)
        return out
    }
}
