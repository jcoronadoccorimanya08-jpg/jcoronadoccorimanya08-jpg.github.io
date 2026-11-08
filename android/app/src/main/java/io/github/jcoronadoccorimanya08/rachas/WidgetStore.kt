package io.github.jcoronadoccorimanya08.rachas

import android.content.Context

/** Guarda el último resumen de cada app (lo que muestra el widget) y avisa a los widgets. */
object WidgetStore {
    private const val PREFS = "widgets"

    /** Devuelve true si el resumen es válido y quedó guardado. */
    fun save(context: Context, appKey: String, message: String): Boolean {
        val data = Model.parse(message) ?: return false
        if (data.app != appKey) return false
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(appKey, message).apply()
        when (appKey) {
            "valq" -> ValQWidget.updateAll(context)
            "bitacora" -> BitacoraWidget.updateAll(context)
        }
        return true
    }

    fun load(context: Context, appKey: String): Payload? =
        Model.parse(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(appKey, null))
}
