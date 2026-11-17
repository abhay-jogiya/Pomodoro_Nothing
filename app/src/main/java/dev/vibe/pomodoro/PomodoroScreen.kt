package dev.vibe.pomodoro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

private val Red = Color(0xFFD71921)
private val Ink = Color(0xFF000000)
private val Panel = Color(0xFF111111)
private val Chip = Color(0xFF1E1E1E)
private val Unlit = Color(0xFF2A2A2A)
private val Dim = Color(0xFF8A8A8A)
private val Mono = FontFamily.Monospace

@Composable
fun PomodoroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Red,
            onPrimary = Color.White,
            background = Ink,
            surface = Panel,
            onSurface = Color.White,
            onBackground = Color.White,
        ),
        content = content,
    )
}

@Composable
fun PomodoroScreen(snap: Snapshot, a: Actions) {
    val s = snap.state
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Red))
            Spacer(Modifier.width(10.dp))
            Text(
                "POMODORO",
                color = Color.White,
                fontFamily = Mono,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 4.sp,
            )
        }

        Spacer(Modifier.height(20.dp))
        DotRing(snap)
        Spacer(Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundButton(size = 56.dp, container = Chip, onClick = a.reset, icon = R.drawable.ic_reset, desc = "Reset")
            RoundButton(
                size = 84.dp,
                container = Red,
                onClick = a.toggle,
                icon = if (s.running) R.drawable.ic_pause else R.drawable.ic_play,
                desc = if (s.running) "Pause" else "Start",
                iconSize = 38.dp,
            )
            RoundButton(size = 56.dp, container = Chip, onClick = a.skip, icon = R.drawable.ic_skip, desc = "Skip")
        }

        Spacer(Modifier.height(32.dp))
        SettingsCard(s.settings, a)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DotRing(snap: Snapshot) {
    val s = snap.state
    val total = s.totalMs.coerceAtLeast(1L)
    val elapsed = (1f - snap.remainingMs.toFloat() / total).coerceIn(0f, 1f)
    val accent = if (s.phase == Phase.FOCUS) Red else Color.White
    val count = 60

    Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f - 8.dp.toPx()
            val c = Offset(size.width / 2f, size.height / 2f)
            val off = floor(elapsed * count).toInt()
            for (i in 0 until count) {
                val angle = Math.toRadians(i * 360.0 / count - 90.0)
                val p = Offset(
                    c.x + (r * cos(angle)).toFloat(),
                    c.y + (r * sin(angle)).toFloat(),
                )
                val lit = i >= off
                drawCircle(
                    color = if (lit) accent else Unlit,
                    radius = if (lit) 4.dp.toPx() else 2.5.dp.toPx(),
                    center = p,
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                s.phase.label,
                color = accent,
                fontFamily = Mono,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 3.sp,
            )
            Text(
                formatClock(snap.remainingMs),
                color = Color.White,
                fontFamily = Mono,
                fontWeight = FontWeight.Bold,
                fontSize = 60.sp,
            )
            Spacer(Modifier.height(6.dp))
            SessionDots(s, accent)
        }
    }
}

@Composable
private fun SessionDots(s: TimerState, accent: Color) {
    val total = s.settings.sessionsUntilLong.coerceIn(1, 8)
    val done = s.focusDone.coerceIn(0, total)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 0 until total) {
            Box(
                Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(if (i < done) accent else Unlit),
            )
        }
    }
}

@Composable
private fun RoundButton(
    size: androidx.compose.ui.unit.Dp,
    container: Color,
    onClick: () -> Unit,
    icon: Int,
    desc: String,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(container)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = desc,
            tint = Color.White,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun SettingsCard(st: Settings, a: Actions) {
    var opacity by remember(st.widgetOpacity) { mutableFloatStateOf(st.widgetOpacity.toFloat()) }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Panel)
            .padding(20.dp),
    ) {
        Text(
            "SETTINGS",
            color = Dim,
            fontFamily = Mono,
            fontSize = 11.sp,
            letterSpacing = 3.sp,
        )
        Spacer(Modifier.height(8.dp))

        Stepper("Focus", st.focusMin, 1..90, " min") { a.setSettings(st.copy(focusMin = it)) }
        Stepper("Short break", st.shortMin, 1..30, " min") { a.setSettings(st.copy(shortMin = it)) }
        Stepper("Long break", st.longMin, 1..60, " min") { a.setSettings(st.copy(longMin = it)) }
        Stepper("Sessions per cycle", st.sessionsUntilLong, 2..8, "") {
            a.setSettings(st.copy(sessionsUntilLong = it))
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Auto-start next phase", color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Switch(
                checked = st.autoStart,
                onCheckedChange = { a.setSettings(st.copy(autoStart = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Red,
                    uncheckedThumbColor = Dim,
                    uncheckedTrackColor = Chip,
                    uncheckedBorderColor = Color(0xFF333333),
                ),
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Widget opacity", color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(
                "${opacity.toInt()}%",
                color = Dim,
                fontFamily = Mono,
                fontSize = 14.sp,
                textAlign = TextAlign.End,
            )
        }
        Slider(
            value = opacity,
            onValueChange = { opacity = it },
            onValueChangeFinished = { a.setSettings(st.copy(widgetOpacity = opacity.toInt())) },
            valueRange = 5f..100f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Red,
                inactiveTrackColor = Unlit,
            ),
        )

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = a.addWidget,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Chip, contentColor = Color.White),
        ) {
            Text("Add widget to home screen", fontFamily = Mono, fontSize = 13.sp)
        }
    }
}

@Composable
private fun Stepper(
    label: String,
    value: Int,
    range: IntRange,
    suffix: String,
    onChange: (Int) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
        StepButton("−", enabled = value > range.first) { onChange(value - 1) }
        Text(
            "$value$suffix",
            color = Color.White,
            fontFamily = Mono,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(64.dp),
        )
        StepButton("+", enabled = value < range.last) { onChange(value + 1) }
    }
}

@Composable
private fun StepButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Chip)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol,
            color = if (enabled) Color.White else Color(0xFF4A4A4A),
            fontFamily = Mono,
            fontSize = 18.sp,
        )
    }
}
