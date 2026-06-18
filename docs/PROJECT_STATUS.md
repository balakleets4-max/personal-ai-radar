# Personal AI Radar — project status

_Last updated: 2026-06-04_

## Current frontend status

### Launcher icon / Splash P1

Latest frontend commits:

- `0064bf1` — Adjust Android splash theme.
- `82321d8` — Polish launcher icon splash and first launch UX.
- `8461d0f` — Fix Android 12 splash resource linking.
- `8dbf913` — Crop launcher icon to central artwork.

Current state from phone testing:

- APK installs cleanly.
- App launches without crash.
- First launch opens the app and does not immediately open Android settings.
- Main screen text is readable.
- Old placeholder icon is gone.
- Text inside the icon is removed.
- Lighthouse artwork is used.
- Bug is **not closed** because splash still showed a large blurry intermediate icon before `0064bf1`.
- Final launcher icon quality still needs a separate large-view check outside a folder.

What changed in `0064bf1`:

- Android 12+ splash no longer uses launcher foreground artwork as `windowSplashScreenAnimatedIcon`.
- Splash now uses the existing transparent splash drawable with the same light background as the app.
- Goal: remove the large blurry intermediate icon and make startup feel like a short neutral transition.

Required verification:

- Run Android Build after `0064bf1`.
- Run Android Emulator Smoke Test after `0064bf1`.
- Install APK cleanly on a phone.
- Start app from launcher and confirm there is no large blurry splash icon.
- Check launcher icon separately on home screen, app list, installer, and Android app info.

Acceptance criteria:

- App builds successfully.
- App launches without crash.
- Splash transition does not show a large blurry icon.
- Main screen text remains readable.
- First launch stays in app unless the user chooses a settings action.
- Launcher icon is acceptable in full-size phone verification.

## Backend status summary

Recent backend commits still known:

- `58fef86` — Russian date parser fix.
- `8ff7197` — spoken dates and date ranges.
- `df1f2b3` — range and event date tests.
- `c5071c1` — Room due date id fix.
- `e86d168` — calendar sync window and holiday noise.
- `d712dd6` — clamp calendar sync window to 30 days.

## Known open issues

1. Launcher icon / splash needs verification after `0064bf1`.
2. Temporal engine is not unified yet.
3. Date ranges remain fragile.
4. Known holidays/events are not a real feature yet.
5. Calendar background sync is not fully proven.

## Current next priority

```text
P1: Run Android Build after 0064bf1.
P1: Run Android Emulator Smoke Test after 0064bf1.
P1: Test splash after clean phone install.
P1: Test launcher icon separately outside a folder.
P1: Then return to CalendarSync and Temporal Engine work.
```

## Cross-project coordination rules

- Update this file after project-state-changing work.
- When handing work to another project/chat, include destination and ready-to-copy text.
- Treat this file as the shared source of truth.
