package dev.vibe.pomodoro

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Date

object Notifier {
    private const val CH_RUNNING = "pomodoro_running"
    private const val CH_DONE = "pomodoro_done"
    private const val ID_RUNNING = 1
    private const val ID_DONE = 2

    private fun ensureChannels(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CH_RUNNING, "Timer running", NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
                description = "Ongoing countdown while a session is running"
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_DONE, "Timer finished", NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                description = "Alerts when a focus session or break ends"
            },
        )
    }

    /** Ongoing countdown notification while running; removed when paused/idle. */
    @SuppressLint("MissingPermission")
    fun updateRunning(c: Context, s: TimerState) {
        val nm = NotificationManagerCompat.from(c)
        if (!s.running) {
            nm.cancel(ID_RUNNING)
            return
        }
        if (!nm.areNotificationsEnabled()) return
        ensureChannels(c)
        val endsAt = DateFormat.getTimeFormat(c).format(Date(s.endAt))
        val n = NotificationCompat.Builder(c, CH_RUNNING)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(s.phase.title)
            .setContentText("Ends at $endsAt")
            .setWhen(s.endAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(Intents.openApp(c))
            .addAction(0, "Pause", Intents.action(c, TimerEngine.ACTION_PAUSE))
            .addAction(0, "Skip", Intents.action(c, TimerEngine.ACTION_SKIP))
            .build()
        try {
            nm.notify(ID_RUNNING, n)
        } catch (_: SecurityException) {
        }
    }

    @SuppressLint("MissingPermission")
    fun notifyFinished(c: Context, finished: Phase, next: Phase, autoStarted: Boolean) {
        val nm = NotificationManagerCompat.from(c)
        if (!nm.areNotificationsEnabled()) return
        ensureChannels(c)
        val title = if (finished == Phase.FOCUS) "Focus complete" else "Break over"
        val text = when {
            autoStarted -> "${next.title} started."
            next == Phase.FOCUS -> "Ready to focus?"
            else -> "Time for a ${next.title.lowercase()}."
        }
        val b = NotificationCompat.Builder(c, CH_DONE)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(Intents.openApp(c))
        if (!autoStarted) {
            b.addAction(0, "Start ${next.title.lowercase()}", Intents.action(c, TimerEngine.ACTION_START))
        }
        try {
            nm.notify(ID_DONE, b.build())
        } catch (_: SecurityException) {
        }
    }

    fun cancelFinished(c: Context) {
        NotificationManagerCompat.from(c).cancel(ID_DONE)
    }
}
