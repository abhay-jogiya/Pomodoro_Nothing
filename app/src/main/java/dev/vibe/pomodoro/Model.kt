package dev.vibe.pomodoro

enum class Phase(val label: String, val title: String) {
    FOCUS("FOCUS", "Focus"),
    SHORT_BREAK("SHORT BREAK", "Short break"),
    LONG_BREAK("LONG BREAK", "Long break"),
}

data class Settings(
    val focusMin: Int = 25,
    val shortMin: Int = 5,
    val longMin: Int = 15,
    val sessionsUntilLong: Int = 4,
    val autoStart: Boolean = false,
    /** Widget background opacity, 5..100 (percent). */
    val widgetOpacity: Int = 35,
) {
    fun durationMs(phase: Phase): Long {
        val minutes = when (phase) {
            Phase.FOCUS -> focusMin
            Phase.SHORT_BREAK -> shortMin
            Phase.LONG_BREAK -> longMin
        }
        return minutes * 60_000L
    }
}

data class TimerState(
    val phase: Phase = Phase.FOCUS,
    val running: Boolean = false,
    /** Wall-clock time (ms) the running phase ends. Only meaningful while [running]. */
    val endAt: Long = 0L,
    /** Time left (ms) while paused / idle. */
    val remainingMs: Long = 25 * 60_000L,
    /** Focus sessions completed in the current cycle. */
    val focusDone: Int = 0,
    val settings: Settings = Settings(),
) {
    fun remaining(now: Long = System.currentTimeMillis()): Long =
        if (running) (endAt - now).coerceAtLeast(0L) else remainingMs

    val totalMs: Long get() = settings.durationMs(phase)
}

/** "25:00", or "1:30:00" once it reaches an hour (matches what a Chronometer shows). */
fun formatClock(ms: Long): String {
    val totalSec = (ms.coerceAtLeast(0L) + 999) / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
