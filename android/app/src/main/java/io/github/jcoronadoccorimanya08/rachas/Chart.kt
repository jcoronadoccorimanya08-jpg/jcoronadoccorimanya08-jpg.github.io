package io.github.jcoronadoccorimanya08.rachas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import org.json.JSONObject
import java.time.LocalDate

/** Un día del resumen: fecha, % y si se ganó (o fue limpio). */
data class Day(val date: LocalDate, val pct: Int, val won: Boolean)

/** Dibuja la montaña de tus últimos días y, debajo, los últimos 7 días con su check. */
object Chart {
    private val INITIALS = arrayOf("L", "M", "M", "J", "V", "S", "D")

    fun parseDays(data: JSONObject): List<Day> {
        val arr = data.optJSONArray("days") ?: return emptyList()
        val out = ArrayList<Day>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val date = try { LocalDate.parse(o.optString("d")) } catch (_: Exception) { continue }
            out.add(Day(date, o.optInt("p").coerceIn(0, 100), o.optBoolean("w")))
        }
        return out
    }

    fun draw(context: Context, days: List<Day>, goal: Int, today: LocalDate, p: Palette, hot: Boolean): Bitmap {
        val d = context.resources.displayMetrics.density
        val w = (300 * d).toInt()
        val h = (118 * d).toInt()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val line = if (hot) 0xFFFFFFFF.toInt() else p.accent
        val soft = if (hot) 0xCCFFFFFF.toInt() else p.info
        val faint = if (hot) 0x55FFFFFF else p.faint

        // ---- Montaña (arriba)
        val top = 6 * d
        val mh = 58 * d
        val left = 4 * d
        val right = w - 4 * d
        val n = maxOf(7, days.size)
        fun x(i: Int) = left + (right - left) * i / (n - 1).coerceAtLeast(1)
        fun y(v: Int) = top + (1f - v / 100f) * mh

        val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = faint; strokeWidth = 1 * d }
        c.drawLine(left, y(0), right, y(0), grid)
        if (goal in 1..99) {
            val dash = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (hot) 0x99FFFFFF.toInt() else 0x80FFFFFF.toInt()
                strokeWidth = 1.2f * d
                pathEffect = DashPathEffect(floatArrayOf(5 * d, 4 * d), 0f)
            }
            c.drawLine(left, y(goal), right, y(goal), dash)
        }
        if (days.isNotEmpty()) {
            val path = Path()
            path.moveTo(x(0), y(days[0].pct))
            for (i in 1 until days.size) {
                val x0 = x(i - 1)
                val x1 = x(i)
                val cx = (x0 + x1) / 2
                path.cubicTo(cx, y(days[i - 1].pct), cx, y(days[i].pct), x1, y(days[i].pct))
            }
            val area = Path(path)
            area.lineTo(x(days.size - 1), y(0))
            area.lineTo(x(0), y(0))
            area.close()
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, top, 0f, top + mh, (line and 0x00FFFFFF) or 0x8C000000.toInt(), (line and 0x00FFFFFF) or 0x08000000, Shader.TileMode.CLAMP)
            }
            c.drawPath(area, fill)
            c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = line
                strokeWidth = 2.4f * d
            })
            val dot = Paint(Paint.ANTI_ALIAS_FLAG)
            days.forEachIndexed { i, day ->
                dot.color = if (day.won) line else soft
                c.drawCircle(x(i), y(day.pct), (if (day.won) 3.2f else 2.6f) * d, dot)
            }
        }

        // ---- Últimos 7 días (abajo)
        val byDate = days.associateBy { it.date }
        val rowTop = top + mh + 10 * d
        val r = 11 * d
        val slot = (right - left) / 7f
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (hot) 0xE6FFFFFF.toInt() else p.muted
            textSize = 9.5f * d
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.6f * d }
        val solid = Paint(Paint.ANTI_ALIAS_FLAG)
        val check = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.2f * d
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = if (hot) 0xFFB8321A.toInt() else 0xFF0B0E13.toInt()
        }
        for (i in 0 until 7) {
            val date = today.minusDays((6 - i).toLong())
            val cx = left + slot * i + slot / 2
            val cy = rowTop + r + 12 * d
            c.drawText(INITIALS[date.dayOfWeek.value - 1], cx, rowTop + 8 * d, label)
            val day = byDate[date]
            when {
                day?.won == true -> {
                    solid.color = line
                    c.drawCircle(cx, cy, r, solid)
                    val pth = Path()
                    pth.moveTo(cx - r * 0.42f, cy + r * 0.02f)
                    pth.lineTo(cx - r * 0.1f, cy + r * 0.34f)
                    pth.lineTo(cx + r * 0.46f, cy - r * 0.3f)
                    c.drawPath(pth, check)
                }
                day != null -> {
                    ring.color = if (date == today) line else faint
                    c.drawCircle(cx, cy, r, ring)
                }
                else -> {
                    ring.color = faint
                    c.drawCircle(cx, cy, r * 0.35f, ring)
                }
            }
        }
        return bmp
    }
}
