# Splash note

Commit `0064bf1` changes Android 12+ splash behavior after phone testing showed a large blurred intermediate icon on a white screen.

Latest fix:

- `0064bf1` — Adjust Android splash theme.

What changed:

- Android 12+ `windowSplashScreenAnimatedIcon` no longer uses the launcher foreground artwork.
- Splash uses the existing transparent splash drawable with the same light background as the app.
- Goal: remove the large blurred intermediate icon and make startup feel like a short neutral transition into the app.

Required verification:

- Android Build after `0064bf1`.
- Android Emulator Smoke Test after `0064bf1`.
- Clean phone install.
- Start app from launcher and confirm there is no large blurred icon on the splash screen.
- Launcher icon still needs separate full-size confirmation outside a folder/app drawer view.

Status: launcher icon / splash is not closed until phone verification passes.
