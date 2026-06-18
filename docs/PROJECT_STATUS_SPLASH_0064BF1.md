# Status note — splash after 0064bf1

Latest frontend fix: `0064bf1` — Adjust Android splash theme.

Phone test before this fix:

- APK installed cleanly.
- App launched without crash.
- First launch stayed in the app.
- Main screen text was readable.
- Old placeholder icon was gone.
- Text inside the icon was removed.
- Splash still showed a large blurry intermediate icon.

What `0064bf1` changes:

- Android 12+ splash no longer uses launcher foreground artwork as the splash animated icon.
- Splash uses the existing transparent splash drawable.
- Splash background remains the same light app background.
- Goal: remove the large blurry splash icon and make launch look like a short neutral transition.

Next verification:

- Android Build after `0064bf1`.
- Android Emulator Smoke Test after `0064bf1`.
- Clean phone install.
- Confirm there is no large blurry splash icon.
- Check launcher icon separately outside a folder / in app list / in app info.

Status: launcher icon / splash is not closed until phone verification passes.
