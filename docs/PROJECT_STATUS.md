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
- `Android Emulator Smoke Test` GitHub Actions workflow works and is green.
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

- `94c418b` — Update launcher icon resources
  - Replaced the adaptive launcher icon source with the user's AI Radar lighthouse concept artwork.
  - Added `src/main/res/drawable-nodpi/ic_launcher_ai_radar.webp`.
  - Updated `src/main/res/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` to use the new artwork.
  - Status: committed to `main`, needs APK build / clean-install verification on a phone.

## Last known app testing context

### Build 254 / Android Build #272 observations

Frontend/UI:

- Launcher icon mismatch was reported: the app icon on the Android launcher did not match the AI Radar icon concept sent by the user.
- Fix committed in `94c418b`.
- Verification still needed: build fresh APK, uninstall the old app, install the fresh APK, and confirm the launcher icon changed on the phone.

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

Known issues:

1. **Date parsing bug: “26 августа”**
   - Phrase: `съездить к родителям на дачу 26 августа`
   - Expected: card should receive a date for 26 August with month/year resolved.
   - Actual in build 200: `26` was lost, month/year were not assigned correctly, and the card was created without a proper date.
   - Current priority: **P1**.

2. **Odd default time on reschedule**
   - Phrase sequence included `съездить к родителям завтра`, then `съездить к родителям через месяц`.
   - AI Resolution correctly suggested update_existing and avoided duplicates.
   - But after replacing, the resulting date used a strange/current-like time instead of expected default `09:00`.

3. **Calendar noise**
   - All-day holiday `День России` remains as an active Radar card with priority 4.
   - Needs filtering or lower-priority handling later.

## Current next priority

Verify the frontend launcher icon fix, then return to app logic.

Recommended next tasks:

```text
P1: Verify launcher icon fix after clean APK install.
P1: Fix Russian date parsing for “26 августа” in captures like:
“съездить к родителям на дачу 26 августа”
```

Acceptance criteria for launcher icon fix:

- Fresh APK builds successfully after `94c418b`.
- Old app is uninstalled before test install.
- New APK is installed cleanly.
- Android launcher shows the AI Radar lighthouse icon from the user's concept image.
- No old vector launcher icon is visible.

Acceptance criteria for date parsing fix:

- The parser detects day `26` and month `августа` as one date expression.
- The year is resolved consistently using the current date/context.
- The resulting RadarCard has a valid due date.
- The fix should not break existing relative date parsing such as `завтра` and `через месяц`.
- Add or update tests for this phrase.

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
