# #XI Media Deck — Android APK & GitHub Actions Build Guide

This repository is pre-configured as a **Capacitor Android APK project** + **PWA** with:

1. **Custom Home Screen Cover Icon**: Generated from `public/assets/aistudio/Arte_de_Stefano_Daddato__d4ed462a.jpg` across all Android `mipmap-*` launcher densities (adaptive foreground + round + legacy) and PWA icons (`192x192`, `512x512`, `maskable`, `apple-touch-icon`).
2. **Android System Media Controls**: Native `MediaPlaybackService` + `AndroidMediaPlugin` (`android.media.session.MediaSession` + `Notification.MediaStyle`) and `navigator.mediaSession` syncing Lock Screen & Notification Shade controls (**Play/Pause, Previous/Backward, Next/Forward, Stop Player, Re-run from 0:00**) with artwork.
3. **Internal Device Files Access**: Declares `READ_MEDIA_AUDIO`, `READ_MEDIA_VIDEO`, `READ_MEDIA_IMAGES`, and `READ_EXTERNAL_STORAGE` (API <= 32) in `AndroidManifest.xml`, wired to `InternalFilesPlugin` (`MediaStore` scanner for `Music/`, `Movies/`, `Download/`).

## Build APK Automatically on GitHub

1. Push this repository to GitHub (`main` or `master` branch).
2. Open your repository's **Actions** tab on GitHub and run **Build Android APK** (or push any commit to trigger automatically).
3. When the workflow finishes, download **`XI-Media-Deck-debug-apk`** from the **Artifacts** section and install `app-debug.apk` on your Android phone.

## Build APK Locally (Optional)

```bash
npm ci
npm run build
npx cap sync android
cd android
./gradlew assembleDebug
# Output APK: android/app/build/outputs/apk/debug/app-debug.apk
```
