package io.github.jcoronadoccorimanya08.rachas

import android.content.Context
import org.json.JSONObject

/** Guarda el último resumen de cada app (solo números) y avisa a los widgets. */
object WidgetStore {
    private const val PREFS = "widgets"

    fun save(context: Context, appKey: String, message: String) {
        val data = parse(message) ?: return
        if (data.optString("app") != appKey) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(appKey, message).apply()
        when (appKey) {
            "valq" -> ValQWidget.updateAll(context)
            "bitacora" -> BitacoraWidget.updateAll(context)
        }
    }

    fun load(context: Context, appKey: String): JSONObject? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(appKey, null)?.let { parse(it) }

    private fun parse(text: String): JSONObject? = try {
        JSONObject(text).takeIf { it.optInt("v") == 1 && it.has("streak") && it.has("days") }
    } catch (_: Exception) {
        null
    }
}
