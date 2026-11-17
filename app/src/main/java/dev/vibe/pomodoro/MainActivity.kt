package dev.vibe.pomodoro

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** What the UI renders: the stored state plus the time left at the moment it was sampled. */
data class Snapshot(val state: TimerState, val remainingMs: Long)

class Actions(
    val toggle: () -> Unit,
    val reset: () -> Unit,
    val skip: () -> Unit,
    val setSettings: (Settings) -> Unit,
    val addWidget: () -> Unit,
)

class MainActivity : ComponentActivity() {

    private lateinit var snap: MutableState<Snapshot>

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* nothing to do */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        snap = mutableStateOf(sample())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Re-sample the stored state while visible. Pausing when the app is in the
        // background means zero battery cost; the widget keeps itself ticking.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                TimerEngine.refresh(this@MainActivity)
                while (true) {
                    snap.value = sample()
                    delay(200)
                }
            }
        }

        val actions = Actions(
            toggle = { TimerEngine.toggle(this); snap.value = sample() },
            reset = { TimerEngine.reset(this); snap.value = sample() },
            skip = { TimerEngine.skip(this); snap.value = sample() },
            setSettings = { TimerEngine.updateSettings(this, it); snap.value = sample() },
            addWidget = {
                if (!PomodoroWidget.requestPin(this)) {
                    Toast.makeText(
                        this,
                        "Long-press your home screen → Widgets → Pomodoro",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
        )

        setContent {
            PomodoroTheme {
                PomodoroScreen(snap.value, actions)
            }
        }
    }

    private fun sample(): Snapshot {
        val s = TimerEngine.load(this)
        return Snapshot(s, s.remaining())
    }
}
