package io.github.jcoronadoccorimanya08.rachas

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId

/** Zona horaria de las dos apps. */
val LA_PAZ: ZoneId = ZoneId.of("America/La_Paz")

/** Un día del resumen: fecha, % y si se ganó (o fue limpio). */
data class Day(val date: LocalDate, val pct: Int, val won: Boolean)

/** Un hábito del núcleo de Val Q: nombre corto, si ya lo marcaste y si cuenta hoy. */
data class Core(val name: String, val done: Boolean, val counts: Boolean)

/** La semana del ciclo en la que estás. */
data class Week(val n: Int, val start: LocalDate, val len: Int, val won: Int, val goal: Int)

data class Prize(val name: String, val daysLeft: Int)

/** Un hábito de Bitácora, sin nombre: días seguidos en «ok», % de días limpios y cómo va hoy (0 sin marcar, 1 ok, 2 caída). */
data class HabitLine(val streak: Int, val pct: Int, val today: Int)

/** Lo que muestra el widget en un día (lo calcula la app web; aquí solo se lee). */
data class Frame(
    val today: LocalDate,
    val dayNumber: Int,
    val cycleLength: Int,
    val start: LocalDate?,
    val streak: Int,
    val todayPct: Int,
    val todayWon: Boolean,
    val goal: Int,
    /** Val Q: hábitos que faltan para ganar hoy (null si ya no alcanza). */
    val need: Int?,
    /** Val Q: hábitos que cuentan hoy y no marcaste (null si la app web es antigua). */
    val left: Int?,
    val days: List<Day>,
    val week: Week?,
    val core: List<Core>,
    val phrase: String,
    val prize: Prize?,
    val badges: Int,
    /** Bitácora: ya marcaste todos tus hábitos hoy (null si la app web es antigua). */
    val todayDone: Boolean?,
    val savedBs: Int,
    val horas: Double,
    val nextStep: Int?,
    val nextKind: String?,
    val step: Int,
    /** Bitácora: cada hábito en el orden de la app (vacío si la app web es antigua). */
    val habits: List<HabitLine> = emptyList(),
) {
    val cycleEnd: LocalDate? get() = start?.plusDays((cycleLength - 1).toLong())
}

/** El último resumen que mandó una app: el día de hoy y los días adelantados. */
data class Payload(val app: String, val accent: String, val test: Boolean, val frames: List<Frame>)

enum class Mode {
    /** Todavía no llegó nada de la app. */
    EMPTY,

    /** Lo último que llegó es de hace más de dos días: hay que abrir la app. */
    STALE,

    /** El ciclo todavía no empieza. */
    BEFORE,
    ACTIVE,

    /** El ciclo ya terminó. */
    FINISHED,
}

/** Qué mostrar hoy: el cuadro del día (si hay) y en qué estado está. */
data class Shown(val payload: Payload?, val frame: Frame?, val mode: Mode) {
    val test: Boolean get() = payload?.test == true
}

object Model {
    fun parse(text: String?): Payload? {
        if (text.isNullOrBlank()) return null
        return try {
            val o = JSONObject(text)
            if (o.optInt("v") != 1) return null
            val first = frame(o) ?: return null
            val frames = ArrayList<Frame>()
            frames.add(first)
            o.optJSONArray("ahead")?.let { arr ->
                for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { f -> frame(f)?.let { frames.add(it) } }
            }
            Payload(o.optString("app"), o.optString("accent", "verde"), o.optBoolean("test", false), frames)
        } catch (_: Exception) {
            null
        }
    }

    private fun date(text: String?): LocalDate? = try {
        if (text.isNullOrBlank()) null else LocalDate.parse(text)
    } catch (_: Exception) {
        null
    }

    private fun frame(o: JSONObject): Frame? {
        val today = date(o.optString("today")) ?: return null
        if (!o.has("streak")) return null
        val days = ArrayList<Day>()
        o.optJSONArray("days")?.let { arr ->
            for (i in 0 until arr.length()) {
                val d = arr.optJSONObject(i) ?: continue
                val dd = date(d.optString("d")) ?: continue
                days.add(Day(dd, d.optInt("p").coerceIn(0, 100), d.optBoolean("w")))
            }
        }
        val week = o.optJSONObject("week")?.let { w ->
            date(w.optString("start"))?.let { Week(w.optInt("n", 1), it, w.optInt("len", 7).coerceIn(1, 7), w.optInt("won"), w.optInt("goal", 0)) }
        }
        val core = ArrayList<Core>()
        o.optJSONArray("core")?.let { arr: JSONArray ->
            for (i in 0 until arr.length()) {
                val c = arr.optJSONObject(i) ?: continue
                core.add(Core(c.optString("n"), c.optBoolean("done"), c.optBoolean("counts", true)))
            }
        }
        val prize = o.optJSONObject("prize")?.let { Prize(it.optString("name"), it.optInt("daysLeft")) }
        val habits = ArrayList<HabitLine>()
        o.optJSONArray("habits")?.let { arr ->
            for (i in 0 until arr.length()) {
                val h = arr.optJSONObject(i) ?: continue
                habits.add(HabitLine(h.optInt("s").coerceAtLeast(0), h.optInt("p").coerceIn(0, 100), h.optInt("t").coerceIn(0, 2)))
            }
        }
        return Frame(
            today = today,
            dayNumber = o.optInt("dayNumber"),
            cycleLength = o.optInt("cycleLength", 90).coerceAtLeast(1),
            start = date(o.optString("start")),
            streak = o.optInt("streak").coerceAtLeast(0),
            todayPct = o.optInt("todayPct").coerceIn(0, 100),
            todayWon = o.optBoolean("todayWon"),
            goal = o.optInt("goal", 65).coerceIn(0, 100),
            need = if (o.isNull("need") || !o.has("need")) null else o.optInt("need"),
            left = if (o.has("left")) o.optInt("left") else null,
            days = days,
            week = week,
            core = core,
            phrase = o.optString("phrase"),
            prize = prize,
            badges = o.optInt("badges").coerceIn(0, 6),
            todayDone = if (o.has("todayDone")) o.optBoolean("todayDone") else null,
            savedBs = o.optInt("savedBs"),
            horas = o.optDouble("horas", 0.0).let { if (it.isNaN()) 0.0 else it },
            nextStep = if (o.isNull("nextStep") || !o.has("nextStep")) null else o.optInt("nextStep"),
            nextKind = o.optString("nextKind").takeIf { it.isNotBlank() && it != "null" },
            step = o.optInt("step", 7).coerceAtLeast(1),
            habits = habits,
        )
    }

    /**
     * Elige el cuadro de hoy. En modo prueba se muestra tal cual (la fecha es simulada).
     * Si no abriste la app, usa los días adelantados que mandó; después de eso, pide abrirla.
     */
    fun pick(payload: Payload?, today: LocalDate): Shown {
        if (payload == null || payload.frames.isEmpty()) return Shown(null, null, Mode.EMPTY)
        val first = payload.frames.first()
        val frame = when {
            payload.test -> first
            today <= first.today -> first
            else -> payload.frames.firstOrNull { it.today == today }
        } ?: return Shown(payload, null, Mode.STALE)
        val end = frame.cycleEnd
        val mode = when {
            frame.dayNumber <= 0 -> Mode.BEFORE
            end != null && frame.today > end -> Mode.FINISHED
            else -> Mode.ACTIVE
        }
        return Shown(payload, frame, mode)
    }
}
