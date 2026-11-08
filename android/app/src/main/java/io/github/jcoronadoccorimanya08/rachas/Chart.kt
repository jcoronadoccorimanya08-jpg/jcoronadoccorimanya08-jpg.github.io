package io.github.jcoronadoccorimanya08.rachas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import org.json.JSONObject
import java.time.LocalDate

/** Un día del resumen: fecha, % y si se ganó (o fue limpio). */
data class Day(val date: LocalDate, val pct: Int, val won: Boolean)

/**
 * Dibuja el contenido de los widgets en un Bitmap: el anillo y el núcleo de hoy (Val Q),
 * la montaña de tus últimos días y los últimos 7 días con su check.
 */
object Chart {
    private val INITIALS = arrayOf("L", "M", "M", "J", "V", "S", "D")

    fun parseDays(data: JSONObject?): List<Day> {
        val arr = data?.optJSONArray("days") ?: return emptyList()
        val out = ArrayList<Day>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val date = try { LocalDate.parse(o.optString("d")) } catch (_: Exception) { continue }
            out.add(Day(date, o.optInt("p").coerceIn(0, 100), o.optBoolean("w")))
        }
        return out
    }

    /** Val Q: anillo con el % de hoy a la izquierda; a la derecha el núcleo, la montaña y la semana. */
    fun drawValQ(context: Context, data: JSONObject?, today: LocalDate, p: Palette): Bitmap {
        val d = context.resources.displayMetrics.density
        val w = (320 * d).toInt()
        val h = (98 * d).toInt()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val fresh = data?.optString("today") == today.toString()
        val pct = if (fresh) data!!.optInt("todayPct").coerceIn(0, 100) else 0
        val goal = data?.optInt("goal", 65) ?: 65

        // ---- Anillo
        val cx = 44 * d
        val cy = h / 2f
        val r = 36 * d
        val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 7 * d; color = p.faint }
        c.drawCircle(cx, cy, r, track)
        val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 7 * d; strokeCap = Paint.Cap.ROUND; color = p.accent }
        if (pct > 0) c.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), -90f, 360f * pct / 100f, false, arc)
        // Marca de la meta
        val ga = Math.toRadians((-90.0 + 360.0 * goal / 100.0))
        val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 2 * d; color = p.text; strokeCap = Paint.Cap.ROUND }
        c.drawLine(
            (cx + (r - 6 * d) * Math.cos(ga)).toFloat(), (cy + (r - 6 * d) * Math.sin(ga)).toFloat(),
            (cx + (r + 6 * d) * Math.cos(ga)).toFloat(), (cy + (r + 6 * d) * Math.sin(ga)).toFloat(), tick,
        )
        val big = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.text; textSize = 19 * d; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
        c.drawText("$pct%", cx, cy + 5 * d, big)
        val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.muted; textSize = 9 * d; textAlign = Paint.Align.CENTER }
        c.drawText("hoy", cx, cy + 17 * d, small)

        // ---- Núcleo de hoy
        val left = 96 * d
        val right = w - 2 * d
        val core = data?.optJSONArray("core")
        val n = core?.length() ?: 0
        if (n > 0) {
            val slot = (right - left) / n
            val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9.5f * d; typeface = Typeface.DEFAULT_BOLD }
            for (i in 0 until n) {
                val o = core!!.optJSONObject(i) ?: continue
                val x = left + slot * i + 8 * d
                val y = 13 * d
                val done = fresh && o.optBoolean("done")
                val counts = o.optBoolean("counts", true)
                drawMark(c, x, y, 7 * d, d, if (done) 2 else if (counts) 1 else 0, p)
                label.color = if (!counts) p.faint else if (done) p.text else p.muted
                c.drawText(o.optString("n"), x + 11 * d, y + 3.5f * d, label)
            }
        }

        // ---- Montaña y semana
        val days = parseDays(data)
        drawMountain(c, days, goal, left, right, 30 * d, 28 * d, d, p)
        drawWeek(c, days, today, left, right, 64 * d, 8 * d, d, p)
        return bmp
    }

    /** Bitácora: la montaña y los últimos 7 días (sin nombres). */
    fun drawBitacora(context: Context, data: JSONObject?, today: LocalDate, p: Palette): Bitmap {
        val d = context.resources.displayMetrics.density
        val w = (320 * d).toInt()
        val h = (84 * d).toInt()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val days = parseDays(data)
        drawMountain(c, days, 100, 2 * d, w - 2 * d, 4 * d, 38 * d, d, p)
        drawWeek(c, days, today, 2 * d, w - 2 * d, 48 * d, 9 * d, d, p)
        return bmp
    }

    /** 0 = no cuenta, 1 = pendiente, 2 = hecho. */
    private fun drawMark(c: Canvas, x: Float, y: Float, r: Float, d: Float, state: Int, p: Palette) {
        when (state) {
            2 -> {
                c.drawCircle(x, y, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.accent })
                val check = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE; strokeWidth = 1.8f * d; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; color = 0xFF0B0E13.toInt()
                }
                val path = Path()
                path.moveTo(x - r * 0.45f, y + r * 0.02f)
                path.lineTo(x - r * 0.1f, y + r * 0.36f)
                path.lineTo(x + r * 0.48f, y - r * 0.32f)
                c.drawPath(path, check)
            }
            1 -> c.drawCircle(x, y, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f * d; color = p.muted })
            else -> c.drawCircle(x, y, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = 1.2f * d; color = p.faint; pathEffect = DashPathEffect(floatArrayOf(2 * d, 2 * d), 0f)
            })
        }
    }

    private fun drawMountain(c: Canvas, days: List<Day>, goal: Int, left: Float, right: Float, top: Float, mh: Float, d: Float, p: Palette) {
        val n = maxOf(7, days.size)
        fun x(i: Int) = left + (right - left) * i / (n - 1).coerceAtLeast(1)
        fun y(v: Int) = top + (1f - v / 100f) * mh
        c.drawLine(left, y(0), right, y(0), Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.faint; strokeWidth = 1 * d })
        if (goal in 1..99) {
            c.drawLine(left, y(goal), right, y(goal), Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x80FFFFFF.toInt(); strokeWidth = 1.1f * d; pathEffect = DashPathEffect(floatArrayOf(5 * d, 4 * d), 0f)
            })
        }
        if (days.isEmpty()) return
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
        c.drawPath(area, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, top, 0f, top + mh, (p.accent and 0x00FFFFFF) or 0x8C000000.toInt(), (p.accent and 0x00FFFFFF) or 0x08000000, Shader.TileMode.CLAMP)
        })
        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = p.accent; strokeWidth = 2.2f * d })
        val dot = Paint(Paint.ANTI_ALIAS_FLAG)
        days.forEachIndexed { i, day ->
            dot.color = if (day.won) p.accent else p.info
            c.drawCircle(x(i), y(day.pct), (if (day.won) 2.8f else 2.3f) * d, dot)
        }
    }

    private fun drawWeek(c: Canvas, days: List<Day>, today: LocalDate, left: Float, right: Float, top: Float, r: Float, d: Float, p: Palette) {
        val byDate = days.associateBy { it.date }
        val slot = (right - left) / 7f
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.muted; textSize = 8.5f * d; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
        for (i in 0 until 7) {
            val date = today.minusDays((6 - i).toLong())
            val cx = left + slot * i + slot / 2
            c.drawText(INITIALS[date.dayOfWeek.value - 1], cx, top + 7 * d, label)
            val cy = top + 10 * d + r
            val day = byDate[date]
            when {
                day?.won == true -> drawMark(c, cx, cy, r, d, 2, p)
                day != null -> {
                    val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f * d; color = if (date == today) p.accent else p.faint }
                    c.drawCircle(cx, cy, r, ring)
                }
                else -> c.drawCircle(cx, cy, r * 0.3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.faint })
            }
        }
    }
}
