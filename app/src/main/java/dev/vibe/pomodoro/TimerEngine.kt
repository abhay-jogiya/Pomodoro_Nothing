package dev.vibe.pomodoro

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Single source of truth for the timer. The app UI, the widget, notification
 * actions and the alarm all go through here, so they can never disagree.
 *
 * State lives in SharedPreferences. While running we only store the wall-clock
 * end time, so nothing needs to "tick" in the background: the widget uses a
 * native countdown Chronometer and an exact alarm fires when the phase ends.
 */
object TimerEngine {
    const val ACTION_TOGGLE = "dev.vibe.pomodoro.TOGGLE"
    const val ACTION_START = "dev.vibe.pomodoro.START"
    const val ACTION_PAUSE = "dev.vibe.pomodoro.PAUSE"
    const val ACTION_RESET = "dev.vibe.pomodoro.RESET"
    const val ACTION_SKIP = "dev.vibe.pomodoro.SKIP"
    const val ACTION_FINISHED = "dev.vibe.pomodoro.FINISHED"

    private const val PREFS = "pomodoro_state"
    private const val ALARM_REQUEST_CODE = 100

    private fun prefs(c: Context) =
        c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---- persistence -------------------------------------------------------

    @Synchronized
    fun load(c: Context): TimerState {
        val p = prefs(c)
        val d = Settings()
        val settings = Settings(
            focusMin = p.getInt("focusMin", d.focusMin),
            shortMin = p.getInt("shortMin", d.shortMin),
            longMin = p.getInt("longMin", d.longMin),
            sessionsUntilLong = p.getInt("sessionsUntilLong", d.sessionsUntilLong),
            autoStart = p.getBoolean("autoStart", d.autoStart),
            widgetOpacity = p.getInt("widgetOpacity", d.widgetOpacity),
        )
        val phase = Phase.entries.getOrElse(p.getInt("phase", 0)) { Phase.FOCUS }
        val stored = p.getLong("remainingMs", -1L)
        val remaining = if (stored < 0L) settings.durationMs(phase) else stored
        return TimerState(
            phase = phase,
            running = p.getBoolean("running", false),
            endAt = p.getLong("endAt", 0L),
            remainingMs = remaining,
            focusDone = p.getInt("focusDone", 0),
            settings = settings,
        )
    }

    private fun save(c: Context, s: TimerState) {
        prefs(c).edit()
            .putInt("phase", s.phase.ordinal)
            .putBoolean("running", s.running)
            .putLong("endAt", s.endAt)
            .putLong("remainingMs", s.remainingMs)
            .putInt("focusDone", s.focusDone)
            .putInt("focusMin", s.settings.focusMin)
            .putInt("shortMin", s.settings.shortMin)
            .putInt("longMin", s.settings.longMin)
            .putInt("sessionsUntilLong", s.settings.sessionsUntilLong)
            .putBoolean("autoStart", s.settings.autoStart)
            .putInt("widgetOpacity", s.settings.widgetOpacity)
            .apply()
    }

    /** Persist, (re)schedule or cancel the alarm, then refresh widget + notification. */
    private fun commit(c: Context, s: TimerState) {
        save(c, s)
        if (s.running) scheduleAlarm(c, s.endAt) else cancelAlarm(c)
        refresh(c, s)
    }

    fun refresh(c: Context, s: TimerState = load(c)) {
        PomodoroWidget.updateAll(c, s)
        Notifier.updateRunning(c, s)
    }

    // ---- user actions ------------------------------------------------------

    @Synchronized
    fun toggle(c: Context) {
        if (load(c).running) pause(c) else start(c)
    }

    @Synchronized
    fun start(c: Context) {
        val s = load(c)
        if (s.running) return
        val rem = if (s.remainingMs <= 0L) s.settings.durationMs(s.phase) else s.remainingMs
        commit(c, s.copy(running = true, endAt = System.currentTimeMillis() + rem, remainingMs = rem))
    }

    @Synchronized
    fun pause(c: Context) {
        val s = load(c)
        if (!s.running) return
        commit(c, s.copy(running = false, remainingMs = s.remaining(), endAt = 0L))
    }

    /** Back to the full length of the current phase, paused. */
    @Synchronized
    fun reset(c: Context) {
        val s = load(c)
        commit(c, s.copy(running = false, endAt = 0L, remainingMs = s.settings.durationMs(s.phase)))
    }

    /** Jump to the next phase. A skipped focus session does not count towards the long break. */
    @Synchronized
    fun skip(c: Context) {
        val s = load(c)
        var n = nextState(s, completed = false)
        if (s.running) {
            n = n.copy(running = true, endAt = System.currentTimeMillis() + n.remainingMs)
        }
        commit(c, n)
    }

    @Synchronized
    fun updateSettings(c: Context, ns: Settings) {
        val s = load(c)
        // If the clock is untouched, let a duration change show up immediately.
        val untouched = !s.running && s.remainingMs == s.settings.durationMs(s.phase)
        commit(
            c,
            s.copy(
                settings = ns,
                remainingMs = if (untouched) ns.durationMs(s.phase) else s.remainingMs,
            ),
        )
    }

    // ---- system callbacks --------------------------------------------------

    /** Called by the alarm (and by [onBoot] when the end time passed while the phone was off). */
    @Synchronized
    fun onTimerFinished(c: Context) {
        val s = load(c)
        if (!s.running) return
        val now = System.currentTimeMillis()
        if (s.endAt - now > 3_000L) {
            // Stale alarm (e.g. clock changed). Re-arm for the real end time.
            scheduleAlarm(c, s.endAt)
            return
        }
        var n = nextState(s, completed = true)
        val autoStarted = n.settings.autoStart
        if (autoStarted) {
            n = n.copy(running = true, endAt = now + n.remainingMs)
        }
        Notifier.notifyFinished(c, finished = s.phase, next = n.phase, autoStarted = autoStarted)
        commit(c, n)
    }

    @Synchronized
    fun onBoot(c: Context) {
        val s = load(c)
        if (s.running) {
            if (s.endAt > System.currentTimeMillis()) {
                scheduleAlarm(c, s.endAt)
                refresh(c, s)
            } else {
                onTimerFinished(c)
            }
        } else {
            refresh(c, s)
        }
    }

    // ---- phase logic -------------------------------------------------------

    private fun nextState(s: TimerState, completed: Boolean): TimerState {
        var done = s.focusDone
        val next: Phase = when (s.phase) {
            Phase.FOCUS -> {
                if (completed) done += 1
                if (completed && done >= s.settings.sessionsUntilLong) Phase.LONG_BREAK
                else Phase.SHORT_BREAK
            }
            Phase.SHORT_BREAK -> Phase.FOCUS
            Phase.LONG_BREAK -> {
                done = 0
                Phase.FOCUS
            }
        }
        return s.copy(
            phase = next,
            running = false,
            endAt = 0L,
            remainingMs = s.settings.durationMs(next),
            focusDone = done,
        )
    }

    // ---- alarm -------------------------------------------------------------

    private fun alarmIntent(c: Context): PendingIntent {
        val i = Intent(c, AlarmReceiver::class.java).setAction(ACTION_FINISHED)
        return PendingIntent.getBroadcast(
            c, ALARM_REQUEST_CODE, i,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun scheduleAlarm(c: Context, at: Long) {
        val am = c.getSystemService(AlarmManager::class.java) ?: return
        val pi = alarmIntent(c)
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun cancelAlarm(c: Context) {
        val am = c.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(alarmIntent(c))
    }
}
