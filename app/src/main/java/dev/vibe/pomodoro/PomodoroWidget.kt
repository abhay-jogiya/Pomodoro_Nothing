package dev.vibe.pomodoro

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import kotlin.math.roundToInt

class PomodoroWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val s = TimerEngine.load(context)
        for (id in appWidgetIds) {
            manager.updateAppWidget(id, build(context, s, manager.getAppWidgetOptions(id)))
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        manager.updateAppWidget(appWidgetId, build(context, TimerEngine.load(context), newOptions))
    }

    companion object {
        private val RED = Color.parseColor("#FFD71921")
        private val WHITE = Color.WHITE

        fun updateAll(c: Context, s: TimerState = TimerEngine.load(c)) {
            val mgr = AppWidgetManager.getInstance(c)
            val ids = mgr.getAppWidgetIds(ComponentName(c, PomodoroWidget::class.java))
            for (id in ids) {
                mgr.updateAppWidget(id, build(c, s, mgr.getAppWidgetOptions(id)))
            }
        }

        /** Ask the launcher to pin a widget (works on Nothing Launcher / Pixel-style launchers). */
        fun requestPin(c: Context): Boolean {
            val mgr = AppWidgetManager.getInstance(c)
            if (!mgr.isRequestPinAppWidgetSupported) return false
            return mgr.requestPinAppWidget(ComponentName(c, PomodoroWidget::class.java), null, null)
        }

        private fun isCompact(options: Bundle?): Boolean {
            val minWidthDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250) ?: 250
            return minWidthDp < 240
        }

        fun build(c: Context, s: TimerState, options: Bundle?): RemoteViews {
            val layout = if (isCompact(options)) R.layout.widget_pomodoro_compact else R.layout.widget_pomodoro
            val rv = RemoteViews(c.packageName, layout)

            // Translucent glass: the background is a rounded-rect ImageView whose alpha we set.
            val alpha = (s.settings.widgetOpacity.coerceIn(5, 100) / 100f * 255f).roundToInt()
            rv.setInt(R.id.widget_bg, "setImageAlpha", alpha)

            rv.setTextViewText(R.id.widget_phase, s.phase.label)
            rv.setTextColor(R.id.widget_phase, if (s.phase == Phase.FOCUS) RED else WHITE)
            rv.setTextViewText(R.id.widget_dots, dots(s))

            if (s.running) {
                // Native countdown: the system ticks this, no widget updates needed.
                val base = SystemClock.elapsedRealtime() + s.remaining()
                rv.setChronometerCountDown(R.id.widget_time_live, true)
                rv.setChronometer(R.id.widget_time_live, base, null, true)
                rv.setViewVisibility(R.id.widget_time_live, View.VISIBLE)
                rv.setViewVisibility(R.id.widget_time_static, View.GONE)
            } else {
                rv.setChronometer(R.id.widget_time_live, SystemClock.elapsedRealtime(), null, false)
                rv.setViewVisibility(R.id.widget_time_live, View.GONE)
                rv.setViewVisibility(R.id.widget_time_static, View.VISIBLE)
                rv.setTextViewText(R.id.widget_time_static, formatClock(s.remainingMs))
            }

            rv.setImageViewResource(
                R.id.widget_toggle,
                if (s.running) R.drawable.ic_pause else R.drawable.ic_play,
            )

            rv.setOnClickPendingIntent(R.id.widget_root, Intents.openApp(c))
            rv.setOnClickPendingIntent(R.id.widget_toggle, Intents.action(c, TimerEngine.ACTION_TOGGLE))
            rv.setOnClickPendingIntent(R.id.widget_reset, Intents.action(c, TimerEngine.ACTION_RESET))
            rv.setOnClickPendingIntent(R.id.widget_skip, Intents.action(c, TimerEngine.ACTION_SKIP))
            return rv
        }

        private fun dots(s: TimerState): String {
            val total = s.settings.sessionsUntilLong.coerceIn(1, 8)
            val done = s.focusDone.coerceIn(0, total)
            return (0 until total).joinToString(" ") { if (it < done) "●" else "○" }
        }
    }
}
