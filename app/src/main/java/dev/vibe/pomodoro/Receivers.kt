package dev.vibe.pomodoro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fires when a phase ends. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TimerEngine.onTimerFinished(context)
    }
}

/** Handles taps from the widget buttons and notification actions. */
class TimerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            TimerEngine.ACTION_TOGGLE -> TimerEngine.toggle(context)
            TimerEngine.ACTION_START -> {
                Notifier.cancelFinished(context)
                TimerEngine.start(context)
            }
            TimerEngine.ACTION_PAUSE -> TimerEngine.pause(context)
            TimerEngine.ACTION_RESET -> TimerEngine.reset(context)
            TimerEngine.ACTION_SKIP -> TimerEngine.skip(context)
        }
    }
}

/** Alarms don't survive a reboot, so re-arm (or finish) the timer. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> TimerEngine.onBoot(context)
        }
    }
}
