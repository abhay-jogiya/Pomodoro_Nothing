# Pomodoro (Nothing-style) + translucent home-screen widget

Native Android (Kotlin + Jetpack Compose). Built for Nothing Phone (2a) on Nothing OS 5.0 Beta (Android 16), minSdk 26 / targetSdk 35.

- **App**: dotted countdown ring, play/pause, reset, skip, focus / short / long break lengths, sessions per cycle, auto-start, widget opacity.
- **Widget**: translucent glass (opacity set in the app), live countdown that ticks on its own, play/pause, reset and skip buttons. Switches between a wide (3-4 cells) and compact (2 cells) layout when you resize it.
- **Alerts**: an exact alarm ends each phase with a notification (works with the screen off), plus an ongoing countdown notification with Pause / Skip while running. Survives reboot.

## Get an APK (no Android Studio needed)

1. Create a new GitHub repo and push this folder to it (`main` branch).
2. Open the repo's **Actions** tab, open **Build APK**, and wait for the run to finish (about 3-5 minutes). You can also start it with **Run workflow**.
3. Download the `pomodoro-debug-apk` artifact, unzip it, and open `app-debug.apk` on your phone (allow "Install unknown apps" for your browser or Files app when asked).

## Or build locally

Open the folder in Android Studio and press Run with your phone connected (USB debugging on), or from a terminal with JDK 17 and the Android SDK:

```
gradle assembleDebug
```

(Android Studio will download Gradle 8.11.1 for you. `gradle-wrapper.jar` is not included, so run `gradle wrapper` once if you want `./gradlew`.)

## Add the widget

Open the app and tap **Add widget to home screen**, or long-press the home screen, then Widgets, then Pomodoro.

## Nothing OS tips

- Allow notifications when asked on first launch.
- For reliable alerts with the screen off, set Settings > Apps > Pomodoro > Battery to **No restrictions** (Nothing OS can otherwise delay alarms).
- The widget's opacity slider is in the app. 5% is almost invisible glass, 100% is solid black.

## Layout

```
app/src/main/java/dev/vibe/pomodoro/
  Model.kt            phases, settings, state
  TimerEngine.kt      state + alarm; everything routes through here
  PomodoroWidget.kt   RemoteViews builder (Chronometer countdown)
  Notifier.kt         running + finished notifications
  Receivers.kt        alarm, widget/notification actions, boot
  MainActivity.kt     host + 200 ms sampling while visible
  PomodoroScreen.kt   Compose UI
app/src/main/res/     layouts (wide + compact widget), icons, widget info
```
