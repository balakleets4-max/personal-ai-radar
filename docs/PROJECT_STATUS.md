# Personal AI Radar — project status

_Last updated: 2026-06-04_

## Current frontend status

### Launcher icon / Splash P1

Latest frontend commits:

- `8461d0f` — Fix Android 12 splash resource linking.
- `28c276b` — Use high resolution launcher drawable and splash theme.
- `8dbf913` — Crop launcher icon to central artwork.

Current state from user testing:

- App launches without crash before the splash-theme change.
- Old placeholder icon is gone.
- Text inside the icon is removed.
- Central lighthouse artwork appears.
- Bug is **not closed** because the icon still looks blurry and the Android launch transition shows a large blurry intermediate icon.

CI finding after `28c276b`:

- Android Build failed at `:processDebugResources`.
- Cause: `values-v31/styles.xml` used `android:postSplashScreenTheme`, but resource linking reported `android:attr/postSplashScreenTheme` not found.
- Fix committed in `8461d0f`: removed the unsupported `android:postSplashScreenTheme` item. The starting theme already inherits from the normal app theme.

What changed in latest frontend work:

- Manifest routes `android:icon` and `android:roundIcon` directly to `@drawable/ic_launcher_ai_radar` so launcher/app-info paths use the higher-resolution cropped source.
- Added `Theme.PersonalAiRadar.Starting` and Android 12+ splash configuration.
- Added a transparent splash animated icon to avoid showing the launcher icon enlarged and blurry during app startup.
- Added `splash_background` color.

Required verification:

- Run Android Build after `8461d0f`.
- Run Android Emulator Smoke Test after `8461d0f`.
- Install APK cleanly on a phone.
- Check icon on launcher, app list, and Android app info.
- Start the app from the launcher and confirm there is no large blurry intermediate icon.

Acceptance criteria:

- App builds successfully.
- App launches without crash.
- Launcher icon is visually sharper than the previous build.
- Splash transition is clean and does not show a large blurry icon.
- No old vector icon, fallback placeholder, poster-with-text asset, or broken launcher XML is visible.

## Recent launcher history

- `8dbf913` — cropped launcher icon to central artwork; fixed internal text but quality still needed improvement.
- `95f5e0a` — rebuilt launcher assets, but text remained in the icon.
- `7e9ef34` — removed launcher crash path, but visual icon stayed wrong.
- Earlier crash mitigation commits: `8269b7d`, `4e4cca8`, `781f4fc`, `c1cf3ce`, `4e73a0a`.
- Earlier icon attempts: `72533e6`, `94c418b`.

## Backend status summary

Recent backend commits still known:

- `58fef86` — Russian date parser fix.
- `8ff7197` — spoken dates and date ranges.
- `df1f2b3` — range and event date tests.
- `c5071c1` — Room due date id fix.
- `e86d168` — calendar sync window and holiday noise.
- `d712dd6` — clamp calendar sync window to 30 days.

## Known open issues

1. Launcher icon and splash need verification after `8461d0f`.
2. Temporal engine is not unified yet.
3. Date ranges remain fragile.
4. Known holidays/events are not a real feature yet.
5. Calendar background sync is not fully proven.

## Current next priority

```text
P1: Run Android Build after 8461d0f.
P1: Run Android Emulator Smoke Test after 8461d0f.
P1: Test launcher icon quality after clean phone install.
P1: Test Android startup transition and confirm no blurry splash icon.
P1: Then return to CalendarSync and Temporal Engine work.
```

## Cross-project coordination rules

- Update this file after project-state-changing work.
- When handing work to another project/chat, include destination and ready-to-copy text.
- Treat this file as the shared source of truth.
