# Personal AI Radar — project status

_Last updated: 2026-06-04_

## Repository

- GitHub: `balakleets4-max/personal-ai-radar`
- Product: **Personal AI Radar / Личный ИИ-Радар**
- Platform: Android
- Current development rule: keep v0.1 small, but keep every included block solid, testable, and extensible. Do not add large new features before critical stability bugs are fixed.

## Product idea

Personal AI Radar is an offline-first Android assistant / second memory. It captures everyday thoughts, tasks, promises, ideas, risks, dates, and small life signals, then turns them into useful Radar cards.

Core loop:

```text
Capture → Analysis → RadarCard → Action → Memory Update → Reflection
```

## Current architecture snapshot

Main planned/active modules:

- `ui`
- `viewmodel`
- `domain`
- `data`
- `parser`
- `radar`
- `notifications`
- `permissions`
- `settings`
- `testing`

Important v0.1 concepts:

- Capture / Захват памяти: raw user input such as a thought, task, promise, reminder, idea, risk, work note, or plan.
- RadarCard types: Task, Reminder, Idea, Promise, Note, Risk.
- Card statuses: `new`, `needs_review`, `active`, `done`, `snoozed`, `archived`, `deleted`.
- Basic language/date/action/person/topic parsing.
- Confidence and fallback logic when parsing is uncertain.

## Build and CI status

### Working

- `Android Build` GitHub Actions workflow works and produces a debug APK.
- `Android Emulator Smoke Test` GitHub Actions workflow works when the app launches cleanly.
- The smoke test builds the APK, launches an Android emulator in GitHub Actions, installs the APK, opens the app, checks that the app process is alive, captures `logcat`, and saves a screenshot/artifacts.
- This gives us a free automatic guard against the situation: APK builds successfully but the app immediately fails to launch.

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

## Firebase Test Lab status

Firebase Test Lab Robo workflow was attempted and mostly configured, but is currently kept as **manual only**.

Current status:

- Cloud Testing API was enabled.
- Cloud Tool Results API was enabled.
- Service account roles were configured.
- Device selection logic was made dynamic.
- The workflow reached the Firebase Test Lab upload/start stage.
- The attempt got blocked by Google Cloud Storage / billing / free trial limitations because Test Lab needed to stage the APK/results in Cloud Storage.
- The user's payment card was rejected by Google Cloud Free Trial because prepaid cards are not supported.

Decision:

- Keep `Firebase Test Lab Robo` as a manual workflow for later.
- Do not let it run automatically on every push.
- Use `Android Emulator Smoke Test` as the current free automated launch test.

## Important recent infrastructure commits

- `50a0fe5` — Limit Firebase Test Lab workflow to manual runs
- `7c1f661` — Add Android emulator smoke test workflow
- `42d9237` — Use stable emulator profile for smoke test
- `3fd5125` — Add emulator smoke test script
- `eff6b9c` — Run emulator smoke script from file
- `f8417a2` — Avoid false positive smoke crash detection

## Important recent app/frontend commits

- `8269b7d` — Remove crashing adaptive round launcher icon
  - Smoke Test #30 on `781f4fc` still failed with `Resources$NotFoundException` for `mipmap/ic_launcher_round`.
  - The adaptive icon XML path is now treated as unsafe for this app/runtime path.
  - Removed `src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` so Android falls back to density-specific launcher resources.
  - Status: committed to `main`, needs CI rerun.

- `4e4cca8` — Remove crashing adaptive launcher icon
  - Removed `src/main/res/mipmap-anydpi-v26/ic_launcher.xml` for the same reason: adaptive icon XML caused runtime launch crashes.
  - App should now use density-specific `mipmap-mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi` icon resources.
  - Status: committed to `main`, needs CI rerun.

- `781f4fc` — Use drawable foreground for round launcher icon
  - Attempted to fix Smoke Test #30 by replacing `@android:color/transparent` with `@drawable/ic_launcher_transparent_foreground`.
  - Result: still failed in the uploaded logs; superseded by removing adaptive icon XML resources.

- `c1cf3ce` — Use drawable foreground for adaptive launcher icon
  - Attempted to use `@drawable/ic_launcher_transparent_foreground` in `ic_launcher.xml`.
  - Superseded by `4e4cca8`.

- `4e73a0a` — Add transparent launcher foreground drawable
  - Added `src/main/res/drawable/ic_launcher_transparent_foreground.xml`.
  - Superseded for launcher routing by deleting adaptive icon XML resources.

- `72533e6` — Fix launcher icon resource routing
  - Build 254 / Android Build #281 showed that the previous launcher icon fix was not enough.
  - Restored `android:roundIcon="@mipmap/ic_launcher_round"` in `src/main/AndroidManifest.xml`.
  - Routed both adaptive launcher resources to `@drawable/ic_launcher_ai_radar` with transparent foreground.
  - Added density-specific `mipmap-mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi` XML launcher resources so fallback launcher paths also resolve to the AI Radar image.
  - Status: superseded because Smoke Test #30 found adaptive icon XML caused a runtime crash.

- `94c418b` — Update launcher icon resources
  - Added `src/main/res/drawable-nodpi/ic_launcher_ai_radar.webp` from the user's AI Radar lighthouse concept artwork.
  - Previous status: insufficient after Build 254 / Android Build #281 because APK still did not visually match the intended original icon.
  - Do not mark launcher icon fixed until it visually matches the user's original image after clean install.

## Important recent backend commits

- `58fef86` — Fix build 254 Russian date parser.
- `8ff7197` — Support spoken dates and date ranges.
- `df1f2b3` — Add build 254 range and event date tests.
- `c5071c1` — Keep radar card due date id non-null / fix Room query issue.
- `e86d168` — Expand calendar sync window and suppress holiday noise.
- `d712dd6` — Clamp calendar sync window to 30 days.

## Last known app testing context

### Android Emulator Smoke Test #30 observations

Frontend/UI:

- Smoke Test #30 failed on `781f4fc`.
- APK built successfully, installed successfully, and launch command was issued.
- App process exited after deterministic `MainActivity` launch.
- Logcat root cause:
  - `Resources$NotFoundException: Drawable com.personalradar.app:mipmap/ic_launcher_round`
  - `XmlPullParserException: <foreground> or <background> tag requires a 'drawable' attribute or child tag defining a drawable`
- Root problem: adaptive launcher icon XML is unsafe in the current app/runtime path because Activity startup tries to load the launcher icon as an action bar/default icon and crashes on the adaptive XML.
- New mitigation: remove `mipmap-anydpi-v26/ic_launcher.xml` and `mipmap-anydpi-v26/ic_launcher_round.xml` so Android falls back to density-specific non-adaptive launcher resources.
- Required next check: rerun Android Emulator Smoke Test and Android Build after `8269b7d`.

### Build 254 / Android Build #281 observations

Frontend/UI:

- Launcher icon bug is **not fixed** in Build 254 / Android Build #281.
- The APK icon still did not match the user's original AI Radar concept image: lighthouse + glowing road / pulse line / “Линия спасения” concept.
- Previous frontend fix `94c418b` is considered insufficient.
- Verification still required: build fresh APK, uninstall old app, install fresh APK, and compare launcher / app list / system app info icon visually against the user's original.

### Build 254 / Android Build #272 observations

Frontend/UI:

- Launcher icon mismatch was reported: the app icon on the Android launcher did not match the AI Radar icon concept sent by the user.
- Fix committed in `94c418b`, but later testing in Build #281 showed it was not enough.

Backend / CalendarSync:

- Google Calendar event `День петуха тест 1` on 30 June 21:30–22:30 did not appear in AI Radar when the phone date was 4 June.
- Cause found: older manual import path still used a 14-day sync window while CalendarSourceReader was being moved toward a wider window.
- Current backend behavior after latest fixes: CalendarSourceReader enforces at least a 30-day window and a limit of at least 120 items.
- All-day holiday noise such as `День России` is filtered in CalendarSourceReader.
- Google Tasks / Задачи are not supported by this CalendarContract-based module yet and should be treated as unsupported, not as a calendar sync bug.

Backend / DateTime:

- Text `съездить к родителям на дачу 26 августа` works.
- Voice-style date `съездить к родителям на дачу двадцать шестого августа` works after parser fixes.
- Relative date `съездить к родителям через месяц` now uses a sane default time, expected `09:00`.
- `день влюбленных в час дня` recognizes `13:00`, but holiday/event knowledge is not yet a real feature. Do not claim known-event dates unless a clear event database exists.
- Date ranges are still fragile and should move toward a proper Temporal Engine instead of more one-off regex patches.

### Build 200 observations

Working:

- APK installed and launched.
- Diagnostics / permissions screen worked.
- Calendar import worked in the tested foreground/open-app scenario.
- Voice capture worked.
- Similar-note search worked.
- AI Resolution update/reschedule flow worked in basic tests.
- Calendar delete lifecycle worked for the tested case: create event in Google Calendar → import into AI Radar → delete in Google Calendar → card disappears from AI Radar after refresh/reopen.

Not proven yet:

- Reliable background calendar sync after process kill, reboot, or long device sleep.

Known issues carried forward:

1. **Launcher icon still needs visual verification**
   - Build #281 proved the previous fix was not sufficient.
   - Smoke Test #30 proved adaptive icon XML can crash the app during startup.
   - Do not close this bug until a fresh APK after `8269b7d` launches successfully and visually matches the user's original AI Radar icon.

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
P1: Rerun Android Emulator Smoke Test after `8269b7d`.
P1: Run Android Build after `8269b7d` and verify the APK.
P1: Test launcher icon after clean install on a phone: launcher, app list, and system app info.
P1: Test CalendarSync with a user event 26 days ahead, e.g. “День петуха тест 1” on 30 June.
P1: Design a real TemporalResult / TemporalEngine instead of adding more regex patches.
```

Acceptance criteria for launcher icon fix:

- Fresh APK builds successfully after `8269b7d`.
- Android Emulator Smoke Test is green after `8269b7d`.
- Old app is uninstalled before test install.
- New APK is installed cleanly.
- Android launcher shows the AI Radar lighthouse icon from the user's original concept image.
- App list shows the same AI Radar icon.
- System app info shows the same AI Radar icon.
- No old vector launcher icon or fallback/safe placeholder is visible.
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
