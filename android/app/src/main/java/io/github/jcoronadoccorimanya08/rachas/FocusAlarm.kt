package io.github.jcoronadoccorimanya08.rachas

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import org.json.JSONObject

/**
 * La alarma de Focus Flow: cuando empiezas una sesión, la página le dice a Rachas a qué hora termina y Rachas
 * programa una alarma del teléfono. Así suena (fuerte, con el sonido de alarma) aunque estés en WhatsApp,
 * Facebook o con la pantalla apagada, cuando Chrome ya durmió la página. Si pausas o reinicias, se cancela.
 * Sin internet: todo queda en el teléfono.
 */
object FocusAlarm {
    const val ACTION_FIRE = "io.github.jcoronadoccorimanya08.rachas.FOCUS_FIRE"
    const val ACTION_DISMISS = "io.github.jcoronadoccorimanya08.rachas.FOCUS_DISMISS"
    private const val PREFS = "focus_alarm"
    private const val CHANNEL = "focusflow_fin"
    private const val NOTIF_ID = 2531
    private const val REQ_FIRE = 31
    private const val REQ_DISMISS = 32
    private const val REQ_OPEN = 33

    /** Lo que manda la página: la hora de fin (ms) o null para cancelar, y el texto del aviso. */
    data class Plan(val endsAt: Long?, val title: String, val body: String)

    /** Lee el mensaje de la página. null si no es de la alarma de Focus Flow. */
    fun parse(message: String): Plan? {
        if (!message.startsWith("{")) return null
        return try {
            val o = JSONObject(message)
            if (o.optInt("v") != 1 || o.optString("app") != "focusflow" || o.optString("kind") != "timer") return null
            val at = if (o.isNull("endsAt") || !o.has("endsAt")) null else o.optLong("endsAt").takeIf { it > 0 }
            Plan(at, o.optString("title", "¡Terminó tu sesión!").take(60), o.optString("body", "").take(140))
        } catch (_: Exception) {
            null
        }
    }

    /** Devuelve true si el mensaje era de la alarma (y ya se programó o canceló). */
    fun handle(context: Context, message: String, now: Long = System.currentTimeMillis()): Boolean {
        val plan = parse(message) ?: return false
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (plan.endsAt == null || plan.endsAt <= now) {
            prefs.edit().clear().apply()
            cancel(context)
        } else {
            prefs.edit().putLong("at", plan.endsAt).putString("title", plan.title).putString("body", plan.body).apply()
            schedule(context, plan.endsAt)
        }
        return true
    }

    /** Después de reiniciar el teléfono: vuelve a programar la que estaba pendiente. */
    fun restore(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val at = prefs.getLong("at", 0L)
        if (at > System.currentTimeMillis()) schedule(context, at) else prefs.edit().clear().apply()
    }

    private fun firePending(context: Context): PendingIntent =
        PendingIntent.getBroadcast(context, REQ_FIRE, Intent(context, FocusAlarmReceiver::class.java).setAction(ACTION_FIRE), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun openPending(context: Context): PendingIntent =
        PendingIntent.getActivity(context, REQ_OPEN, Intent(context, FocusActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun schedule(context: Context, at: Long) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = firePending(context)
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
                // Como un despertador: suena a la hora exacta aunque el teléfono esté en reposo.
                am.setAlarmClock(AlarmManager.AlarmClockInfo(at, openPending(context)), pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(firePending(context))
        context.getSystemService(NotificationManager::class.java)?.cancel(NOTIF_ID)
    }

    /** Suena: aviso encima de cualquier app, con el sonido de alarma, que se repite hasta que lo toques (máx. 2 min). */
    fun show(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val title = prefs.getString("title", null) ?: "¡Terminó tu sesión!"
        val body = prefs.getString("body", null) ?: "Abre Focus Flow."
        prefs.edit().clear().apply()
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Focus Flow: fin de sesión", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Suena fuerte cuando termina tu sesión de estudio o tu pausa, aunque estés en otra app."
                setSound(sound, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 600)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(false)
            },
        )
        val dismiss = PendingIntent.getBroadcast(context, REQ_DISMISS, Intent(context, FocusAlarmReceiver::class.java).setAction(ACTION_DISMISS), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val icon = Icon.createWithResource(context, R.drawable.ic_flame)
        val n = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_flame)
            .setContentTitle(title)
            .setContentText(body)
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(openPending(context))
            .setAutoCancel(true)
            .setTimeoutAfter(120_000)
            .addAction(Notification.Action.Builder(icon, "Listo", dismiss).build())
            .addAction(Notification.Action.Builder(icon, "Abrir Focus Flow", openPending(context)).build())
            .build()
        // Se repite el sonido hasta que la toques (como un despertador).
        n.flags = n.flags or Notification.FLAG_INSISTENT
        try {
            nm.notify(NOTIF_ID, n)
        } catch (_: SecurityException) {
            // Sin permiso de avisos: no hay nada que hacer aquí.
        }
    }
}

class FocusAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            FocusAlarm.ACTION_FIRE -> FocusAlarm.show(context)
            FocusAlarm.ACTION_DISMISS -> FocusAlarm.cancel(context)
        }
    }
}
