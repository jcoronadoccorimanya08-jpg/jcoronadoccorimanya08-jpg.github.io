package io.github.jcoronadoccorimanya08.rachas

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.View
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** Colores de cada app (los mismos tokens del tema oscuro de sus estilos). */
data class Palette(
    val accent: Int,
    /** Color del check sobre el acento. */
    val onAccent: Int,
    val info: Int,
    val text: Int,
    val muted: Int,
    val faint: Int,
    val track: Int,
    /** Fondo del widget (para separar los puntos de la línea). */
    val bg: Int,
    /** Caída de hoy en un hábito. */
    val fail: Int = 0xFFEE6F5B.toInt(),
    /** Color de cada hábito de Bitácora, el mismo de la app (tokens --h1 … --h6 del tema oscuro). */
    val habits: IntArray = HABIT_COLORS,
)

private val HABIT_COLORS = intArrayOf(
    0xFF5AB6FF.toInt(), 0xFFFFB547.toInt(), 0xFFB69CFF.toInt(), 0xFFFF8AC4.toInt(), 0xFFB8E65A.toInt(), 0xFF4FD1C5.toInt(),
)

object Palettes {
    private val ACCENTS_VALQ = mapOf("verde" to 0xFF2EE6A8.toInt(), "violeta" to 0xFFB69CFF.toInt(), "lima" to 0xFFB8E65A.toInt(), "rosa" to 0xFFFF8AC4.toInt())
    private val ACCENTS_BITACORA = mapOf("verde" to 0xFF3BBFA7.toInt(), "violeta" to 0xFFB69CFF.toInt(), "lima" to 0xFFB8E65A.toInt(), "rosa" to 0xFFFF8AC4.toInt(), "vino" to 0xFFE0727F.toInt())

    fun of(appKey: String, accent: String): Palette {
        val valq = appKey == "valq"
        val map = if (valq) ACCENTS_VALQ else ACCENTS_BITACORA
        val acc = map[accent] ?: map.getValue("verde")
        return if (valq) Palette(acc, 0xFF0B0E13.toInt(), 0xFF5AB6FF.toInt(), 0xFFF2F5F9.toInt(), 0xFFA0ABBB.toInt(), 0x47FFFFFF, 0x1FFFFFFF, 0xFF121820.toInt())
        else Palette(acc, 0xFF08100D.toInt(), 0xFF6CB4E8.toInt(), 0xFFEEF3F1.toInt(), 0xFF94A49F.toInt(), 0x47FFFFFF, 0x1FFFFFFF, 0xFF111815.toInt())
    }
}

/** Textos en español de Bolivia. */
object Texts {
    private val WEEKDAYS = arrayOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")
    private val MONTHS = arrayOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

    fun days(n: Int) = if (n == 1) "1 día" else "$n días"

    /** 85 → «1 h 25 min»; 45 → «45 min». */
    fun minutes(m: Int): String = when {
        m < 60 -> "$m min"
        m % 60 == 0 -> "${m / 60} h"
        else -> "${m / 60} h ${m % 60} min"
    }

    /** «lun 9 nov». */
    fun date(d: LocalDate) = "${WEEKDAYS[d.dayOfWeek.value - 1]} ${d.dayOfMonth} ${MONTHS[d.monthValue - 1]}"

    /** 1234 → «1.234». */
    fun number(n: Int): String {
        val s = kotlin.math.abs(n).toString().reversed().chunked(3).joinToString(".").reversed()
        return if (n < 0) "-$s" else s
    }

    fun hours(h: Double): String =
        if (h == Math.floor(h)) h.toInt().toString() else String.format(java.util.Locale.ROOT, "%.1f", h).replace('.', ',')

    /** 28.0 → «Bs 28»; 46.67 → «Bs 46,7». */
    fun bs(x: Double): String {
        val r = Math.round(x * 10) / 10.0
        val whole = r == Math.floor(r)
        val n = if (whole) number(r.toInt()) else number(r.toInt()) + "," + Math.abs(Math.round(r * 10) % 10)
        return "Bs $n"
    }

    /** En la moneda principal de BilleFlash: «S/ 2,7» o «Bs 28». */
    fun money(x: Double, cur: String): String = if (cur == "PEN") "S/ " + bs(x).removePrefix("Bs ") else bs(x)

    fun daysUntil(f: Frame): Int = f.start?.let { ChronoUnit.DAYS.between(f.today, it).toInt() } ?: 1
}

object WidgetRenderer {
    /** Desde esta hora, si todavía no ganaste (o marcaste) el día, el widget te lo recuerda con un borde. */
    const val EVENING_HOUR = 21

    private val TIERS = arrayOf("BRONCE", "PLATA", "ORO", "PLATINO", "DIAMANTE", "LEYENDA")
    private val CHIPS = intArrayOf(R.drawable.chip_bronce, R.drawable.chip_plata, R.drawable.chip_oro, R.drawable.chip_platino, R.drawable.chip_diamante, R.drawable.chip_leyenda)
    private val CHIP_TEXT = intArrayOf(0xFFE7AA78.toInt(), 0xFFD9E0E9.toInt(), 0xFFF5C542.toInt(), 0xFFA6F0E9.toInt(), 0xFF96D6FF.toInt(), 0xFFFF9FE9.toInt())

    fun open(context: Context, appKey: String, urge: Boolean): PendingIntent {
        val target = when (appKey) {
            "valq" -> ValQActivity::class.java
            "billeflash" -> BilleFlashActivity::class.java
            "midiario" -> DiarioActivity::class.java
            "focusflow" -> FocusActivity::class.java
            else -> BitacoraActivity::class.java
        }
        val intent = Intent(context, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (urge) intent.putExtra(TwaLauncherActivity.EXTRA_URGE, true)
        val code = when {
            appKey == "valq" -> 1
            appKey == "billeflash" -> 4
            appKey == "midiario" -> 5
            appKey == "focusflow" -> 6
            urge -> 3
            else -> 2
        }
        return PendingIntent.getActivity(context, code, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun render(context: Context, appKey: String, widthDp: Int, heightDp: Int, now: ZonedDateTime = ZonedDateTime.now(LA_PAZ)): RemoteViews {
        if (appKey == "billeflash") return renderBilleFlash(context, widthDp, heightDp, now)
        if (appKey == "midiario") return renderDiario(context, widthDp, heightDp, now)
        if (appKey == "focusflow") return renderFocus(context, widthDp, heightDp, now)
        val valq = appKey == "valq"
        val views = RemoteViews(context.packageName, if (valq) R.layout.widget_valq else R.layout.widget_bitacora)
        views.setOnClickPendingIntent(android.R.id.background, open(context, appKey, urge = false))
        if (!valq) views.setOnClickPendingIntent(R.id.urge, open(context, appKey, urge = true))

        val today = now.toLocalDate()
        val payload = WidgetStore.load(context, appKey)?.takeIf { it.app == appKey }
        val shown = Model.pick(payload, today)
        val f = shown.frame
        val p = Palettes.of(appKey, payload?.accent ?: "verde")
        val active = shown.mode == Mode.ACTIVE && f != null
        val evening = active && !shown.test && now.hour >= EVENING_HOUR && (if (valq) !f!!.todayWon else f!!.todayDone == false)
        val hot = active && (f!!.streak >= 7 || f.todayWon)

        // Racha encendida (7 días o más, o día ganado): un brillo del color de la app detrás de la llama.
        views.setViewVisibility(R.id.glow, if (hot) View.VISIBLE else View.GONE)
        views.setInt(R.id.glow, "setColorFilter", p.accent)
        // Desde las 21:00, si falta: borde del color de la app.
        views.setViewVisibility(R.id.outline, if (evening) View.VISIBLE else View.GONE)
        views.setInt(R.id.outline, "setColorFilter", p.accent)

        val lit = (active && f!!.streak > 0) || shown.mode == Mode.BEFORE || shown.mode == Mode.FINISHED
        views.setImageViewResource(R.id.flame, if (lit) R.drawable.ic_flame else R.drawable.ic_flame_off)
        views.setTextViewText(R.id.streak, headline(shown, valq, p))
        views.setTextColor(R.id.streak, p.text)
        chip(views, shown)

        // Sin datos todavía: el último paso de la conexión, para saber dónde se quedó.
        val diag = if (shown.mode == Mode.EMPTY || shown.mode == Mode.STALE) Diag.text(context, appKey) else null
        val chartW = (widthDp - 28).coerceAtLeast(160).toFloat()
        val chartH = (heightDp - if (valq) 80 else 104).coerceAtLeast(44).toFloat()
        if (valq) {
            // Sin datos, la frase muestra el último paso de la conexión (en gris).
            views.setTextViewText(R.id.phrase, diag ?: valqPhrase(shown, evening))
            views.setTextColor(R.id.phrase, if (diag != null) p.muted else p.accent)
            views.setImageViewBitmap(R.id.chart, Chart.drawValQ(context, shown, chartW, chartH, p, today))
            valqFooter(views, shown, p)
        } else {
            views.setTextViewText(R.id.savings, savingsLine(shown))
            views.setTextColor(R.id.savings, p.accent)
            views.setTextViewText(R.id.day, diag ?: dayLine(shown))
            views.setTextColor(R.id.day, p.muted)
            views.setImageViewBitmap(R.id.chart, Chart.drawBitacora(context, shown, chartW, chartH, p, today))
            val (line, strong) = bitacoraLine(shown, evening)
            views.setTextViewText(R.id.line, line)
            views.setTextColor(R.id.line, if (strong) p.accent else p.muted)
        }
        views.setContentDescription(android.R.id.background, describe(shown, valq))
        return views
    }

    /** BilleFlash y Mi Diario cambian de día a las 00:00, como el calendario. */
    const val BF_DAY_START = 0

    fun billeFlashDay(now: ZonedDateTime): LocalDate = now.minusHours(BF_DAY_START.toLong()).toLocalDate()

    private fun renderBilleFlash(context: Context, widthDp: Int, heightDp: Int, now: ZonedDateTime): RemoteViews {
        val key = "billeflash"
        val views = RemoteViews(context.packageName, R.layout.widget_billeflash)
        views.setOnClickPendingIntent(android.R.id.background, open(context, key, urge = false))
        views.setOnClickPendingIntent(R.id.urge, open(context, key, urge = false))
        val today = billeFlashDay(now)
        val payload = WidgetStore.load(context, key)?.takeIf { it.app == key }
        val shown = Model.pick(payload, today)
        val f = shown.frame
        val p = Palettes.of(key, payload?.accent ?: "verde")
        val discreet = payload?.discreet == true
        val cur = payload?.cur ?: "BOB"
        val active = shown.mode == Mode.ACTIVE && f != null
        val closed = active && f!!.closedPct != null
        // De noche (desde las 21:00 y hasta las 4:00), si todavía no cerraste: borde del color de la app.
        val evening = active && !closed && (now.hour >= EVENING_HOUR || now.hour < BF_DAY_START)
        views.setViewVisibility(R.id.glow, if (active && (f!!.streak >= 7 || closed)) View.VISIBLE else View.GONE)
        views.setInt(R.id.glow, "setColorFilter", p.accent)
        views.setViewVisibility(R.id.outline, if (evening) View.VISIBLE else View.GONE)
        views.setInt(R.id.outline, "setColorFilter", p.accent)
        views.setImageViewResource(R.id.flame, if (active && f!!.streak > 0) R.drawable.ic_flame else R.drawable.ic_flame_off)
        views.setTextViewText(R.id.streak, if (active) bigNumber(f!!.streak, if (f.streak == 1) "noche" else "noches", p) else "BilleFlash")
        views.setTextColor(R.id.streak, p.text)
        views.setViewVisibility(R.id.chip, View.GONE)

        val diag = if (shown.mode == Mode.EMPTY || shown.mode == Mode.STALE) Diag.text(context, key) else null
        views.setTextViewText(R.id.savings, if (active && !discreet) f!!.available?.let { "Quedan ${Texts.money(it, cur)}" } ?: "" else "")
        views.setTextColor(R.id.savings, p.accent)
        views.setTextColor(R.id.urge, p.accent)
        val next = f?.nextIn
        views.setTextViewText(
            R.id.day,
            diag ?: if (!active || next == null) "" else when {
                next > 1 -> "Quincena en $next d"
                next == 1 -> "Quincena mañana"
                next == 0 -> "Quincena hoy"
                else -> "Quincena atrasada"
            },
        )
        views.setTextColor(R.id.day, p.muted)
        val chartW = (widthDp - 28).coerceAtLeast(160).toFloat()
        val chartH = (heightDp - 104).coerceAtLeast(44).toFloat()
        views.setImageViewBitmap(R.id.chart, Chart.drawBilleFlash(context, shown, chartW, chartH, p, today, discreet))
        val (line, strong) = when (shown.mode) {
            Mode.EMPTY -> "Toca y desbloquea BilleFlash" to false
            Mode.STALE -> "Toca para ponerte al día" to false
            else -> when {
                !active -> "" to false
                f!!.urgent != null -> f.urgent to true
                evening -> "Cierra tu día antes de dormir" to true
                f.saveGoal != null -> goalLine(f.saveGoal, cur) to false
                closed -> "Día cerrado · ${f.closedPct} %" to false
                else -> "Anota tus gastos de hoy" to false
            }
        }
        views.setTextViewText(R.id.line, line)
        views.setTextColor(R.id.line, if (strong) p.accent else p.muted)
        views.setContentDescription(
            android.R.id.background,
            when {
                !active -> "BilleFlash: abre la app desde Rachas para ver tu día."
                discreet -> "BilleFlash: racha de ${f!!.streak} noches. Hoy vas ${f.usePct} % de tu límite."
                else -> "BilleFlash: racha de ${f!!.streak} noches. Hoy gastaste ${f.spent?.let { Texts.money(it, cur) } ?: "—"} de ${f.limit?.let { Texts.money(it, cur) } ?: "—"}."
            },
        )
        return views
    }

    /** Mi Diario: racha, tu semana, tus caritas y la pregunta del día. Nunca lo que escribiste. */
    private fun renderDiario(context: Context, widthDp: Int, heightDp: Int, now: ZonedDateTime): RemoteViews {
        val key = "midiario"
        val views = RemoteViews(context.packageName, R.layout.widget_diario)
        views.setOnClickPendingIntent(android.R.id.background, open(context, key, urge = false))
        views.setOnClickPendingIntent(R.id.urge, open(context, key, urge = false))
        val today = billeFlashDay(now)
        val payload = WidgetStore.load(context, key)?.takeIf { it.app == key }
        val shown = Model.pick(payload, today)
        val f = shown.frame
        val p = Palettes.of(key, payload?.accent ?: "verde")
        val active = shown.mode == Mode.ACTIVE && f != null
        val closed = active && f!!.closedPct != null
        val evening = active && !closed && (now.hour >= EVENING_HOUR || now.hour < BF_DAY_START)
        views.setViewVisibility(R.id.glow, if (active && (f!!.streak >= 7 || closed)) View.VISIBLE else View.GONE)
        views.setInt(R.id.glow, "setColorFilter", p.accent)
        views.setViewVisibility(R.id.outline, if (evening) View.VISIBLE else View.GONE)
        views.setInt(R.id.outline, "setColorFilter", p.accent)
        views.setImageViewResource(R.id.flame, if (active && f!!.streak > 0) R.drawable.ic_flame else R.drawable.ic_flame_off)
        views.setTextViewText(R.id.streak, if (active) bigNumber(f!!.streak, if (f.streak == 1) "día" else "días", p) else "Mi Diario")
        views.setTextColor(R.id.streak, p.text)
        views.setViewVisibility(R.id.chip, View.GONE)
        val ago = f?.yearAgoMood
        views.setTextViewText(R.id.savings, if (active && ago != null) "📖 Hace un año escribiste" else "")
        views.setTextColor(R.id.savings, p.accent)
        val diag = if (shown.mode == Mode.EMPTY || shown.mode == Mode.STALE) Diag.text(context, key) else null
        views.setTextViewText(R.id.day, diag ?: if (active) "Día ${f!!.dayNumber}" else "")
        views.setTextColor(R.id.day, p.muted)
        views.setTextColor(R.id.urge, p.accent)
        val chartW = (widthDp - 28).coerceAtLeast(160).toFloat()
        val chartH = (heightDp - 104).coerceAtLeast(44).toFloat()
        views.setImageViewBitmap(R.id.chart, Chart.drawDiario(context, shown, chartW, chartH, p, today))
        val (line, strong) = when (shown.mode) {
            Mode.EMPTY -> "Toca y desbloquea tu diario" to false
            Mode.STALE -> "Toca para ponerte al día" to false
            else -> when {
                !active -> "" to false
                closed -> "Día escrito · ${f!!.closedPct} % ✓" to false
                evening -> "Escribe tu día antes de dormir" to true
                else -> "Hoy todavía no escribes" to false
            }
        }
        views.setTextViewText(R.id.line, line)
        views.setTextColor(R.id.line, if (strong) p.accent else p.muted)
        views.setContentDescription(android.R.id.background, if (active) "Mi Diario: racha de ${f!!.streak} días.${if (closed) " Hoy ya escribiste." else ""}" else "Mi Diario: abre la app desde Rachas.")
        return views
    }

    /** Focus Flow: tu arbolito de hoy, minutos y meta, racha, repasos y próximo examen. */
    private fun renderFocus(context: Context, widthDp: Int, heightDp: Int, now: ZonedDateTime): RemoteViews {
        val key = "focusflow"
        val views = RemoteViews(context.packageName, R.layout.widget_focus)
        views.setOnClickPendingIntent(android.R.id.background, open(context, key, urge = false))
        views.setOnClickPendingIntent(R.id.urge, open(context, key, urge = false))
        val today = billeFlashDay(now)
        val payload = WidgetStore.load(context, key)?.takeIf { it.app == key }
        val shown = Model.pick(payload, today)
        val f = shown.frame
        val p = Palettes.of(key, payload?.accent ?: "verde")
        val active = shown.mode == Mode.ACTIVE && f != null
        val met = active && (f!!.focusMin ?: 0) >= (f.focusGoal ?: Int.MAX_VALUE)
        views.setViewVisibility(R.id.glow, if (active && (f!!.streak >= 7 || met)) View.VISIBLE else View.GONE)
        views.setInt(R.id.glow, "setColorFilter", p.accent)
        views.setViewVisibility(R.id.outline, View.GONE)
        views.setImageViewResource(R.id.flame, if (active && f!!.streak > 0) R.drawable.ic_flame else R.drawable.ic_flame_off)
        views.setTextViewText(R.id.streak, if (active) bigNumber(f!!.streak, if (f.streak == 1) "día" else "días", p) else "Focus Flow")
        views.setTextColor(R.id.streak, p.text)
        views.setViewVisibility(R.id.chip, View.GONE)
        val exam = if (active) f!!.examIn else null
        views.setTextViewText(R.id.savings, when {
            exam == null -> ""
            exam == 0 -> "📅 ¡Hoy es tu examen!"
            else -> "📅 ${f!!.examName ?: "Examen"} en $exam ${if (exam == 1) "día" else "días"}"
        })
        views.setTextColor(R.id.savings, p.accent)
        val diag = if (shown.mode == Mode.EMPTY || shown.mode == Mode.STALE) Diag.text(context, key) else null
        views.setTextViewText(R.id.day, diag ?: if (active && f!!.reviewsDue > 0) "🔁 ${f.reviewsDue} ${if (f.reviewsDue == 1) "repaso" else "repasos"}" else "")
        views.setTextColor(R.id.day, p.muted)
        views.setTextColor(R.id.urge, p.accent)
        val chartW = (widthDp - 28).coerceAtLeast(160).toFloat()
        val chartH = (heightDp - 104).coerceAtLeast(44).toFloat()
        views.setImageViewBitmap(R.id.chart, Chart.drawFocus(context, shown, chartW, chartH, p, today))
        val (line, strong) = when (shown.mode) {
            Mode.EMPTY -> "Toca y abre Focus Flow" to false
            Mode.STALE -> "Toca para ponerte al día" to false
            else -> when {
                !active -> "" to false
                met -> "¡Meta de hoy cumplida! 🌳" to true
                (f!!.focusMin ?: 0) == 0 -> "Hoy todavía no estudias" to false
                else -> "Meta: ${Texts.minutes(f.focusMin ?: 0)} de ${Texts.minutes(f.focusGoal ?: 0)}" to false
            }
        }
        views.setTextViewText(R.id.line, line)
        views.setTextColor(R.id.line, if (strong) p.accent else p.muted)
        views.setContentDescription(android.R.id.background, if (active) "Focus Flow: hoy ${f!!.focusMin ?: 0} minutos, racha de ${f.streak} días." else "Focus Flow: abre la app desde Rachas.")
        return views
    }

    /** «⭐ Fondo: 30 % · Bs 150 de 500»; en modo discreto, «⭐ Tu meta: 30 %». */
    fun goalLine(g: GoalLine, cur: String = "BOB"): String {
        val name = g.name ?: "Tu meta"
        val amounts = if (g.saved != null && g.target != null) " · ${Texts.money(g.saved, cur)} de ${Texts.bs(g.target).removePrefix("Bs ")}" else ""
        return "⭐ $name: ${g.pct} %$amounts"
    }

    private fun headline(shown: Shown, valq: Boolean, p: Palette): CharSequence {
        val f = shown.frame
        return when (shown.mode) {
            Mode.ACTIVE, Mode.FINISHED -> bigNumber(f!!.streak, if (f.streak == 1) "día" else "días", p)
            Mode.BEFORE -> Texts.daysUntil(f!!).let { if (it <= 1) "Empieza mañana" else "Empieza en $it días" }
            Mode.EMPTY, Mode.STALE -> if (valq) "Val Q" else "Tu racha"
        }
    }

    /** «12 días»: el número grande y la unidad en gris, como los widgets de Samsung. */
    private fun bigNumber(n: Int, unit: String, p: Palette): CharSequence {
        val num = n.toString()
        val s = SpannableString("$num $unit")
        s.setSpan(RelativeSizeSpan(1.75f), 0, num.length, 0)
        s.setSpan(ForegroundColorSpan(p.muted), num.length, s.length, 0)
        return s
    }

    private fun chip(views: RemoteViews, shown: Shown) {
        val f = shown.frame
        val tier = if (f != null && shown.mode != Mode.STALE) f.badges.coerceIn(0, TIERS.size) else 0
        when {
            shown.test -> {
                views.setTextViewText(R.id.chip, "PRUEBA")
                views.setInt(R.id.chip, "setBackgroundResource", R.drawable.chip_prueba)
                views.setTextColor(R.id.chip, 0xFFFFC24D.toInt())
                views.setViewVisibility(R.id.chip, View.VISIBLE)
            }
            tier > 0 -> {
                views.setTextViewText(R.id.chip, TIERS[tier - 1])
                views.setInt(R.id.chip, "setBackgroundResource", CHIPS[tier - 1])
                views.setTextColor(R.id.chip, CHIP_TEXT[tier - 1])
                views.setViewVisibility(R.id.chip, View.VISIBLE)
            }
            else -> views.setViewVisibility(R.id.chip, View.GONE)
        }
    }

    // ---------------------------------------------------------------- Val Q

    private fun valqPhrase(shown: Shown, evening: Boolean): String {
        val f = shown.frame ?: return ""
        return when (shown.mode) {
            Mode.ACTIVE -> if (!evening) f.phrase else when (val n = f.need) {
                null -> "Anota tu día"
                1 -> "Te falta 1 para ganar"
                else -> "Te faltan $n para ganar"
            }
            Mode.BEFORE -> f.start?.let { "Arranca el ${Texts.date(it)}" } ?: f.phrase
            Mode.FINISHED -> "¡Ciclo completo!"
            else -> ""
        }
    }

    private fun valqFooter(views: RemoteViews, shown: Shown, p: Palette) {
        val f = shown.frame
        val prize = f?.prize
        val line = when {
            shown.mode == Mode.EMPTY -> "Toca aquí y desbloquea Val Q"
            shown.mode == Mode.STALE -> "Toca aquí para ponerte al día"
            prize != null -> "${prize.name} · ${left(prize.daysLeft)}"
            else -> "Un día ganado a la vez"
        }
        views.setTextViewText(R.id.line, line)
        views.setTextColor(R.id.line, p.muted)
        val withFlag = prize != null && shown.mode != Mode.EMPTY && shown.mode != Mode.STALE
        views.setTextViewCompoundDrawables(R.id.line, if (withFlag) R.drawable.ic_flag else 0, 0, 0, 0)
        val week = f?.week
        val weekWon = shown.mode == Mode.ACTIVE && week != null && week.goal > 0 && week.won >= week.goal
        val day = when (shown.mode) {
            Mode.ACTIVE -> if (weekWon) "Semana ganada ✓" else "Día ${f!!.dayNumber}/${f.cycleLength}"
            Mode.FINISHED -> "Día ${f!!.cycleLength}/${f.cycleLength}"
            Mode.BEFORE -> "Ciclo de ${f!!.cycleLength} días"
            else -> ""
        }
        views.setTextViewText(R.id.day, day)
        views.setTextColor(R.id.day, if (weekWon) p.accent else p.muted)
    }

    private fun left(n: Int) = when {
        n <= 0 -> "¡es hoy!"
        n == 1 -> "falta 1 día"
        else -> "faltan $n días"
    }

    // ---------------------------------------------------------------- Bitácora

    /** Sin dinero: solo las horas recuperadas (los Bs ya no se muestran). */
    private fun savingsLine(shown: Shown): String {
        val f = shown.frame ?: return ""
        if (shown.mode != Mode.ACTIVE && shown.mode != Mode.FINISHED) return ""
        return if (f.horas > 0) "${Texts.hours(f.horas)} h recuperadas" else ""
    }

    private fun dayLine(shown: Shown): String {
        val f = shown.frame ?: return ""
        return when (shown.mode) {
            Mode.ACTIVE -> "Día ${f.dayNumber}/${f.cycleLength}"
            Mode.FINISHED -> "Día ${f.cycleLength}/${f.cycleLength}"
            else -> ""
        }
    }

    /** La línea junto a «Tengo un antojo». El segundo valor dice si va en el color de la app. */
    private fun bitacoraLine(shown: Shown, evening: Boolean): Pair<String, Boolean> {
        val f = shown.frame
        return when (shown.mode) {
            Mode.EMPTY -> "Toca y desbloquea Bitácora" to false
            Mode.STALE -> "Toca para ponerte al día" to false
            Mode.BEFORE -> (f?.start?.let { "Arranca el ${Texts.date(it)}" } ?: "Tu ciclo está por empezar") to false
            Mode.FINISHED -> "¡Ciclo completo!" to true
            Mode.ACTIVE -> when {
                evening -> "Marca tu día antes de dormir" to true
                f!!.nextStep == null -> "Llegaste al último escalón" to false
                f.nextStep <= 0 -> "¡Escalón hoy!" to true
                else -> "Tu ${if (f.nextKind == "premio") "premio" else "cofre"} en ${Texts.days(f.nextStep)}" to false
            }
        }
    }

    private fun describe(shown: Shown, valq: Boolean): String {
        val app = if (valq) "Val Q" else "Bitácora"
        val f = shown.frame
        return when (shown.mode) {
            Mode.EMPTY -> "$app: abre la app desde Rachas para ver tu racha."
            Mode.STALE -> "$app: abre la app para ver tu día de hoy."
            Mode.BEFORE -> "$app: tu ciclo empieza en ${Texts.days(Texts.daysUntil(f!!))}."
            else -> {
                val base = "$app: racha de ${Texts.days(f!!.streak)}."
                if (valq) "$base Hoy ${f.todayPct} %${if (f.todayWon) ", día ganado" else ""}." else base
            }
        }
    }
}

/** Los dos widgets comparten todo; solo cambia la app. */
abstract class RachasWidgetProvider(private val appKey: String) : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { Widgets.update(context, manager, it, appKey) }
        Ticker.scheduleAll(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle) {
        Widgets.update(context, manager, id, appKey)
    }

    override fun onEnabled(context: Context) = Ticker.scheduleAll(context)

    override fun onDisabled(context: Context) = Ticker.scheduleAll(context)
}

class ValQWidget : RachasWidgetProvider("valq") {
    companion object {
        fun updateAll(context: Context) = Widgets.updateAll(context, "valq")
    }
}

class DiarioWidget : RachasWidgetProvider("midiario") {
    companion object {
        fun updateAll(context: Context) = Widgets.updateAll(context, "midiario")
    }
}

class FocusWidget : RachasWidgetProvider("focusflow") {
    companion object {
        fun updateAll(context: Context) = Widgets.updateAll(context, "focusflow")
    }
}

class BilleFlashWidget : RachasWidgetProvider("billeflash") {
    companion object {
        fun updateAll(context: Context) = Widgets.updateAll(context, "billeflash")
    }
}

class BitacoraWidget : RachasWidgetProvider("bitacora") {
    companion object {
        fun updateAll(context: Context) = Widgets.updateAll(context, "bitacora")
    }
}

object Widgets {
    private fun provider(appKey: String): Class<*> = when (appKey) {
        "valq" -> ValQWidget::class.java
        "billeflash" -> BilleFlashWidget::class.java
        "midiario" -> DiarioWidget::class.java
        "focusflow" -> FocusWidget::class.java
        else -> BitacoraWidget::class.java
    }

    private fun layout(appKey: String): Int = when (appKey) {
        "valq" -> R.layout.widget_valq
        "billeflash" -> R.layout.widget_billeflash
        "midiario" -> R.layout.widget_diario
        "focusflow" -> R.layout.widget_focus
        else -> R.layout.widget_bitacora
    }

    fun ids(context: Context, appKey: String): IntArray = try {
        AppWidgetManager.getInstance(context)?.getAppWidgetIds(ComponentName(context, provider(appKey))) ?: IntArray(0)
    } catch (_: Exception) {
        IntArray(0)
    }

    fun hasAny(context: Context) = listOf("valq", "bitacora", "billeflash", "midiario", "focusflow").any { ids(context, it).isNotEmpty() }

    /** Tamaño del widget en dp (en vertical: ancho mínimo y alto máximo). */
    private fun sizeDp(manager: AppWidgetManager, id: Int): Pair<Int, Int> {
        val o = manager.getAppWidgetOptions(id)
        val w = o?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)?.takeIf { it > 0 } ?: 320
        val h = o?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)?.takeIf { it > 0 } ?: 170
        return w to h
    }

    fun update(context: Context, manager: AppWidgetManager, id: Int, appKey: String) {
        val (w, h) = sizeDp(manager, id)
        val views = try {
            WidgetRenderer.render(context, appKey, w, h)
        } catch (_: Throwable) {
            // Si algo falla al dibujar, al menos el widget abre la app.
            RemoteViews(context.packageName, layout(appKey)).apply {
                setOnClickPendingIntent(android.R.id.background, WidgetRenderer.open(context, appKey, urge = false))
            }
        }
        manager.updateAppWidget(id, views)
    }

    fun updateAll(context: Context, appKey: String) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        ids(context, appKey).forEach { update(context, manager, it, appKey) }
        Ticker.scheduleAll(context)
    }
}
