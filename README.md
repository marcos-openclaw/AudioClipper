# AudioClipper

A simple, single-purpose Android app for trimming audio files.

## Features

- **Load audio files** — supports MP3, M4A, AAC, OGG, WAV, FLAC
- **Timeline scrubber** — horizontal seek bar with visual trim range
- **Trim controls** — SET IN / SET OUT buttons to define clip boundaries
- **Fade in / Fade out** — toggle fades with adjustable duration (0–5 seconds)
- **Export as MP3** — export trimmed audio using FFmpeg, saved to `Music/AudioClipper/`
- **Export feedback** — progress indicator and snackbar with output path

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Audio playback:** AndroidX Media3 (ExoPlayer)
- **Export engine:** FFmpeg-Kit (`io.github.maitrungduc1410:ffmpeg-kit-min:6.0.1`)
- **Architecture:** Single-Activity, ViewModel (MVVM)
- **Min SDK:** 26 / **Target SDK:** 35

## Design

Dark theme with charcoal background and purple/violet accent colors. Minimalist and functional interface focused on the trimming workflow.

## Build

```bash
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`
