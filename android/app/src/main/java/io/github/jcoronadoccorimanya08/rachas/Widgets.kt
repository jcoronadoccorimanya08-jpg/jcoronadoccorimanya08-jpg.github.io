package io.github.jcoronadoccorimanya08.rachas

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.ZoneId

/** Colores de cada app (los mismos tokens de sus estilos). */
data class Palette(
    val accent: Int,
    val info: Int,
    val text: Int,
    val muted: Int,
    val faint: Int,
    val background: Int,
)

object Palettes {
    val VALQ = Palette(0xFF2EE6A8.toInt(), 0xFF5AB6FF.toInt(), 0xFFE9EEF5.toInt(), 0xFFA3AFC0.toInt(), 0x33FFFFFF, R.drawable.widget_bg_valq)
    val BITACORA = Palette(0xFF3BBFA7.toInt(), 0xFF5AB6FF.toInt(), 0xFFE4EDEA.toInt(), 0xFF94A49F.toInt(), 0x33FFFFFF, R.drawable.widget_bg_bitacora)
}

object WidgetRenderer {
    private val LA_PAZ: ZoneId = ZoneId.of("America/La_Paz")

    fun render(context: Context, appKey: String): RemoteViews {
        val p = if (appKey == "valq") Palettes.VALQ else Palettes.BITACORA
        val views = RemoteViews(context.packageName, R.layout.widget)
        val target = if (appKey == "valq") ValQActivity::class.java else BitacoraActivity::class.java
        val open = PendingIntent.getActivity(
            context,
            if (appKey == "valq") 1 else 2,
            Intent(context, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        views.setOnClickPendingIntent(R.id.widget_root, open)
        views.setTextColor(R.id.streak, p.text)
        views.setTextColor(R.id.day, p.muted)
        views.setTextColor(R.id.line, p.muted)

        val data = WidgetStore.load(context, appKey)
        if (data == null) {
            views.setInt(R.id.widget_root, "setBackgroundResource", p.background)
            views.setTextViewText(R.id.streak, "— días")
            views.setTextViewText(R.id.day, "")
            views.setTextViewText(R.id.line, "Ábrela una vez desde este ícono")
            views.setImageViewBitmap(R.id.chart, Chart.draw(context, emptyList(), 0, LocalDate.now(LA_PAZ), p, hot = false))
            return views
        }

        val streak = data.optInt("streak")
        val hot = streak >= 7
        views.setInt(R.id.widget_root, "setBackgroundResource", if (hot) R.drawable.widget_bg_hot else p.background)
        if (hot) {
            views.setTextColor(R.id.day, 0xE6FFFFFF.toInt())
            views.setTextColor(R.id.line, 0xE6FFFFFF.toInt())
        }
        views.setTextViewText(R.id.streak, "$streak ${if (streak == 1) "día" else "días"}")
        val dayNumber = data.optInt("dayNumber")
        val cycle = data.optInt("cycleLength", 90)
        views.setTextViewText(R.id.day, if (dayNumber > 0) "Día $dayNumber de $cycle" else "Empieza pronto")

        val today = LocalDate.now(LA_PAZ)
        val fresh = data.optString("today") == today.toString()
        val days = Chart.parseDays(data)
        val goal = data.optInt("goal", if (appKey == "valq") 65 else 100)
        views.setImageViewBitmap(R.id.chart, Chart.draw(context, days, goal, today, p, hot))

        val line = StringBuilder()
        if (fresh) {
            line.append("Hoy ${data.optInt("todayPct")} %")
            if (data.optBoolean("todayWon")) line.append(if (appKey == "valq") " · ganado ✓" else " · limpio ✓")
        } else {
            line.append("Toca para marcar tu día")
        }
        val prize = data.optJSONObject("prize")
        if (appKey == "valq" && prize != null) {
            val left = prize.optInt("daysLeft")
            line.append(" · ${prize.optString("name")}: ${if (left == 1) "1 día" else "$left días"}")
        }
        views.setTextViewText(R.id.line, line.toString())
        return views
    }
}

class ValQWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, WidgetRenderer.render(context, "valq")) }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ValQWidget::class.java))
            ids.forEach { manager.updateAppWidget(it, WidgetRenderer.render(context, "valq")) }
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
            val ids = manager.getAppWidgetIds(ComponentName(context, BitacoraWidget::class.java))
            ids.forEach { manager.updateAppWidget(it, WidgetRenderer.render(context, "bitacora")) }
        }
    }
}
