# Personal AI Radar — project status

_Last updated: 2026-06-28_

## Current verified app status

### Build 254 / version 0.1.5 — OnePlus 15

Manual phone verification completed on **OnePlus 15** after the latest launcher icon / splash fixes.

Verified result:

- Clean install succeeded.
- App launches without crash.
- Main screen opens normally.
- Splash transition looks normal.
- Large blurry intermediate splash icon is not visible.
- Empty dark splash screen is not visible.
- First launch does not immediately send the user to Android system settings.
- Main screen text is readable.
- Launcher icon was checked on the home screen and in Android app info.
- Old placeholder icon is gone.
- Text inside the launcher icon is gone.

Decision:

- **Launcher icon / splash / first launch UX is confirmed on OnePlus 15.**
- Build 254 / version 0.1.5 can be treated as verified for this UI/launch block on OnePlus 15.
- Do not keep launcher icon / splash / first launch UX as an active P1 blocker unless a new device-specific regression is reported.

## Recent frontend status

Latest relevant frontend commits:

- `0064bf1` — Adjust Android splash theme.
- `82321d8` — Polish launcher icon splash and first launch UX.
- `8461d0f` — Fix Android 12 splash resource linking.
- `8dbf913` — Crop launcher icon to central artwork.

Resolved frontend issue:

- Earlier builds had launcher icon / splash / first launch UX problems:
  - icon looked like a small image inside a white frame;
  - icon quality looked blurry;
  - splash / launch transition could show a blank dark screen or a large blurry intermediate icon;
  - first launch could immediately open Android “Alarms & reminders” settings;
  - main screen text readability needed verification.
- After latest APK testing on OnePlus 15, these are verified as fixed for that device.

## Backend status summary

Recent backend commits still known:

- `58fef86` — Russian date parser fix.
- `8ff7197` — spoken dates and date ranges.
- `df1f2b3` — range and event date tests.
- `c5071c1` — Room due date id fix.
- `e86d168` — calendar sync window and holiday noise.
- `d712dd6` — clamp calendar sync window to 30 days.

Important backend state:

- CalendarSourceReader should enforce at least a 30-day calendar sync window and at least 120 events.
- All-day holiday noise such as `День России` should not appear as an active Radar card.
- Google Tasks / Задачи are not supported by the current CalendarContract-based calendar module and should be treated as unsupported, not as a CalendarSync bug.

## Known open issues

1. **CalendarSync still needs focused verification**
   - Manual / foreground calendar import should be tested with a user timed event about 26 days ahead.
   - Background calendar sync after process kill, reboot, or long device sleep is not fully proven.

2. **Temporal engine is not unified yet**
   - Current DateTime parsing has several local fixes.
   - Needed: one shared `TemporalResult` / `TemporalEngine` used by diagnostics, AI Resolution, RadarCard creation, and notification scheduling.

3. **Date ranges remain fragile**
   - Example: `день китаец с пятого мая по седьмое мая` should produce start and end dates.
   - Current implementation may not represent ranges as first-class data.

4. **Known holidays / events are not a real feature yet**
   - Example: `день влюбленных в час дня` should not automatically claim a holiday date unless an explicit event database exists.

## Current next priority

```text
P1: Return to CalendarSync verification.
P1: Test CalendarSync with a user event about 26 days ahead, e.g. “День петуха тест 1”.
P1: Confirm holiday noise such as “День России” does not appear as an active Radar card.
P1: Keep Google Tasks / Задачи classified as unsupported unless a dedicated Tasks module is added.
P1: Start designing a real TemporalResult / TemporalEngine instead of adding more regex patches.
```

Acceptance criteria for the next CalendarSync pass:

- Fresh APK installs and launches successfully.
- Manual calendar import reads at least the next 30 days.
- A user timed event 26 days ahead appears in AI Radar.
- All-day holiday noise such as `День России` does not appear as an active Radar card.
- Google Tasks remain clearly unsupported by the calendar module.

Acceptance criteria for future Temporal Engine work:

- One parser output object is shared by diagnostics, AI Resolution, RadarCard creation, and notification scheduling.
- The output includes start date/time, optional end date/time, range flag, all-day flag, confidence, source text, normalized text, missing parts, and reason.
- Manual text, voice text, local parser output, and Yandex AI result are reconciled into the same result type.

## Cross-project coordination rules

- Update this file after project-state-changing work.
- When handing work to another project/chat, include destination and ready-to-copy text.
- Treat this file as the shared source of truth.
