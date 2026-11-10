package io.github.jcoronadoccorimanya08.rachas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.View
import android.widget.FrameLayout
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import kotlin.math.roundToInt

/**
 * Dibuja los widgets con datos DE EJEMPLO (nada personal) y guarda las imágenes para revisarlas sin teléfono.
 * También genera la vista previa de la lista de widgets (res/drawable-nodpi/preview_*.png) si se pide
 * con RACHAS_PREVIEW_RES=1. Las imágenes van a RACHAS_PREVIEWS (o a build/previews).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class WidgetPreviewTest {
    private val ctx: Context get() = RuntimeEnvironment.getApplication()
    private val out: File by lazy { File(System.getenv("RACHAS_PREVIEWS") ?: "build/previews").apply { mkdirs() } }

    // ------------------------------------------------------------ Datos de ejemplo

    private val start = LocalDate.parse("2026-11-09")

    private fun at(text: String): ZonedDateTime = LocalDateTime.parse(text).atZone(LA_PAZ)

    private fun days(today: LocalDate, pcts: List<Int>, goal: Int = 65): JSONArray {
        val arr = JSONArray()
        val first = today.minusDays((pcts.size - 1).toLong())
        pcts.forEachIndexed { i, p -> arr.put(JSONObject().put("d", first.plusDays(i.toLong()).toString()).put("p", p).put("w", p >= goal)) }
        return arr
    }

    private fun valqFrame(
        today: LocalDate,
        streak: Int,
        pcts: List<Int>,
        coreDone: List<Boolean>,
        need: Int?,
        left: Int,
        phrase: String,
        badges: Int = 0,
    ): JSONObject {
        val dayNumber = (today.toEpochDay() - start.toEpochDay() + 1).toInt()
        val weekStart = start.plusDays(((dayNumber - 1) / 7 * 7).toLong())
        val todayPct = if (dayNumber >= 1) pcts.last() else 0
        val week = JSONObject().put("n", (dayNumber - 1) / 7 + 1).put("start", weekStart.toString()).put("len", 7)
            .put("won", if (dayNumber >= 1) pcts.takeLast((today.toEpochDay() - weekStart.toEpochDay() + 1).toInt()).count { it >= 65 } else 0).put("goal", 6)
        val names = listOf("Dormir", "Rutina", "Gym", "Bloque", "Lectura").take(coreDone.size)
        val core = JSONArray()
        names.forEachIndexed { i, n -> core.put(JSONObject().put("n", n).put("done", coreDone[i]).put("counts", true)) }
        return JSONObject()
            .put("today", today.toString())
            .put("dayNumber", maxOf(0, dayNumber))
            .put("cycleLength", 90)
            .put("start", start.toString())
            .put("streak", streak)
            .put("todayPct", todayPct)
            .put("todayWon", dayNumber >= 1 && todayPct >= 65)
            .put("goal", 65)
            .put("need", need ?: JSONObject.NULL)
            .put("left", left)
            .put("days", if (dayNumber >= 1) days(today, pcts) else JSONArray())
            .put("week", week)
            .put("prize", JSONObject().put("name", "Gran premio").put("daysLeft", (start.plusDays(89).toEpochDay() - today.toEpochDay()).toInt()))
            .put("core", core)
            .put("phrase", phrase)
            .put("badges", badges)
    }

    private fun payload(app: String, frame: JSONObject, ahead: List<JSONObject> = emptyList(), test: Boolean = false, accent: String = "verde"): String {
        val o = JSONObject(frame.toString()).put("v", 1).put("app", app).put("accent", accent).put("ahead", JSONArray(ahead))
        if (test) o.put("test", true)
        return o.toString()
    }

    /** Cinco hábitos de ejemplo: días seguidos, % limpio y cómo van hoy (0 sin marcar, 1 ok, 2 caída). */
    private val sampleHabits = listOf(Triple(9, 100, 1), Triple(9, 100, 1), Triple(2, 79, 1), Triple(9, 93, 0), Triple(5, 86, 1))

    private fun bitacoraFrame(today: LocalDate, streak: Int, pcts: List<Int>, done: Boolean, next: Int?, badges: Int = 0, habits: List<Triple<Int, Int, Int>> = sampleHabits): JSONObject {
        val dayNumber = (today.toEpochDay() - start.toEpochDay() + 1).toInt()
        val weekStart = start.plusDays(((dayNumber - 1) / 7 * 7).toLong())
        val todayPct = if (dayNumber >= 1) pcts.last() else 0
        val week = JSONObject().put("n", (dayNumber - 1) / 7 + 1).put("start", weekStart.toString()).put("len", 7)
            .put("won", if (dayNumber >= 1) pcts.takeLast((today.toEpochDay() - weekStart.toEpochDay() + 1).toInt()).count { it == 100 } else 0)
        return JSONObject()
            .put("today", today.toString())
            .put("dayNumber", maxOf(0, dayNumber))
            .put("cycleLength", 90)
            .put("start", start.toString())
            .put("streak", streak)
            .put("best", streak)
            .put("todayPct", todayPct)
            .put("todayWon", dayNumber >= 1 && todayPct == 100)
            .put("todayDone", done)
            .put("days", if (dayNumber >= 1) days(today, pcts, 100) else JSONArray())
            .put("week", week)
            .put("savedBs", if (dayNumber >= 1) 1234 else 0)
            .put("horas", if (dayNumber >= 1) 12.5 else 0.0)
            .put("nextStep", next ?: JSONObject.NULL)
            .put("nextKind", "premio")
            .put("step", 7)
            .put("badges", badges)
            .put("habits", JSONArray(if (dayNumber >= 1) habits.map { (st, pc, t) -> JSONObject().put("s", st).put("p", pc).put("t", t) } else emptyList()))
    }

    private val thu = LocalDate.parse("2026-11-26")
    private val valqPcts = listOf(70, 80, 66, 90, 75, 68, 88, 71, 95, 82, 85, 70, 92, 72)

    private fun valqScenarios(): List<Triple<String, String?, ZonedDateTime>> = listOf(
        Triple(
            "valq-1-dia-ganado",
            payload("valq", valqFrame(thu, 12, valqPcts, listOf(true, true, true, false), 0, 3, "¡Gran racha! Día ganado", badges = 1)),
            at("2026-11-26T18:40:00"),
        ),
        Triple(
            "valq-2-noche-falta",
            payload("valq", valqFrame(thu, 11, valqPcts.dropLast(1) + 45, listOf(true, true, false, false), 2, 6, "¡Gran racha! Hoy también", badges = 1)),
            at("2026-11-26T21:40:00"),
        ),
        Triple(
            "valq-3-semana-ganada",
            payload("valq", valqFrame(LocalDate.parse("2026-11-28"), 14, valqPcts.drop(2) + listOf(78, 81), listOf(true, true, true, true), 0, 0, "¡Gran racha! Día ganado", badges = 1), accent = "violeta"),
            at("2026-11-28T20:10:00"),
        ),
        Triple(
            "valq-8-nucleo-de-5",
            payload("valq", valqFrame(thu, 12, valqPcts, listOf(true, true, true, false, true), 0, 2, "¡Gran racha! Día ganado", badges = 1)),
            at("2026-11-26T18:40:00"),
        ),
        Triple("valq-4-sin-datos", null, at("2026-11-08T14:00:00")),
        Triple(
            "valq-5-antes-del-ciclo",
            payload("valq", valqFrame(LocalDate.parse("2026-11-08"), 0, emptyList(), listOf(false, false, false, false), null, 0, "Tu ciclo está por empezar")),
            at("2026-11-08T14:00:00"),
        ),
        Triple(
            "valq-6-modo-prueba",
            payload("valq", valqFrame(LocalDate.parse("2026-11-13"), 4, listOf(80, 72, 90, 66, 30), listOf(true, false, false, false), 2, 7, "Vas encendido, sigue"), test = true),
            at("2026-11-08T14:00:00"),
        ),
        Triple(
            "valq-7-empieza-hoy",
            payload("valq", valqFrame(LocalDate.parse("2026-11-08"), 0, emptyList(), listOf(false, false, false, false), null, 0, "Tu ciclo está por empezar"), ahead = listOf(valqFrame(start, 0, listOf(0), listOf(false, false, false, false), 4, 9, "Hoy empieza tu racha"))),
            at("2026-11-09T07:30:00"),
        ),
    )

    private fun bitacoraScenarios(): List<Triple<String, String?, ZonedDateTime>> = listOf(
        Triple("bitacora-1-dia-limpio", payload("bitacora", bitacoraFrame(thu, 9, listOf(100, 100, 50, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100), true, 5)), at("2026-11-26T18:40:00")),
        Triple("bitacora-2-noche-falta", payload("bitacora", bitacoraFrame(thu, 8, listOf(100, 100, 50, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 50), false, 6, habits = listOf(Triple(8, 100, 1), Triple(0, 86, 2), Triple(3, 79, 0), Triple(8, 93, 0), Triple(5, 86, 1)))), at("2026-11-26T21:40:00")),
        Triple("bitacora-3-sin-datos", null, at("2026-11-08T14:00:00")),
        Triple("bitacora-4-antes-del-ciclo", payload("bitacora", bitacoraFrame(LocalDate.parse("2026-11-08"), 0, emptyList(), false, 7)), at("2026-11-08T14:00:00")),
        Triple("bitacora-5-insignia", payload("bitacora", bitacoraFrame(LocalDate.parse("2026-11-24"), 15, List(14) { if (it == 4) 50 else 100 }, true, 6, badges = 2), accent = "lima"), at("2026-11-24T12:00:00")),
    )

    // ------------------------------------------------------------ Dibujo

    private fun store(app: String, json: String?) {
        val e = ctx.getSharedPreferences("widgets", Context.MODE_PRIVATE).edit()
        if (json == null) e.remove(app) else e.putString(app, json)
        e.commit()
    }

    private fun widgetBitmap(app: String, now: ZonedDateTime, wDp: Int = 333, hDp: Int = 180): Bitmap {
        val rv = WidgetRenderer.render(ctx, app, wDp, hDp, now)
        val view = rv.apply(ctx, FrameLayout(ctx))
        val d = ctx.resources.displayMetrics.density
        val w = (wDp * d).roundToInt()
        val h = (hDp * d).roundToInt()
        view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, w, h)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bmp))
        return bmp
    }

    /** El widget sobre un fondo oscuro parecido a una pantalla de inicio. */
    private fun onWallpaper(widgets: List<Bitmap>): Bitmap {
        val d = ctx.resources.displayMetrics.density
        val pad = (24 * d).roundToInt()
        val gap = (22 * d).roundToInt()
        val w = widgets.maxOf { it.width } + 2 * pad
        val h = widgets.sumOf { it.height } + gap * (widgets.size - 1) + 2 * pad
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawPaint(Paint().apply { shader = LinearGradient(0f, 0f, 0f, h.toFloat(), 0xFF2B3550.toInt(), 0xFF3A2E3C.toInt(), Shader.TileMode.CLAMP) })
        var y = pad.toFloat()
        widgets.forEach {
            c.drawBitmap(it, pad.toFloat(), y, null)
            y += it.height + gap
        }
        return bmp
    }

    private fun save(bmp: Bitmap, file: File) {
        file.parentFile?.mkdirs()
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun dibujaLosWidgets() {
        val all = ArrayList<Pair<String, Bitmap>>()
        for ((name, json, now) in valqScenarios()) {
            store("valq", json)
            all.add(name to widgetBitmap("valq", now))
        }
        for ((name, json, now) in bitacoraScenarios()) {
            store("bitacora", json)
            all.add(name to widgetBitmap("bitacora", now))
        }
        all.forEach { (name, bmp) -> save(onWallpaper(listOf(bmp)), File(out, "$name.png")) }
        // La pantalla de inicio: Bitácora arriba y Val Q abajo, como en tu teléfono.
        save(onWallpaper(listOf(all.first { it.first == "bitacora-1-dia-limpio" }.second, all.first { it.first == "valq-1-dia-ganado" }.second)), File(out, "pantalla.png"))

        if (System.getenv("RACHAS_PREVIEW_RES") == "1") {
            val res = File(System.getenv("RACHAS_RES_DIR") ?: "src/main/res/drawable-nodpi")
            save(all.first { it.first == "valq-1-dia-ganado" }.second, File(res, "preview_valq.png"))
            save(all.first { it.first == "bitacora-1-dia-limpio" }.second, File(res, "preview_bitacora.png"))
        }
    }

    // ------------------------------------------------------------ Reglas

    private fun fixture(name: String): String = requireNotNull(javaClass.classLoader?.getResource(name)) { name }.readText()

    /** Resúmenes reales generados por buildWidgetPayload de cada app web (datos de ejemplo). */
    @Test
    fun entiendeElResumenRealDeLasApps() {
        val vq = fixture("payload_valq.json")
        assertTrue(WidgetStore.save(ctx, "valq", vq))
        assertFalse("el resumen de Val Q no se guarda como Bitácora", WidgetStore.save(ctx, "bitacora", vq))
        val p = WidgetStore.load(ctx, "valq")
        assertEquals(Mode.ACTIVE, Model.pick(p, LocalDate.parse("2026-11-10")).mode)
        assertEquals(2, Model.pick(p, LocalDate.parse("2026-11-10")).frame?.dayNumber)
        assertEquals(listOf("Dormir", "Rutina", "Gym", "Bloque"), Model.pick(p, LocalDate.parse("2026-11-10")).frame?.core?.map { it.name })
        assertEquals(3, Model.pick(p, LocalDate.parse("2026-11-11")).frame?.dayNumber)
        assertEquals(Mode.STALE, Model.pick(p, LocalDate.parse("2026-11-13")).mode)
        val bt = fixture("payload_bitacora.json")
        assertTrue(WidgetStore.save(ctx, "bitacora", bt))
        assertEquals(1, Model.pick(WidgetStore.load(ctx, "bitacora"), LocalDate.parse("2026-11-10")).frame?.streak)
        assertEquals(listOf(HabitLine(2, 100, 1), HabitLine(1, 100, 0)), Model.pick(WidgetStore.load(ctx, "bitacora"), LocalDate.parse("2026-11-10")).frame?.habits)
        assertFalse(WidgetStore.save(ctx, "valq", "no es un resumen"))
        val now = at("2026-11-10T08:00:00")
        save(onWallpaper(listOf(widgetBitmap("bitacora", now), widgetBitmap("valq", now))), File(out, "real-dia-2.png"))
        // Sin datos y con el último paso de la conexión.
        store("valq", null)
        store("bitacora", null)
        Diag.mark(ctx, "valq", Diag.READY)
        save(onWallpaper(listOf(widgetBitmap("bitacora", now), widgetBitmap("valq", now))), File(out, "vacio-con-diagnostico.png"))
    }


    @Test
    fun eligeElDiaCorrecto() {
        val p = Model.parse(payload("valq", valqFrame(thu, 12, valqPcts, listOf(true, true, true, false), 0, 3, "x"), ahead = listOf(
            valqFrame(thu.plusDays(1), 12, valqPcts.drop(1) + 0, listOf(false, false, false, false), 4, 9, "y"),
            valqFrame(thu.plusDays(2), 0, valqPcts.drop(2) + listOf(0, 0), listOf(false, false, false, false), 4, 9, "z"),
        )))
        assertNotNull(p)
        assertEquals(thu, Model.pick(p, thu).frame?.today)
        assertEquals(Mode.ACTIVE, Model.pick(p, thu.plusDays(1)).mode)
        assertEquals("y", Model.pick(p, thu.plusDays(1)).frame?.phrase)
        assertEquals("z", Model.pick(p, thu.plusDays(2)).frame?.phrase)
        assertEquals(Mode.STALE, Model.pick(p, thu.plusDays(3)).mode)
        assertNull(Model.pick(p, thu.plusDays(3)).frame)
        assertEquals(Mode.EMPTY, Model.pick(null, thu).mode)
        // Modo prueba: la fecha es simulada, se muestra tal cual.
        val test = Model.parse(payload("valq", valqFrame(thu, 3, valqPcts, listOf(true, true, true, false), 0, 3, "x"), test = true))
        assertEquals(thu, Model.pick(test, LocalDate.parse("2026-11-08")).frame?.today)
        // Antes del ciclo.
        val before = Model.parse(payload("valq", valqFrame(LocalDate.parse("2026-11-08"), 0, emptyList(), listOf(false, false, false, false), null, 0, "x")))
        assertEquals(Mode.BEFORE, Model.pick(before, LocalDate.parse("2026-11-08")).mode)
    }

    @Test
    fun recordatorioSoloSiFaltaAnotar() {
        val vqDone = Model.pick(Model.parse(payload("valq", valqFrame(thu, 12, valqPcts, listOf(true, true, true, true), 0, 0, "x"))), thu)
        val vqHalf = Model.pick(Model.parse(payload("valq", valqFrame(thu, 12, valqPcts.dropLast(1) + 45, listOf(true, true, false, false), 2, 6, "x"))), thu)
        val btDone = Model.pick(Model.parse(payload("bitacora", bitacoraFrame(thu, 9, List(14) { 100 }, true, 5))), thu)
        val btOpen = Model.pick(Model.parse(payload("bitacora", bitacoraFrame(thu, 9, List(14) { 100 }, false, 5))), thu)
        val before = Model.pick(Model.parse(payload("valq", valqFrame(LocalDate.parse("2026-11-08"), 0, emptyList(), listOf(false, false, false, false), null, 0, "x"))), LocalDate.parse("2026-11-08"))
        val btBefore = Model.pick(Model.parse(payload("bitacora", bitacoraFrame(LocalDate.parse("2026-11-08"), 0, emptyList(), false, 7))), LocalDate.parse("2026-11-08"))

        assertNull("ya anotaste todo", Reminder.compose(vqDone, btDone))
        assertNull("el ciclo todavía no empieza", Reminder.compose(before, btBefore))
        val m = Reminder.compose(vqHalf, btOpen)!!
        assertEquals("Hora de anotar tu día", m.title)
        assertTrue(m.text, m.text.contains("45 %") && m.text.contains("faltan 2 hábitos"))
        assertTrue(m.text.contains("Bitácora: marca tu día."))
        assertEquals("valq", m.app)
        assertEquals("bitacora", Reminder.compose(vqDone, btOpen)!!.app)
        // Sin datos todavía: igual te recuerda.
        assertNotNull(Reminder.compose(Model.pick(null, thu), Model.pick(null, thu)))
    }

    @Test
    fun horasDelReloj() {
        assertEquals(at("2026-11-26T21:00:00"), Ticker.nextTick(at("2026-11-26T18:40:00")))
        assertEquals(at("2026-11-27T00:00:30"), Ticker.nextTick(at("2026-11-26T21:00:00")))
        assertEquals(at("2026-11-26T22:30:00"), Ticker.nextReminder(at("2026-11-26T18:40:00")))
        assertEquals(at("2026-11-27T22:30:00"), Ticker.nextReminder(at("2026-11-26T22:30:00")))
    }
}
