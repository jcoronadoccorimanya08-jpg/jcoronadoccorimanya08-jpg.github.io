package io.github.jcoronadoccorimanya08.rachas

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** La alarma de Focus Flow: se programa al empezar, se cancela al pausar y suena con aviso aunque estés en otra app. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FocusAlarmTest {
    private val ctx: Context get() = RuntimeEnvironment.getApplication()
    private fun msg(at: Long?) = """{"v":1,"app":"focusflow","kind":"timer","endsAt":${at ?: "null"},"title":"⏰ ¡Terminó tu sesión de estudio!","body":"+25 min"}"""

    @Test
    fun soloEntiendeLosMensajesDeLaAlarma() {
        assertNull(FocusAlarm.parse("""{"v":1,"app":"focusflow","today":"2026-11-28","streak":2}"""))
        assertNull(FocusAlarm.parse("rachas:hola"))
        assertEquals(123L, FocusAlarm.parse(msg(123L))!!.endsAt)
        assertNull(FocusAlarm.parse(msg(null))!!.endsAt)
    }

    @Test
    fun programaYCancela() {
        val now = 1_000_000L
        val manager = ctx.getSystemService(AlarmManager::class.java)
        val am = shadowOf(manager)
        assertTrue(FocusAlarm.handle(ctx, msg(now + 25 * 60_000), now))
        val at = manager.nextAlarmClock?.triggerTime ?: am.scheduledAlarms.firstOrNull()?.triggerAtTime
        assertEquals(now + 25 * 60_000, at)
        assertTrue(FocusAlarm.handle(ctx, msg(null), now))
        assertTrue(am.scheduledAlarms.isEmpty())
        assertFalse(FocusAlarm.handle(ctx, "no es json", now))
    }

    @Test
    fun suenaConAvisoQueSeRepite() {
        FocusAlarm.handle(ctx, msg(System.currentTimeMillis() + 60_000))
        FocusAlarm.show(ctx)
        val nm = shadowOf(ctx.getSystemService(NotificationManager::class.java))
        val n = nm.allNotifications.single()
        assertEquals("⏰ ¡Terminó tu sesión de estudio!", shadowOf(n).contentTitle.toString())
        assertTrue(n.flags and android.app.Notification.FLAG_INSISTENT != 0)
        FocusAlarm.cancel(ctx)
        assertTrue(nm.allNotifications.isEmpty())
    }
}
