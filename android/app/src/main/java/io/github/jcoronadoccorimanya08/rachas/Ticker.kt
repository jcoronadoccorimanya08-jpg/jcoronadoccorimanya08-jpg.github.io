package io.github.jcoronadoccorimanya08.rachas

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Relojes de la app, sin internet:
 * - los widgets cambian de día a medianoche y a las 21:00 muestran el borde si falta marcar;
 * - a las 22:30 llega el recordatorio para anotar tu lista (si ya anotaste todo, no molesta).
 */
object Ticker {
    private const val ACTION_TICK = "io.github.jcoronadoccorimanya08.rachas.TICK"
    const val ACTION_REMINDER = "io.github.jcoronadoccorimanya08.rachas.REMINDER"

    /** Hora del recordatorio (hora de La Paz). */
    val REMINDER_TIME: LocalTime = LocalTime.of(22, 30)

    fun scheduleAll(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val now = ZonedDateTime.now(LA_PAZ)
        try {
            val tick = pending(context, ACTION_TICK, 20)
            if (Widgets.hasAny(context)) am.setAndAllowWhileIdle(AlarmManager.RTC, nextTick(now).toInstant().toEpochMilli(), tick)
            else am.cancel(tick)

            val reminder = pending(context, ACTION_REMINDER, 21)
            val at = nextReminder(now).toInstant().toEpochMilli()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, reminder)
            } else {
                am.setWindow(AlarmManager.RTC_WAKEUP, at, 10 * 60_000L, reminder)
            }
        } catch (_: SecurityException) {
            // Sin permiso de alarmas exactas: el widget igual se actualiza al abrir la app.
        }
    }

    /** Próximo cambio del widget: hoy a las 21:00 o, si ya pasó, a medianoche. */
    fun nextTick(now: ZonedDateTime): ZonedDateTime {
        val evening = now.toLocalDate().atTime(WidgetRenderer.EVENING_HOUR, 0).atZone(LA_PAZ)
        return if (now.isBefore(evening)) evening else now.toLocalDate().plusDays(1).atStartOfDay(LA_PAZ).plusSeconds(30)
    }

    fun nextReminder(now: ZonedDateTime): ZonedDateTime {
        val t = now.toLocalDate().atTime(REMINDER_TIME).atZone(LA_PAZ)
        return if (now.isBefore(t)) t else t.plusDays(1)
    }

    private fun pending(context: Context, action: String, code: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, code, Intent(context, TickReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}

/** Despierta a los widgets y al recordatorio. También al reiniciar el teléfono o al actualizar la app. */
class TickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Ticker.ACTION_REMINDER) Reminder.show(context)
        ValQWidget.updateAll(context)
        BitacoraWidget.updateAll(context)
        Ticker.scheduleAll(context)
    }
}

/** El recordatorio de las 22:30. En la pantalla de bloqueo solo dice «Hora de anotar tu día». */
object Reminder {
    private const val CHANNEL = "recordatorio"
    private const val ID = 2230

    data class Message(val title: String, val text: String, val app: String)

    fun allowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(context: Context, today: LocalDate = LocalDate.now(LA_PAZ)) {
        if (!allowed(context)) return
        // Lo del modo prueba no cuenta: la fecha es simulada.
        val vq = Model.pick(WidgetStore.load(context, "valq")?.takeUnless { it.test }, today)
        val bt = Model.pick(WidgetStore.load(context, "bitacora")?.takeUnless { it.test }, today)
        val msg = compose(vq, bt) ?: return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Recordatorio de las 22:30", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Para anotar tu lista en Val Q y en Bitácora antes de dormir."
            },
        )
        val icon = Icon.createWithResource(context, R.drawable.ic_stat_flame)
        val public = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_flame)
            .setContentTitle(msg.title)
            .build()
        val n = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_flame)
            .setColor(0xFFFF7A3D.toInt())
            .setContentTitle(msg.title)
            .setContentText(msg.text.lineSequence().first())
            .setStyle(Notification.BigTextStyle().bigText(msg.text))
            .setContentIntent(WidgetRenderer.open(context, msg.app, urge = false))
            .addAction(Notification.Action.Builder(icon, "Val Q", WidgetRenderer.open(context, "valq", urge = false)).build())
            .addAction(Notification.Action.Builder(icon, "Bitácora", WidgetRenderer.open(context, "bitacora", urge = false)).build())
            .setCategory(Notification.CATEGORY_REMINDER)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(public)
            .setAutoCancel(true)
            .build()
        nm.notify(ID, n)
    }

    /** El texto según lo que ya marcaste hoy. Null si no hay nada que anotar. */
    fun compose(vq: Shown, bt: Shown): Message? {
        val vqOff = vq.mode == Mode.BEFORE || vq.mode == Mode.FINISHED
        val btOff = bt.mode == Mode.BEFORE || bt.mode == Mode.FINISHED
        if (vqOff && btOff) return null
        val vqDone = vq.mode == Mode.ACTIVE && vq.frame?.left == 0
        val btDone = bt.mode == Mode.ACTIVE && bt.frame?.todayDone == true
        if ((vqDone || vqOff) && (btDone || btOff)) return null
        val lines = ArrayList<String>()
        if (!vqOff) lines.add(valqLine(vq))
        if (!btOff) lines.add(if (btDone) "Bitácora: lista ✓" else "Bitácora: marca tu día.")
        return Message("Hora de anotar tu día", lines.joinToString("\n"), if (!vqDone && !vqOff) "valq" else "bitacora")
    }

    private fun valqLine(vq: Shown): String {
        val f = vq.frame
        if (vq.mode != Mode.ACTIVE || f == null) return "Val Q: marca tu lista."
        if (f.left == 0) return "Val Q: todo anotado ✓"
        if (f.todayWon) return "Val Q: día ganado ✓ Anota lo que falta."
        return when (val n = f.need) {
            null -> "Val Q: vas ${f.todayPct} %. Anota lo que hiciste hoy."
            1 -> "Val Q: vas ${f.todayPct} %. Te falta 1 hábito para ganar el día."
            else -> "Val Q: vas ${f.todayPct} %. Te faltan $n hábitos para ganar el día."
        }
    }
}
