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
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Dibuja la parte gráfica de los widgets en un Bitmap del tamaño justo:
 * Val Q: el anillo de hoy, el núcleo y tu semana; Bitácora: tu semana (sin nombres).
 * La semana es una montaña por día: cada pico es el % de ese día, encima de su letra y su check.
 */
object Chart {
    private val INITIALS = arrayOf("L", "M", "M", "J", "V", "S", "D")
    private const val MAX_PIXELS = 1_500_000f
    private const val MAX_HABITS = 6
    private val BOLD: Typeface = Typeface.DEFAULT_BOLD
    private val MEDIUM: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    /** Una columna de la semana. value: 0..1, o null si ese día todavía no llega o no es del ciclo. */
    data class Col(val date: LocalDate, val value: Float?, val won: Boolean, val today: Boolean, val future: Boolean, val outside: Boolean)

    /** La semana del ciclo en la que estás (o, sin datos, la semana de lunes a domingo de hoy). */
    fun columns(frame: Frame?, today: LocalDate): List<Col> {
        val ref = frame?.today ?: today
        val first: LocalDate = when {
            frame?.week != null -> frame.week.start
            frame == null -> ref.minusDays((ref.dayOfWeek.value - 1).toLong())
            else -> ref.minusDays(6)
        }
        val byDate = frame?.days?.associateBy { it.date } ?: emptyMap()
        val start = frame?.start
        val end = frame?.cycleEnd
        return (0 until 7).map { i ->
            val date = first.plusDays(i.toLong())
            val outside = frame != null && ((start != null && date < start) || (end != null && date > end))
            val future = date > ref
            val day = byDate[date]
            val value = if (frame == null || outside || future) null else (day?.pct ?: 0) / 100f
            Col(date, value, day?.won == true && !future, frame != null && date == ref, future, outside)
        }
    }

    private fun bitmap(context: Context, wDp: Float, hDp: Float): Pair<Bitmap, Float> {
        val d = context.resources.displayMetrics.density
        // Un tope de memoria por si el lanzador informa un tamaño enorme.
        val area = max(1f, wDp * hDp)
        val scale = if (d * d * area > MAX_PIXELS) kotlin.math.sqrt(MAX_PIXELS / area) else d
        val w = max(1, (wDp * scale).toInt())
        val h = max(1, (hDp * scale).toInt())
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888) to scale
    }

    // ---------------------------------------------------------------- Val Q

    fun drawValQ(context: Context, shown: Shown, wDp: Float, hDp: Float, p: Palette, today: LocalDate): Bitmap {
        val (bmp, d) = bitmap(context, wDp, hDp)
        val c = Canvas(bmp)
        val w = bmp.width.toFloat()
        val h = bmp.height.toFloat()
        val f = shown.frame

        // ---- Anillo de hoy
        val ringD = min(min(h - 2 * d, 80 * d), w * 0.28f)
        val stroke = ringD * 0.1f
        val cx = ringD / 2f + d
        val cy = h / 2f
        val r = ringD / 2f - stroke / 2f
        when (shown.mode) {
            Mode.ACTIVE -> {
                val frame = f!!
                drawRing(c, cx, cy, r, stroke, frame.todayPct, if (frame.todayWon) null else frame.goal, p, d)
                drawPercent(c, cx, cy, r, frame.todayPct, p)
                drawRingLabel(c, cx, cy, r, if (frame.todayWon) "¡ganado!" else "hoy", if (frame.todayWon) p.accent else p.muted)
            }
            Mode.BEFORE -> {
                val left = f?.start?.let { java.time.temporal.ChronoUnit.DAYS.between(f.today, it).toInt() } ?: 1
                drawRing(c, cx, cy, r, stroke, 0, null, p, d)
                drawCenter(c, cx, cy, r, "${max(1, left)}", p.text)
                drawRingLabel(c, cx, cy, r, if (left <= 1) "día" else "días", p.muted)
            }
            Mode.FINISHED -> {
                drawRing(c, cx, cy, r, stroke, 100, null, p, d)
                drawCheck(c, cx, cy - r * 0.08f, r * 0.62f, p.accent, stroke * 0.5f)
                drawRingLabel(c, cx, cy, r, "ciclo", p.muted)
            }
            else -> {
                drawRing(c, cx, cy, r, stroke, 0, null, p, d)
                drawCenter(c, cx, cy, r, "—", p.muted)
                drawRingLabel(c, cx, cy, r, "hoy", p.muted)
            }
        }

        // ---- Derecha: núcleo de hoy y la semana
        val left = ringD + 16 * d
        val right = w - d
        var top = 2 * d
        if (f != null && f.core.isNotEmpty() && (shown.mode == Mode.ACTIVE || shown.mode == Mode.BEFORE)) {
            drawCore(c, f.core, left, right, 8 * d, shown.mode == Mode.ACTIVE, p, d)
            top = 22 * d
        }
        val goal = if (f != null && shown.mode == Mode.ACTIVE && f.goal in 1..99) f.goal / 100f else null
        drawWeek(c, columns(if (shown.mode == Mode.STALE) null else f, today), left, right, top, h - d, goal, p, d)
        return bmp
    }

    // ---------------------------------------------------------------- BilleFlash

    /**
     * La semana de tus noches a la izquierda (la montaña: el % con que cerraste cada día) y, a la derecha,
     * lo gastado hoy contra tu límite con su barra. En modo discreto, solo el porcentaje (sin montos).
     */
    fun drawBilleFlash(context: Context, shown: Shown, wDp: Float, hDp: Float, p: Palette, today: LocalDate, discreet: Boolean): Bitmap {
        val (bmp, d) = bitmap(context, wDp, hDp)
        val c = Canvas(bmp)
        val w = bmp.width.toFloat()
        val h = bmp.height.toFloat()
        val f = shown.frame
        val active = shown.mode == Mode.ACTIVE && f != null
        val cur = shown.payload?.cur ?: "BOB"
        drawWeek(c, columns(if (shown.mode == Mode.STALE) null else f, today), d, w * 0.56f, 2 * d, h - d, null, p, d)

        val left = w * 0.62f
        val right = w - d
        val over = active && f!!.usePct > 100
        val anto = if (active && !discreet) f!!.antojos else null
        val bigText = when {
            !active -> "—"
            discreet || f!!.spent == null -> "${f!!.usePct} %"
            else -> Texts.money(f.spent, cur)
        }
        val subText = when {
            !active -> ""
            discreet || f!!.limit == null -> "de tu límite"
            else -> "de ${Texts.money(f.limit, cur)}"
        }
        // Tamaños base; si no entran en el alto del widget, todo se achica por igual (k).
        val bigBase = min(26f * d, h * 0.3f)
        fun blockFor(k: Float) = k * (11.5f * d + 6 * d + bigBase + 4 * d + 11.5f * d + 10 * d + 7 * d + if (anto != null) 7 * d + 11.5f * d else 0f)
        val k = min(1f, (h - 2 * d) / blockFor(1f))
        val label = paint(p.muted).apply { textSize = 11.5f * d * k; typeface = MEDIUM }
        val big = paint(if (over) p.fail else p.text).apply { textSize = bigBase * k; typeface = BOLD }
        val sub = paint(p.muted).apply { textSize = 11.5f * d * k; typeface = MEDIUM }
        val antoP = paint(p.accent).apply { textSize = 11.5f * d * k; typeface = BOLD }
        val barH = 7 * d * k
        var y = (h - blockFor(k)) / 2f + label.textSize
        c.drawText(fit("Gastado hoy", label, right - left), left, y, label)
        y += 6 * d * k + big.textSize
        c.drawText(fit(bigText, big, right - left), left, y, big)
        y += 4 * d * k + sub.textSize
        c.drawText(fit(subText, sub, right - left), left, y, sub)
        y += 10 * d * k
        val r = barH / 2f
        c.drawRoundRect(left, y, right, y + barH, r, r, paint(p.track))
        if (active && f!!.usePct > 0) {
            val frac = min(1f, f.usePct / 100f)
            c.drawRoundRect(left, y, left + max(barH, (right - left) * frac), y + barH, r, r, paint(if (over) p.fail else p.accent))
        }
        if (anto != null) c.drawText(fit("Antojos: ${Texts.money(anto, cur)}", antoP, right - left), left, y + barH + 7 * d * k + antoP.textSize, antoP)
        return bmp
    }

    // ---------------------------------------------------------------- Focus Flow

    /** Tu arbolito de hoy (dibujado como en la app) a la izquierda; minutos, etapa y tu semana a la derecha. */
    fun drawFocus(context: Context, shown: Shown, wDp: Float, hDp: Float, p: Palette, today: LocalDate): Bitmap {
        val (bmp, d) = bitmap(context, wDp, hDp)
        val c = Canvas(bmp)
        val w = bmp.width.toFloat()
        val h = bmp.height.toFloat()
        val f = if (shown.mode == Mode.STALE) null else shown.frame
        val minutes = f?.focusMin ?: 0
        val treeW = min(w * 0.36f, h * 1.05f)
        drawTree(c, treeW / 2f + d, h - 2 * d, treeW, h - 3 * d, minutes)
        val left = treeW + 10 * d
        val right = w - d
        val big = paint(p.text).apply { textSize = min(26f * d, h * 0.3f); typeface = BOLD }
        val small = paint(p.muted).apply { textSize = 11f * d; typeface = MEDIUM }
        val acc = paint(p.accent).apply { textSize = 12f * d; typeface = BOLD }
        var y = big.textSize + 2 * d
        c.drawText(Texts.minutes(minutes), left, y, big)
        y += 4 * d + acc.textSize
        c.drawText(f?.treeLabel ?: "Semilla", left, y, acc)
        f?.treeNext?.let {
            y += 3 * d + small.textSize
            c.drawText("Faltan $it", left, y, small)
        }
        // La semana: una barra por día (llena = meta cumplida).
        val cols = f?.days.orEmpty().takeLast(7)
        if (cols.isNotEmpty()) {
            val barTop = y + 8 * d
            val barBottom = h - 2 * d - small.textSize
            if (barBottom - barTop > 10 * d) {
                val gap = 4 * d
                val bw = ((right - left) - gap * 6) / 7f
                val lbl = paint(p.muted).apply { textSize = 8.5f * d; typeface = MEDIUM; textAlign = Paint.Align.CENTER }
                cols.forEachIndexed { i, day ->
                    val x = left + i * (bw + gap)
                    val track = paint(p.track)
                    c.drawRoundRect(RectF(x, barTop, x + bw, barBottom), 3 * d, 3 * d, track)
                    val hh = (barBottom - barTop) * (day.pct / 100f)
                    if (hh > 0) {
                        val fill = paint(if (day.won) p.accent else (p.accent and 0x00FFFFFF) or (0x88 shl 24))
                        c.drawRoundRect(RectF(x, barBottom - hh, x + bw, barBottom), 3 * d, 3 * d, fill)
                    }
                    c.drawText(INITIALS[day.date.dayOfWeek.value - 1], x + bw / 2f, h - 2 * d, lbl.apply { color = if (day.date == today) p.text else p.muted })
                }
            }
        }
        return bmp
    }

    /** El arbolito, por etapas como en Focus Flow: semilla, tronco (25), ramas (50), hojas (60), flores (2 h), frutos (3 h). */
    private fun drawTree(c: Canvas, cx: Float, ground: Float, width: Float, height: Float, minutes: Int) {
        val u = min(width / 120f, height / 140f)
        fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)
        val m = minutes.toFloat()
        val groundP = paint(0xFF2F3A2E.toInt())
        c.drawOval(RectF(cx - 40 * u, ground - 4 * u, cx + 40 * u, ground + 10 * u), groundP)
        if (m <= 0f) {
            c.drawOval(RectF(cx - 6 * u, ground - 6 * u, cx + 6 * u, ground + 3 * u), paint(0xFFA0764F.toInt()))
            return
        }
        val trunkH = (if (m < 60) lerp(10f, 46f, m / 60f) else lerp(46f, 62f, (m - 60) / 120f)) * u
        val trunkW = (if (m < 60) lerp(3f, 7f, m / 60f) else lerp(7f, 10f, (m - 60) / 120f)) * u
        val top = ground - trunkH
        val wood = paint(0xFF8B5E3C.toInt())
        c.drawRoundRect(RectF(cx - trunkW / 2, top, cx + trunkW / 2, ground + 2 * u), trunkW / 2, trunkW / 2, wood)
        val leaf = paint(0xFF34D27B.toInt())
        val leaf2 = paint(0xFF24B064.toInt())
        if (m < 60) {
            val s = lerp(6f, 14f, m / 50f) * u
            c.drawOval(RectF(cx - 2 * s, top - s * 0.6f, cx, top + s * 0.3f), leaf)
            c.drawOval(RectF(cx, top - s * 0.8f, cx + 2 * s, top + s * 0.2f), leaf)
        }
        if (m >= 50) {
            val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8B5E3C.toInt(); strokeWidth = lerp(2.5f, 4f, (m - 50) / 130f) * u; strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE }
            val dx = lerp(12f, 20f, (m - 50) / 70f) * u
            c.drawLine(cx, top + trunkH * 0.45f, cx - dx, top + trunkH * 0.45f - lerp(10f, 16f, (m - 50) / 70f) * u, bp)
            c.drawLine(cx, top + trunkH * 0.3f, cx + dx, top + trunkH * 0.3f - lerp(12f, 18f, (m - 50) / 70f) * u, bp)
        }
        if (m >= 60) {
            val r = lerp(20f, 34f, (m - 60) / 120f) * u
            val cy = top - r * 0.55f
            c.drawCircle(cx - r * 0.75f, cy + r * 0.35f, r * 0.68f, leaf2)
            c.drawCircle(cx + r * 0.75f, cy + r * 0.35f, r * 0.68f, leaf2)
            c.drawCircle(cx, cy, r, leaf)
            c.drawCircle(cx - r * 0.25f, cy - r * 0.35f, r * 0.45f, paint(0x8C6EE7A1.toInt()))
            val spots = arrayOf(floatArrayOf(-0.55f, -0.25f), floatArrayOf(0.5f, -0.4f), floatArrayOf(-0.15f, -0.75f), floatArrayOf(0.25f, 0.15f), floatArrayOf(-0.6f, 0.25f), floatArrayOf(0.65f, 0.25f), floatArrayOf(0.05f, -0.35f), floatArrayOf(-0.3f, 0.45f), floatArrayOf(0.35f, -0.85f))
            if (m >= 120) {
                val n = lerp(3f, 9f, (m - 120) / 60f).toInt()
                val fl = paint(0xFFFF9CCF.toInt())
                for (i in 0 until n) c.drawCircle(cx + spots[i][0] * r, cy + spots[i][1] * r, r * 0.1f + u, fl)
            }
            if (m >= 180) {
                val fr = paint(0xFFFF6B4A.toInt())
                for (i in 0 until 6) c.drawCircle(cx + spots[i][1] * r * 0.9f, cy + spots[i][0] * r * 0.6f + r * 0.15f, r * 0.13f + 1.2f * u, fr)
            }
        }
    }

    // ---------------------------------------------------------------- Mi Diario

    /** Tus días escritos (la semana) a la izquierda y la pregunta del día, grande, a la derecha. Sin caritas. */
    fun drawDiario(context: Context, shown: Shown, wDp: Float, hDp: Float, p: Palette, today: LocalDate): Bitmap {
        val (bmp, d) = bitmap(context, wDp, hDp)
        val c = Canvas(bmp)
        val w = bmp.width.toFloat()
        val h = bmp.height.toFloat()
        val f = shown.frame
        drawWeek(c, columns(if (shown.mode == Mode.STALE) null else f, today), d, w * 0.5f, 2 * d, h - d, null, p, d)
        val q = f?.prompt ?: return bmp
        val left = w * 0.56f
        val right = w - d
        val label = paint(p.muted).apply { textSize = 11f * d; typeface = MEDIUM }
        // La pregunta lo más grande posible que entre en 4 renglones.
        var size = 17f * d
        var lines: List<String>
        val qp = paint(p.text).apply { typeface = BOLD }
        while (true) {
            qp.textSize = size
            lines = wrap(q, qp, right - left)
            val block = label.textSize + 6 * d + lines.size * size * 1.25f
            if ((lines.size <= 4 && block <= h - 4 * d) || size <= 10f * d) break
            size -= 1f * d
        }
        val block = label.textSize + 6 * d + lines.size * size * 1.25f
        var y = (h - block) / 2f + label.textSize
        c.drawText("Pregunta del día", left, y, label)
        y += 6 * d + size
        for (ln in lines.take(4)) {
            c.drawText(ln, left, y, qp)
            y += size * 1.25f
        }
        return bmp
    }

    private fun wrap(text: String, paint: Paint, max: Float): List<String> {
        val out = ArrayList<String>()
        var cur = ""
        for (wd in text.split(" ")) {
            val t = if (cur.isEmpty()) wd else "$cur $wd"
            if (paint.measureText(t) <= max || cur.isEmpty()) cur = t else {
                out.add(cur)
                cur = wd
            }
        }
        if (cur.isNotEmpty()) out.add(cur)
        return out
    }

    // ---------------------------------------------------------------- Bitácora

    fun drawBitacora(context: Context, shown: Shown, wDp: Float, hDp: Float, p: Palette, today: LocalDate): Bitmap {
        val (bmp, d) = bitmap(context, wDp, hDp)
        val c = Canvas(bmp)
        val w = bmp.width.toFloat()
        val h = bmp.height.toFloat()
        val habits = shown.frame?.habits.orEmpty().take(MAX_HABITS)
        val withHabits = habits.isNotEmpty() && (shown.mode == Mode.ACTIVE || shown.mode == Mode.FINISHED)
        // Con hábitos: la semana a la izquierda y cada hábito a la derecha (por color y orden, sin nombres).
        val weekRight = if (withHabits) w * 0.56f else w - d
        drawWeek(c, columns(if (shown.mode == Mode.STALE) null else shown.frame, today), d, weekRight, 2 * d, h - d, null, p, d)
        if (withHabits) drawHabits(c, habits, w * 0.61f, w - d, 2 * d, h - d, p, d)
        return bmp
    }

    /**
     * Cada hábito en una fila: el punto de su color (lleno si hoy está en «ok», rojo si hoy cayó, vacío si falta),
     * una barra con su % de días limpios y sus días seguidos a la derecha.
     */
    private fun drawHabits(c: Canvas, habits: List<HabitLine>, left: Float, right: Float, top: Float, bottom: Float, p: Palette, d: Float) {
        val n = habits.size
        val rowH = min((bottom - top) / n, 17f * d)
        val y0 = top + ((bottom - top) - rowH * n) / 2f
        val r = min(4.2f * d, rowH * 0.3f)
        val num = paint(p.text).apply { textSize = min(11f * d, rowH * 0.72f); typeface = BOLD; textAlign = Paint.Align.RIGHT }
        val unit = paint(p.muted).apply { textSize = min(8.5f * d, rowH * 0.55f); typeface = MEDIUM; textAlign = Paint.Align.RIGHT }
        val numW = num.measureText("88") + unit.measureText("d") + 2 * d
        val barLeft = left + 2 * r + 6 * d
        val barRight = right - numW - 6 * d
        val barH = min(5f * d, rowH * 0.32f)
        habits.forEachIndexed { i, hb ->
            val cy = y0 + rowH * i + rowH / 2f
            val color = p.habits[i % p.habits.size]
            val cx = left + r
            when (hb.today) {
                1 -> c.drawCircle(cx, cy, r, paint(color))
                2 -> c.drawCircle(cx, cy, r, paint(p.fail))
                else -> c.drawCircle(cx, cy, r - 0.7f * d, paint(color).apply { style = Paint.Style.STROKE; strokeWidth = 1.4f * d })
            }
            if (barRight > barLeft + 8 * d) {
                val track = RectF(barLeft, cy - barH / 2f, barRight, cy + barH / 2f)
                c.drawRoundRect(track, barH / 2f, barH / 2f, paint(p.track))
                if (hb.pct > 0) {
                    val fill = RectF(barLeft, track.top, barLeft + (barRight - barLeft) * hb.pct / 100f, track.bottom)
                    c.drawRoundRect(fill, barH / 2f, barH / 2f, paint(color))
                }
            }
            val unitX = right
            c.drawText("d", unitX, cy + num.textSize * 0.36f, unit)
            c.drawText("${hb.streak}", unitX - unit.measureText("d") - 1.5f * d, cy + num.textSize * 0.36f, num)
        }
    }

    // ---------------------------------------------------------------- Piezas

    private fun paint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    private fun alpha(color: Int, a: Int) = (color and 0x00FFFFFF) or ((a and 0xFF) shl 24)

    private fun drawRing(c: Canvas, cx: Float, cy: Float, r: Float, stroke: Float, pct: Int, goal: Int?, p: Palette, d: Float) {
        c.drawCircle(cx, cy, r, paint(p.track).apply { style = Paint.Style.STROKE; strokeWidth = stroke })
        if (pct > 0) {
            val arc = paint(p.accent).apply { style = Paint.Style.STROKE; strokeWidth = stroke; strokeCap = Paint.Cap.ROUND }
            c.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), -90f, 360f * min(pct, 100) / 100f, false, arc)
        }
        // La meta: un punto chico sobre el anillo.
        if (goal != null && goal in 1..99 && pct < goal) {
            val a = Math.toRadians(-90.0 + 360.0 * goal / 100.0)
            c.drawCircle((cx + r * cos(a)).toFloat(), (cy + r * sin(a)).toFloat(), stroke * 0.2f, paint(p.muted))
        }
    }

    private fun drawPercent(c: Canvas, cx: Float, cy: Float, r: Float, pct: Int, p: Palette) {
        val num = paint(p.text).apply { textSize = r * 0.56f; typeface = BOLD }
        val unit = paint(p.text).apply { textSize = r * 0.3f; typeface = BOLD }
        val n = "$pct"
        val total = num.measureText(n) + unit.measureText("%") + r * 0.03f
        val x = cx - total / 2f
        val base = cy + r * 0.12f
        c.drawText(n, x, base, num)
        c.drawText("%", x + num.measureText(n) + r * 0.03f, base, unit)
    }

    private fun drawCenter(c: Canvas, cx: Float, cy: Float, r: Float, text: String, color: Int) {
        val t = paint(color).apply { textSize = r * 0.56f; typeface = BOLD; textAlign = Paint.Align.CENTER }
        c.drawText(text, cx, cy + r * 0.12f, t)
    }

    private fun drawRingLabel(c: Canvas, cx: Float, cy: Float, r: Float, text: String, color: Int) {
        val t = paint(color).apply { textSize = r * 0.25f; typeface = MEDIUM; textAlign = Paint.Align.CENTER }
        c.drawText(text, cx, cy + r * 0.47f, t)
    }

    private fun drawCheck(c: Canvas, x: Float, y: Float, r: Float, color: Int, width: Float) {
        val check = paint(color).apply { style = Paint.Style.STROKE; strokeWidth = width; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
        val path = Path()
        path.moveTo(x - r * 0.45f, y + r * 0.02f)
        path.lineTo(x - r * 0.1f, y + r * 0.36f)
        path.lineTo(x + r * 0.48f, y - r * 0.32f)
        c.drawPath(path, check)
    }

    /** Marca redonda: 2 = hecho (con check), 1 = pendiente, 0 = no cuenta hoy. */
    private fun drawMark(c: Canvas, x: Float, y: Float, r: Float, d: Float, state: Int, p: Palette) {
        when (state) {
            2 -> {
                c.drawCircle(x, y, r, paint(p.accent))
                drawCheck(c, x, y, r, p.onAccent, 1.7f * d)
            }
            1 -> c.drawCircle(x, y, r - 0.7f * d, paint(p.muted).apply { style = Paint.Style.STROKE; strokeWidth = 1.4f * d })
            else -> c.drawCircle(x, y, r - 0.6f * d, paint(p.faint).apply {
                style = Paint.Style.STROKE; strokeWidth = 1.2f * d; pathEffect = DashPathEffect(floatArrayOf(2 * d, 2 * d), 0f)
            })
        }
    }

    private fun fit(text: String, paint: Paint, max: Float): String {
        if (paint.measureText(text) <= max) return text
        var t = text
        while (t.isNotEmpty() && paint.measureText("$t…") > max) t = t.dropLast(1)
        return if (t.isEmpty()) "" else "$t…"
    }

    /**
     * El núcleo de hoy en una fila: cada hábito con su marca y su nombre completo, repartidos a lo ancho.
     * Si no entran (por ejemplo, con 5 hábitos), la letra se achica un poco; solo si aun así no entran, se cortan.
     */
    private fun drawCore(c: Canvas, core: List<Core>, left: Float, right: Float, cy: Float, active: Boolean, p: Palette, d: Float) {
        val n = core.size
        if (n == 0) return
        val avail = right - left
        val label = paint(p.muted).apply { typeface = MEDIUM }
        // Tamaños de letra, marca y separación, de más cómodo a más compacto.
        val sizes = listOf(Triple(10f, 6f, 3.5f), Triple(9.5f, 5.6f, 3f), Triple(9f, 5.2f, 2.8f), Triple(8.5f, 5f, 2.5f))
        var chosen = sizes.last()
        var widths = emptyList<Float>()
        var fits = false
        for (sz in sizes) {
            label.textSize = sz.first * d
            val r = sz.second * d
            val gap = sz.third * d
            widths = core.map { 2 * r + gap + label.measureText(it.name) }
            chosen = sz
            if (widths.sum() + (n - 1) * 4f * d <= avail) {
                fits = true
                break
            }
        }
        label.textSize = chosen.first * d
        val r = chosen.second * d
        val gap = chosen.third * d
        val space = if (fits && n > 1) min((avail - widths.sum()) / (n - 1), 22 * d) else 0f
        val slot = avail / n
        var x0 = left
        core.forEachIndexed { i, item ->
            val start = if (fits) x0 else left + slot * i
            val done = active && item.done
            drawMark(c, start + r, cy, r, d, if (done) 2 else if (item.counts) 1 else 0, p)
            label.color = if (!item.counts) p.faint else if (done) p.text else p.muted
            val text = if (fits) item.name else fit(item.name, label, slot - 2 * r - gap - 3 * d)
            c.drawText(text, start + 2 * r + gap, cy + label.textSize * 0.35f, label)
            x0 += widths[i] + space
        }
    }

    /**
     * La semana: arriba la montaña (un pico por día: azul si no se ganó, del color de la app si se ganó),
     * con la línea de la meta; abajo la letra de cada día y su marca.
     */
    private fun drawWeek(c: Canvas, cols: List<Col>, left: Float, right: Float, top: Float, bottom: Float, goal: Float?, p: Palette, d: Float) {
        val n = cols.size
        val slot = (right - left) / n
        fun cx(i: Int) = left + slot * (i + 0.5f)
        val markR = min(7f * d, slot * 0.2f)
        val markY = bottom - markR - 0.5f * d
        val letterSize = 9.5f * d
        val letterBase = markY - markR - 4f * d
        val base = letterBase - letterSize - 4f * d
        val maxH = base - top - 4f * d

        // Base y meta
        c.drawLine(left, base, right, base, paint(p.faint).apply { strokeWidth = d })
        if (maxH > 6 * d) {
            if (goal != null) {
                val y = base - goal * maxH
                c.drawLine(left, y, right, y, paint(alpha(p.text, 0x38)).apply {
                    strokeWidth = d; pathEffect = DashPathEffect(floatArrayOf(3 * d, 3 * d), 0f)
                })
            }
            // Montaña: primero todos los días (azul), encima los ganados (color de la app).
            val step = max(1f, d * 0.75f)
            val xs = ArrayList<Float>()
            var x = left
            while (x < right) {
                xs.add(x); x += step
            }
            xs.add(right)
            val hw = slot * 0.95f
            fun heights(only: (Col) -> Boolean): FloatArray = FloatArray(xs.size) { k ->
                var best = 0f
                cols.forEachIndexed { i, col ->
                    val v = col.value ?: return@forEachIndexed
                    if (!only(col) || v <= 0f) return@forEachIndexed
                    val t = (xs[k] - cx(i)) / hw
                    if (abs(t) < 1f) best = max(best, v * maxH * ((1f + cos(PI.toFloat() * t)) / 2f))
                }
                best
            }
            layer(c, xs, heights { true }, base, top, p.info, 0x60, 0x06, 0xB0, d)
            layer(c, xs, heights { it.won }, base, top, p.accent, 0x9A, 0x0A, 0xFF, d)
            // Puntos en la cima
            cols.forEachIndexed { i, col ->
                val v = col.value ?: return@forEachIndexed
                if (v <= 0f) return@forEachIndexed
                val y = base - v * maxH
                c.drawCircle(cx(i), y, (if (col.won) 3.4f else 3f) * d, paint(p.bg))
                c.drawCircle(cx(i), y, (if (col.won) 2.4f else 2f) * d, paint(if (col.won) p.accent else p.info))
            }
        }

        // Letras y marcas
        val label = paint(p.muted).apply { textSize = letterSize; typeface = BOLD; textAlign = Paint.Align.CENTER }
        cols.forEachIndexed { i, col ->
            val x = cx(i)
            label.color = when {
                col.today -> p.accent
                col.future || col.outside -> p.faint
                else -> p.muted
            }
            c.drawText(INITIALS[col.date.dayOfWeek.value - 1], x, letterBase, label)
            when {
                col.outside -> Unit
                col.won -> {
                    c.drawCircle(x, markY, markR, paint(p.accent))
                    drawCheck(c, x, markY, markR, p.onAccent, 1.7f * d)
                }
                col.today -> {
                    // Hoy: el anillo se va llenando con tu %.
                    val rr = markR - 0.8f * d
                    c.drawCircle(x, markY, rr, paint(p.track).apply { style = Paint.Style.STROKE; strokeWidth = 1.6f * d })
                    val v = col.value ?: 0f
                    if (v > 0f) c.drawArc(RectF(x - rr, markY - rr, x + rr, markY + rr), -90f, 360f * v, false, paint(p.accent).apply {
                        style = Paint.Style.STROKE; strokeWidth = 1.6f * d; strokeCap = Paint.Cap.ROUND
                    })
                }
                col.future -> c.drawCircle(x, markY, markR * 0.22f, paint(p.faint))
                (col.value ?: 0f) > 0f -> c.drawCircle(x, markY, markR - 0.7f * d, paint(p.muted).apply { style = Paint.Style.STROKE; strokeWidth = 1.4f * d })
                else -> c.drawCircle(x, markY, markR * 0.3f, paint(p.faint))
            }
        }
    }

    private fun layer(c: Canvas, xs: List<Float>, hs: FloatArray, base: Float, top: Float, color: Int, aTop: Int, aBottom: Int, aStroke: Int, d: Float) {
        if (hs.none { it > 0.5f * d }) return
        val fill = Path()
        fill.moveTo(xs.first(), base)
        for (k in xs.indices) fill.lineTo(xs[k], base - hs[k])
        fill.lineTo(xs.last(), base)
        fill.close()
        c.drawPath(fill, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, top, 0f, base, alpha(color, aTop), alpha(color, aBottom), Shader.TileMode.CLAMP)
        })
        val line = Path()
        var pen = false
        for (k in xs.indices) {
            val y = base - hs[k]
            if (hs[k] > 0.5f * d) {
                if (pen) line.lineTo(xs[k], y) else line.moveTo(xs[k], y)
                pen = true
            } else {
                pen = false
            }
        }
        c.drawPath(line, paint(alpha(color, aStroke)).apply { style = Paint.Style.STROKE; strokeWidth = 1.7f * d; strokeJoin = Paint.Join.ROUND })
    }
}
