# Personal AI Radar — project status

_Last updated: 2026-06-04_

## Current frontend status

### Launcher icon P1

Latest frontend commit:

- `8dbf913` — Crop launcher icon to central artwork.

What changed:

- The app icon crash had already been removed.
- The old placeholder icon had already been removed.
- The remaining bug was that the launcher icon still contained text from the source poster.
- `8dbf913` replaces launcher image assets with a crop of the central artwork only.
- Expected icon content: lighthouse plus glowing road / pulse line.
- The icon must not contain internal text.

Required verification:

- Run Android Build after `8dbf913`.
- Run Android Emulator Smoke Test after `8dbf913`.
- Install APK cleanly on a phone.
- Check icon on launcher, app list, and Android app info.

Acceptance criteria:

- App launches without crash.
- Icon shows only the central artwork.
- No poster title or subtitle is visible inside the icon.
- No old vector icon or fallback placeholder is visible.

## Recent launcher history

- `95f5e0a` — rebuilt launcher assets, but text remained in the icon.
- `7e9ef34` — removed launcher crash path, but visual icon stayed wrong.
- `8269b7d`, `4e4cca8`, `781f4fc`, `c1cf3ce`, `4e73a0a` — earlier crash mitigation attempts.
- `72533e6` and `94c418b` — earlier icon attempts, superseded.

## Backend status summary

Recent backend commits still known:

- `58fef86` — Russian date parser fix.
- `8ff7197` — spoken dates and date ranges.
- `df1f2b3` — range and event date tests.
- `c5071c1` — Room due date id fix.
- `e86d168` — calendar sync window and holiday noise.
- `d712dd6` — clamp calendar sync window to 30 days.

## Known open issues

1. Launcher icon needs verification after `8dbf913`.
2. Temporal engine is not unified yet.
3. Date ranges remain fragile.
4. Known holidays/events are not a real feature yet.
5. Calendar background sync is not fully proven.

## Current next priority

```text
P1: Run Android Build after 8dbf913.
P1: Run Android Emulator Smoke Test after 8dbf913.
P1: Test launcher icon after clean phone install.
P1: Then return to CalendarSync and Temporal Engine work.
```

## Cross-project coordination rules

- Update this file after project-state-changing work.
- When handing work to another project/chat, include destination and ready-to-copy text.
- Treat this file as the shared source of truth.
