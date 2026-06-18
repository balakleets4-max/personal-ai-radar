# Personal AI Radar — project status

_Last updated: 2026-06-04_

## Repository

- GitHub: `balakleets4-max/personal-ai-radar`
- Product: **Personal AI Radar / Личный ИИ-Радар**
- Platform: Android
- Current development rule: keep v0.1 small, but every included block must be solid, testable, and extensible. Do not add large new features before critical stability bugs are fixed.

## Product idea

Personal AI Radar is an offline-first Android assistant / second memory. It captures everyday thoughts, tasks, promises, ideas, risks, dates, and small life signals, then turns them into useful Radar cards.

Core loop:

```text
Capture → Analysis → RadarCard → Action → Memory Update → Reflection
```

## Current architecture snapshot

Main planned/active modules: `ui`, `viewmodel`, `domain`, `data`, `parser`, `radar`, `notifications`, `permissions`, `settings`, `testing`.

Important v0.1 concepts:

- Capture / Захват памяти: raw user input such as a thought, task, promise, reminder, idea, risk, work note, or plan.
- RadarCard types: Task, Reminder, Idea, Promise, Note, Risk.
- Card statuses: `new`, `needs_review`, `active`, `done`, `snoozed`, `archived`, `deleted`.
- Basic language/date/action/person/topic parsing.
- Confidence and fallback logic when parsing is uncertain.

## Build and CI status

### Working infrastructure

- `Android Build` GitHub Actions workflow builds and uploads a debug APK.
- `Android Emulator Smoke Test` builds the APK, launches an emulator, installs the APK, opens the app, checks the process, captures `logcat`, and uploads artifacts.
- Smoke test is the current free automated guard against APK-builds-but-app-does-not-launch failures.
- Firebase Test Lab Robo workflow is configured only as **manual** because automatic use is blocked by Google Cloud Storage / billing / card limitations.

### Current useful artifacts

From `Android Emulator Smoke Test`:

- `android-emulator-smoke-results`
  - `environment.txt`
  - `app.pid`
  - `logcat.txt`
  - `smoke-screenshot.png`
  - `summary.txt` when successful
  - `failure.txt` / `crash-snippet.txt` when failed

From `Android Build`:

- `personal-ai-radar-debug-apk`

## Important recent frontend/UI commits

- `95f5e0a` — Rebuild launcher icon assets
  - Current launcher icon asset fix after user confirmed that APK after `7e9ef34` launches but still does not show the original AI Radar icon.
  - Restored manifest routing to `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`.
  - Replaced `src/main/res/drawable-nodpi/ic_launcher_ai_radar.webp` with an asset generated from the original user image.
  - Added real image launcher assets for `mipmap-mdpi`, `mipmap-hdpi`, `mipmap-xhdpi`, `mipmap-xxhdpi`, and `mipmap-xxxhdpi` instead of unsafe `<bitmap>` XML aliases.
  - Added valid adaptive icon XML for Android 8+ using `@color/ic_launcher_background` and `@mipmap/ic_launcher_foreground`.
  - Removed density-specific launcher XML aliases that previously crashed with `XmlPullParserException: <bitmap> requires a valid 'src' attribute`.
  - Status: committed to `main`, needs Android Build + Emulator Smoke Test + clean phone install verification.

- `7e9ef34` — Route launcher icon directly to drawable
  - Fixed crash route enough that the app launched, but the visual icon was still wrong after user APK testing.
  - Superseded by `95f5e0a`.

- `8269b7d` — Remove crashing adaptive round launcher icon
  - Removed `src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`.
  - Status: insufficient; smoke logs still showed crash via density-specific `mipmap-xxhdpi-v4/ic_launcher_round.xml`.

- `4e4cca8` — Remove crashing adaptive launcher icon
  - Removed `src/main/res/mipmap-anydpi-v26/ic_launcher.xml`.
  - Status: insufficient; density-specific XML fallback still crashed.

- `781f4fc` / `c1cf3ce` / `4e73a0a` — transparent foreground attempts
  - Superseded by later launcher asset rebuild.

- `72533e6` — Fix launcher icon resource routing
  - Restored `android:roundIcon` and added density-specific XML launcher resources.
  - Status: caused/surfaced launcher icon XML crashes in smoke tests; superseded.

- `94c418b` — Update launcher icon resources
  - Added the first `ic_launcher_ai_radar.webp` attempt.
  - Status: visually insufficient; superseded.

## Important recent backend commits

- `58fef86` — Fix build 254 Russian date parser.
- `8ff7197` — Support spoken dates and date ranges.
- `df1f2b3` — Add build 254 range and event date tests.
- `c5071c1` — Keep radar card due date id non-null / fix Room query issue.
- `e86d168` — Expand calendar sync window and suppress holiday noise.
- `d712dd6` — Clamp calendar sync window to 30 days.

## Last known app testing context

### After `7e9ef34`

Frontend/UI:

- User verified APK after `7e9ef34`: app launches, so launcher-icon crash appears resolved.
- Visual bug remains: original AI Radar icon is still not displayed.
- Current response: `95f5e0a` rebuilds launcher icon assets from the original user image and restores proper mipmap/adaptive resources.

### Android Emulator Smoke Test after `8269b7d`

Frontend/UI:

- APK built successfully.
- APK installed successfully.
- App still crashed during launch.
- Logcat root cause:
  - `Resources$NotFoundException: Drawable com.personalradar.app:mipmap/ic_launcher_round`
  - `File res/mipmap-xxhdpi-v4/ic_launcher_round.xml from drawable resource ID #0x7f090001`
  - `XmlPullParserException: <bitmap> requires a valid 'src' attribute`
- Diagnosis: density-specific `mipmap-*dpi/ic_launcher_round.xml` XML files were still being packaged and loaded during startup.

### Build 254 / Android Build #281 observations

Frontend/UI:

- Launcher icon bug is **not fixed** in Build 254 / Android Build #281.
- APK icon still did not match the user's original AI Radar concept image: lighthouse + glowing road / pulse line / “Линия спасения” concept.
- Previous frontend fix `94c418b` is considered insufficient.

### Build 254 / Android Build #272 observations

Backend / CalendarSync:

- Google Calendar event `День петуха тест 1` on 30 June 21:30–22:30 did not appear in AI Radar when the phone date was 4 June.
- Cause found: older manual import path still used a 14-day sync window while CalendarSourceReader was being moved toward a wider window.
- Current backend behavior after latest fixes: CalendarSourceReader enforces at least a 30-day window and a limit of at least 120 items.
- All-day holiday noise such as `День России` is filtered in CalendarSourceReader.
- Google Tasks / Задачи are not supported by this CalendarContract-based module yet and should be treated as unsupported, not as a calendar sync bug.

Backend / DateTime:

- Text `съездить к родителям на дачу 26 августа` works.
- Voice-style date `съездить к родителям на дачу двадцать шестого августа` works after parser fixes.
- Relative date `съездить к родителям через месяц` now uses sane default time, expected `09:00`.
- `день влюбленных в час дня` recognizes `13:00`, but holiday/event knowledge is not yet a real feature. Do not claim known-event dates unless a clear event database exists.
- Date ranges are still fragile and should move toward a proper Temporal Engine instead of more one-off regex patches.

## Known issues carried forward

1. **Launcher icon still needs visual verification**
   - Build #281 proved the visual icon bug was not fixed.
   - Smoke tests proved launcher icon XML resources can crash the app during startup.
   - `7e9ef34` allowed the app to launch but did not fix the visual icon.
   - Do not close this bug until fresh APK after `95f5e0a` launches successfully and visually matches the user's original AI Radar icon.

2. **Temporal engine is not unified yet**
   - Current DateTime parsing has many local fixes.
   - Needed: one `TemporalResult` / `TemporalEngine` used by diagnostics, AI Resolution, RadarCard creation, and notification scheduling.

3. **Date ranges remain fragile**
   - Example: `день китаец с пятого мая по седьмое мая` should produce start and end dates.
   - Current implementation may not represent ranges as first-class data.

4. **Known holidays/events are not supported as a real knowledge base**
   - Example: `день влюбленных в час дня` should not automatically claim a holiday date unless an explicit event database is added.

5. **Calendar background sync not fully proven**
   - Manual/foreground import works better than background reliability.
   - Still needs testing after process kill, reboot, and long idle.

## Current next priority

Recommended next tasks:

```text
P1: Run Android Build after `95f5e0a` and verify APK artifact.
P1: Run Android Emulator Smoke Test after `95f5e0a` and confirm no launcher-icon crash.
P1: Test launcher icon after clean install on a phone: launcher, app list, and system app info.
P1: Test CalendarSync with a user event 26 days ahead, e.g. “День петуха тест 1” on 30 June.
P1: Design a real TemporalResult / TemporalEngine instead of adding more regex patches.
```

Acceptance criteria for launcher icon fix:

- Fresh APK builds successfully after `95f5e0a`.
- Android Emulator Smoke Test is green after `95f5e0a`.
- Old app is uninstalled before test install.
- New APK is installed cleanly.
- Android launcher shows the AI Radar lighthouse icon from the user's original concept image.
- App list shows the same AI Radar icon.
- System app info shows the same AI Radar icon.
- No old vector icon, safe placeholder, or broken launcher XML is visible.
- Bug is not closed unless the icon visually matches the user's original image.

Acceptance criteria for CalendarSync fix:

- Fresh APK builds successfully after `d712dd6`.
- Manual calendar import reads at least the next 30 days.
- A user timed event 26 days ahead appears in AI Radar.
- All-day holiday noise such as `День России` does not appear as an active Radar card.
- Google Tasks remain clearly unsupported by the calendar module.

Acceptance criteria for future Temporal Engine:

- One parser output object is shared by diagnostics, AI Resolution, RadarCard, and notification scheduler.
- The output includes start date/time, optional end date/time, range flag, all-day flag, confidence, source text, normalized text, missing parts, and reason.
- Manual text, voice text, local parser output, and Yandex AI result are reconciled into the same result type.

## Cross-project coordination rules

- After any completed frontend/UI, testing, architecture, APK, or app-logic work that changes project state, update this file immediately.
- When handing work to another project/chat/department, include the destination and a ready-to-copy message for that project/chat.
- Keep this file as the shared source of truth so parallel project chats do not duplicate or overwrite each other's work.

## How future AI assistants should use this file

Before making project decisions, inspect this file first:

```text
docs/PROJECT_STATUS.md
```

Use it as the current source of truth for:

- current build/test infrastructure,
- latest known stable state,
- known bugs,
- next priority,
- decisions about what not to work on yet.

If new important work is completed, update this file in the same PR/commit or immediately after.
