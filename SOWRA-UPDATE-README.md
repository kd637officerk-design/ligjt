# SOWRA update — merge into the existing `ligjt` repository

This is an **additive update ZIP**. It is not a replacement repository and does not contain a new GitHub project.

## What this update changes

- Renames the Android launcher/app label to **SOWRA**.
- Uses the Ready Player One image supplied in the conversation as the launcher icon.
- Requests Android shared-media permissions for audio and video, and notification permission on Android 13+.
- Scans the Android MediaStore without the previous 500-item cap, so libraries larger than 500 songs can be imported.
- Automatically scans/reconciles the media library after permission is granted when SOWRA opens.
- Adds album artwork from Android's MediaStore album-art cache to song rows and the native media session/notification when Android provides it.
- Updates existing scanned entries and removes entries previously imported by the MediaStore scanner when their source files are no longer present.
- Adds library sorting by date added, title, artist, album, duration, year, file size and modified date.
- Fixes the milliseconds/seconds mismatch in native playback metadata and media-session seek events.
- Adds notification actions for previous, rewind 10 seconds, play/pause, next, forward 10 seconds, restart and stop, plus playback position/duration metadata for Android's media controls.

## Important Android permission note

SOWRA requests permission to read shared audio/video media through Android MediaStore. This is the correct access path for a music library and should include songs in shared internal storage that Android indexes. It does **not** grant access to every app-private folder or every file on the device. Android intentionally restricts those locations; broad `MANAGE_EXTERNAL_STORAGE` access is not required for music scanning and is not added here.

Album artwork depends on artwork being indexed by Android MediaStore. If a song has no indexed album artwork, SOWRA will use its fallback cover.

## How to merge this into your existing GitHub repository

1. Download `SOWRA-Update.zip` and extract it on your computer.
2. Open the extracted folder. It contains repository-relative paths such as `index.html` and `android/app/...`.
3. Open your existing repository: https://github.com/kd637officerk-design/ligjt
4. Upload the **contents** of the extracted folder to the repository root. Do not upload a second `ligjt-main` folder.
5. When GitHub asks about files that already exist, replace only the matching files from this update. Keep every other repository file and folder.
6. Commit the changes to `main`.
7. Open **Actions** and run **Build Android APK**. The existing workflow should run `npm run build` and `npx cap sync android`, which copies the updated web app into Android.

GitHub's browser uploader does not unpack ZIP files automatically. Extract the ZIP first, then upload its folders/files while preserving their directory paths.

## Files included

- `index.html`
- `public/xi_deck.html`
- `android/app/src/main/AndroidManifest.xml`
- `android/app/src/main/java/com/xideck/mediaplayer/InternalFilesPlugin.java`
- `android/app/src/main/java/com/xideck/mediaplayer/AndroidMediaPlugin.java`
- `android/app/src/main/java/com/xideck/mediaplayer/MediaPlaybackService.java`
- `android/app/src/main/res/values/strings.xml`
- `android/app/src/main/res/values/colors.xml`
- `android/app/src/main/res/drawable-nodpi/sowra_icon.png`
- `android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `android/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- Launcher icon PNGs under `android/app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/`

## Build verification status

The HTML JavaScript syntax and Android XML files were checked. A local Gradle build could not be completed in this environment because the Gradle distribution host was unreachable, so this ZIP is **not represented as a verified APK build**. Use the existing GitHub Actions workflow to perform the full Android build and inspect its result.
