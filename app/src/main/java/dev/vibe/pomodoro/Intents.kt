package dev.vibe.pomodoro

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object Intents {
    private const val FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    fun openApp(c: Context): PendingIntent {
        val i = Intent(c, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(c, 0, i, FLAGS)
    }

    /** Broadcast to [TimerActionReceiver]; one request code per action keeps them distinct. */
    fun action(c: Context, action: String): PendingIntent {
        val i = Intent(c, TimerActionReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(c, action.hashCode(), i, FLAGS)
    }
}
