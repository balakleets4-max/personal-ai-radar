# Frontend note — launcher icon / splash / first launch UX

Updated after commit `82321d8`.

## Current state

Build 254 testing showed that launcher icon / splash / first launch UX is still not closed.

Working before `82321d8`:

- APK installs.
- App launches.
- Old placeholder icon is gone.
- Text inside the icon is removed.
- Lighthouse artwork is used.

Still broken before `82321d8`:

- Launcher icon looked like a small picture inside a white tile/frame.
- Icon quality still looked blurry.
- Launch transition had a poor empty/dark intermediate screen.
- First launch could send the user directly to Android alarm/reminder settings.
- Main-screen text contrast was poor on the current background.

## Latest frontend fix

`82321d8` — Polish launcher icon splash and first launch UX

Changes:

- Restored launcher routing to `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round` so Android uses adaptive icon behavior instead of raw legacy drawable behavior.
- Changed adaptive icon background to dark navy to avoid the white-tile/frame impression.
- Changed Android 12+ splash animated icon to `@mipmap/ic_launcher_foreground` instead of a transparent icon, so startup is not an empty dark screen.
- Changed app window background to a light app background to restore readable default text contrast.
- Replaced the exact-alarm manifest permission path to avoid immediately pushing the user into system alarm/reminder settings on first launch in the current APK path.

## Required next verification

- Run Android Build after `82321d8`.
- Run Android Emulator Smoke Test after `82321d8`.
- Clean-install APK on phone.
- Check launcher icon on home screen, app list, installer, and Android app info.
- Start app from launcher and check launch transition.
- Confirm first launch stays in-app unless user explicitly chooses a system permission/settings action.
- Check text contrast for `Введите захват памяти` and `Готово. Введите мысль...`.

## Status

Do not close launcher icon / splash / first launch UX until the APK after `82321d8` passes phone verification.
