# Essence — Private Notes, Voice Notes & To-Dos

Essence is a native Android application designed with the minimalist, monochrome aesthetic of **Nothing OS / Nothing Essential Space**. It operates entirely offline and locally by default, prioritizing privacy, tactile haptics, and system-level entry points.

---

## Key Features

1. **Nothing OS Design System**
   - Pure black (`#000000`) background, subtle rounded cards, thin borders, and Nothing Red (`#D71921`) accent.
   - Dual-font typography: **Doto** (dot-matrix) for headlines, timestamps, and numbers paired with **Space Mono** for body copy.
   - Tactile haptic feedback on every interactive touchpoint.
   - Bottom navigation across Notes, To-Dos, Voice, and Settings.

2. **Notes & Checklists**
   - Notes with rich checklists, customizable folders, tags, pinning, archive, and trash with restore.
   - Full-text search spanning titles, bodies, tags, and speech transcripts.

3. **Voice Recording & Transcription**
   - Screen-off background recording powered by a foreground service with `foregroundServiceType="microphone"`.
   - Ongoing recording notification with interactive Pause, Resume, and Stop controls.
   - High-fidelity AAC (`.m4a`) audio storage in app-private storage.
   - Playback player with seek bar, variable playback speed (`1x`, `1.25x`, `1.5x`, `2x`), and interactive transcript scrubbing.
   - **Switchable Speech-to-Text Providers**:
     - **On-Device (Default)**: Fully offline and private using Android SpeechRecognizer.
     - **Custom Endpoint**: Connect to any OpenAI-compatible API (self-hosted Whisper, faster-whisper, LocalAI, Ollama, Groq, OpenAI). Includes an endpoint connectivity test.
   - Hardware-backed Keystore encryption (`AES/GCM`) for API keys.
   - **Intelligent Actions**: Summarize notes and extract actionable to-dos with one tap.

4. **To-Dos & Reminders**
   - Priority levels (P1 Urgent/Red, P2 High, P3 Medium, P4 Low), subtasks, and project tags.
   - Repeating schedules: Daily, Weekdays, Weekly, Monthly, Yearly.
   - Exact alarms via `AlarmManager` with high-priority notifications, "Done", and "Snooze 10m" actions.
   - Automatic alarm rescheduling on device reboot via `BOOT_COMPLETED`.

5. **Entry Points**
   - **Home-Screen Widgets**: 2x2 and 4x2 Nothing-styled RemoteViews widgets featuring an instant red record button (launches a transparent trigger activity compliant with Android 14 microphone FGS policies) plus New Note and New Task shortcuts.
   - **Quick Settings Tile**: "Quick Record" tile for instant one-tap audio capture from anywhere in the OS.
   - **App Shortcuts**: Long-press app icon for Quick Record, New Note, and New Task.

6. **Local Backup & Export**
   - Full export/import to a single `.zip` file containing `backup.json` and all `.m4a` audio recordings using the Android Storage Access Framework.

---

## How to Build & Install

### Prerequisites
- JDK 17 or higher
- Android SDK with platform tools (`adb`)

### Build Debug APK
```bash
gradle :app:assembleDebug
```
The resulting APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Install via ADB
Connect your phone with USB Debugging enabled:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Launch Main Activity
```bash
adb shell am start -n com.aistudio.essence.ntgspc/com.example.MainActivity
```

---

## Xiaomi HyperOS / MIUI Hardening Guide

Xiaomi's HyperOS and MIUI aggressively kill background processes and restrict exact alarms and microphone foreground services by default. Follow this checklist to ensure 100% reliable operation:

### 1. Battery Saver: Set to "No Restrictions"
1. Open phone **Settings** > **Apps** > **Manage apps** > **Essence**.
2. Tap **Battery saver**.
3. Select **No restrictions** (instead of "Battery saver (recommended)").

### 2. Enable Autostart
1. In **Settings** > **Apps** > **Manage apps** > **Essence** (or **Security** > **Permissions** > **Autostart**).
2. Toggle **Autostart** to **ON**.
3. If prompted with a dialog, allow background autostart.

### 3. Grant Additional Background Permissions
1. Under **Essence** > **Other permissions**:
   - Enable **Show on Lock screen**.
   - Enable **Display pop-up windows while running in the background**.
   - Enable **Permanent notification**.

### 4. Lock Essence in the Recents Screen
1. Open Essence, then open the system **Recents** screen (swipe up and hold).
2. Press and hold the **Essence** app preview card until action icons appear.
3. Tap the **Padlock** icon so a lock badge appears on the card. This stops the MIUI memory cleaner from killing the background recording service.

### 5. Disable Battery Optimization (Doze Mode)
1. In Essence, go to **Settings** > **System & OEM Hardening**.
2. Tap **Run Hardening Checklist** > **Battery Optimization** and select **Don't optimize / Allow**.
