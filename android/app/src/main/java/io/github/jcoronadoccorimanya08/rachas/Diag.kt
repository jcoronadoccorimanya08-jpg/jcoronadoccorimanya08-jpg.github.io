package io.github.jcoronadoccorimanya08.rachas

import android.content.Context
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Rastro del último paso de la conexión con cada app web (sin datos de la persona: solo el paso y la hora).
 * Mientras el widget no tiene datos, muestra este paso: así una captura dice dónde se quedó.
 */
object Diag {
    private const val PREFS = "diag"

    const val OPENED = "abierta"
    const val NO_CHROME = "sin_chrome"
    const val VALIDATED = "validada"
    const val NOT_VALIDATED = "no_validada"
    const val PAGE = "pagina"
    const val REQUESTED = "canal_pedido"
    const val REJECTED = "canal_rechazado"
    const val READY = "canal_listo"
    const val BAD_DATA = "datos_malos"
    const val SAVED = "datos_ok"

    fun mark(context: Context, appKey: String, step: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(appKey, step)
            .putLong("${appKey}_at", System.currentTimeMillis())
            .apply()
        // Si el widget todavía no tiene datos, que muestre el paso nuevo.
        if (step != SAVED && WidgetStore.load(context, appKey) == null) {
            if (appKey == "valq") ValQWidget.updateAll(context) else BitacoraWidget.updateAll(context)
        }
    }

    /** Texto corto para el widget vacío, con la hora del paso («Conectando… 06:02»). */
    fun text(context: Context, appKey: String): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val step = prefs.getString(appKey, null) ?: return "Sin abrir desde Rachas"
        val at = prefs.getLong("${appKey}_at", 0L)
        val label = when (step) {
            OPENED -> "Abriendo…"
            NO_CHROME -> "Falta Chrome"
            NOT_VALIDATED -> "Chrome no verificó"
            VALIDATED -> "Verificada"
            PAGE -> "Página lista"
            REJECTED -> "Chrome no abrió el canal"
            REQUESTED -> "Conectando…"
            READY -> "Conectada, sin datos"
            BAD_DATA -> "Datos no válidos"
            else -> "Listo"
        }
        if (at <= 0L) return label
        val time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(at), LA_PAZ).format(DateTimeFormatter.ofPattern("HH:mm"))
        return "$label $time"
    }
}
