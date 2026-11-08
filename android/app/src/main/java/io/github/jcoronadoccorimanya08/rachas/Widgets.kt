package io.github.jcoronadoccorimanya08.rachas

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId

/** Colores de cada app (los mismos tokens del tema oscuro de sus estilos). */
data class Palette(val accent: Int, val info: Int, val text: Int, val muted: Int, val faint: Int)

object Palettes {
    private val ACCENTS_VALQ = mapOf("verde" to 0xFF2EE6A8.toInt(), "violeta" to 0xFFB69CFF.toInt(), "lima" to 0xFFB8E65A.toInt(), "rosa" to 0xFFFF8AC4.toInt())
    private val ACCENTS_BITACORA = mapOf("verde" to 0xFF3BBFA7.toInt(), "violeta" to 0xFFB69CFF.toInt(), "lima" to 0xFFB8E65A.toInt(), "rosa" to 0xFFFF8AC4.toInt())

    fun of(appKey: String, accent: String): Palette {
        val valq = appKey == "valq"
        val acc = (if (valq) ACCENTS_VALQ else ACCENTS_BITACORA)[accent] ?: (if (valq) ACCENTS_VALQ else ACCENTS_BITACORA).getValue("verde")
        return if (valq) Palette(acc, 0xFF5AB6FF.toInt(), 0xFFE9EEF5.toInt(), 0xFFA3AFC0.toInt(), 0x33FFFFFF)
        else Palette(acc, 0xFF5AB6FF.toInt(), 0xFFE4EDEA.toInt(), 0xFF94A49F.toInt(), 0x33FFFFFF)
    }

    fun background(appKey: String, accent: String, hot: Boolean): Int {
        if (!hot) return if (appKey == "valq") R.drawable.widget_bg_valq else R.drawable.widget_bg_bitacora
        return when ("${appKey}_$accent") {
            "valq_violeta" -> R.drawable.widget_hot_valq_violeta
            "valq_lima" -> R.drawable.widget_hot_valq_lima
            "valq_rosa" -> R.drawable.widget_hot_valq_rosa
            "valq_verde" -> R.drawable.widget_hot_valq_verde
            "bitacora_violeta" -> R.drawable.widget_hot_bitacora_violeta
            "bitacora_lima" -> R.drawable.widget_hot_bitacora_lima
            "bitacora_rosa" -> R.drawable.widget_hot_bitacora_rosa
            else -> R.drawable.widget_hot_bitacora_verde
        }
    }
}

object WidgetRenderer {
    private val LA_PAZ: ZoneId = ZoneId.of("America/La_Paz")

    private fun open(context: Context, appKey: String, urge: Boolean): PendingIntent {
        val target = if (appKey == "valq") ValQActivity::class.java else BitacoraActivity::class.java
        val intent = Intent(context, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (urge) intent.putExtra(TwaLauncherActivity.EXTRA_URGE, true)
        val code = if (appKey == "valq") 1 else if (urge) 3 else 2
        return PendingIntent.getActivity(context, code, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun render(context: Context, appKey: String): RemoteViews {
        val valq = appKey == "valq"
        val views = RemoteViews(context.packageName, if (valq) R.layout.widget_valq else R.layout.widget_bitacora)
        views.setOnClickPendingIntent(R.id.widget_root, open(context, appKey, urge = false))
        if (!valq) views.setOnClickPendingIntent(R.id.urge, open(context, appKey, urge = true))

        val data = WidgetStore.load(context, appKey)
        val accent = data?.optString("accent", "verde") ?: "verde"
        val p = Palettes.of(appKey, accent)
        val streak = data?.optInt("streak") ?: 0
        val hot = streak >= 7
        val today = LocalDate.now(LA_PAZ)
        views.setInt(R.id.widget_root, "setBackgroundResource", Palettes.background(appKey, accent, hot))
        views.setTextColor(R.id.streak, p.text)
        views.setTextColor(R.id.day, p.muted)
        views.setTextColor(R.id.line, p.muted)

        if (data == null) {
            views.setTextViewText(R.id.streak, "— días")
            views.setTextViewText(R.id.day, "")
            views.setTextViewText(R.id.line, "Ábrela una vez desde este ícono")
            if (valq) views.setTextViewText(R.id.phrase, "") else views.setTextViewText(R.id.savings, "")
            views.setImageViewBitmap(R.id.chart, if (valq) Chart.drawValQ(context, null, today, p) else Chart.drawBitacora(context, null, today, p))
            return views
        }

        views.setTextViewText(R.id.streak, "$streak ${if (streak == 1) "día" else "días"}")
        val dayNumber = data.optInt("dayNumber")
        views.setTextViewText(R.id.day, if (dayNumber > 0) "Día $dayNumber/${data.optInt("cycleLength", 90)}" else "Pronto")
        val fresh = data.optString("today") == today.toString()

        if (valq) {
            views.setTextColor(R.id.phrase, p.accent)
            views.setTextViewText(R.id.phrase, if (fresh) data.optString("phrase") else "Toca para marcar tu día")
            views.setImageViewBitmap(R.id.chart, Chart.drawValQ(context, data, today, p))
            val prize = data.optJSONObject("prize")
            views.setTextViewText(R.id.line, prizeLine(prize) ?: "Un día ganado a la vez")
        } else {
            views.setTextColor(R.id.savings, p.accent)
            views.setTextViewText(R.id.savings, savingsLine(data))
            views.setImageViewBitmap(R.id.chart, Chart.drawBitacora(context, data, today, p))
            views.setTextViewText(R.id.line, nextStepLine(data, fresh))
        }
        return views
    }

    private fun prizeLine(prize: JSONObject?): String? {
        if (prize == null) return null
        val left = prize.optInt("daysLeft")
        return "${prize.optString("name")}: ${if (left == 1) "falta 1 día" else "faltan $left días"}"
    }

    private fun savingsLine(data: JSONObject): String {
        val bs = data.optInt("savedBs")
        val horas = data.optDouble("horas", 0.0)
        val h = if (horas == Math.floor(horas)) horas.toInt().toString() else String.format(java.util.Locale.ROOT, "%.1f", horas).replace('.', ',')
        return if (horas > 0) "Bs $bs · $h h" else "Bs $bs"
    }

    private fun nextStepLine(data: JSONObject, fresh: Boolean): String {
        if (!fresh) return "Toca para marcar tu día"
        if (data.isNull("nextStep")) return "Llegaste al último escalón"
        val n = data.optInt("nextStep")
        val kind = if (data.optString("nextKind") == "premio") "premio" else "cofre"
        return if (n <= 0) "¡Escalón hoy!" else "Tu $kind en ${if (n == 1) "1 día" else "$n días"}"
    }
}

class ValQWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, WidgetRenderer.render(context, "valq")) }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, ValQWidget::class.java)).forEach {
                manager.updateAppWidget(it, WidgetRenderer.render(context, "valq"))
            }
        }
    }
}

class BitacoraWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, WidgetRenderer.render(context, "bitacora")) }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, BitacoraWidget::class.java)).forEach {
                manager.updateAppWidget(it, WidgetRenderer.render(context, "bitacora"))
            }
        }
    }
}
