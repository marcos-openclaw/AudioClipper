![AudioClipper](docs/images/hero.png)

# AudioClipper

A powerful, single-purpose Android app for trimming and processing audio files.

<img src="docs/images/logo.png" alt="AudioClipper Logo" width="96" />

## Features

- **Load audio files** — supports MP3, M4A, AAC, OGG, WAV, FLAC
- **Timeline scrubber** — horizontal seek bar with visual trim range
- **Trim controls** — SET IN / SET OUT buttons to define clip boundaries
- **Pitch shifting** — shift pitch up or down by semitones (-12 to +12)
- **Volume control** — adjust volume from 0% to 200%
- **Fade in / Fade out** — toggle fades with adjustable duration (0–5 seconds)
- **Export as MP3** — export processed audio using FFmpeg, saved to `Music/AudioClipper/`
- **Export feedback** — progress indicator and snackbar with output path

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Audio playback:** AndroidX Media3 (ExoPlayer)
- **Export engine:** FFmpeg-Kit (`io.github.maitrungduc1410:ffmpeg-kit-min:6.0.1`)
- **Architecture:** Single-Activity, ViewModel (MVVM)
- **Min SDK:** 26 / **Target SDK:** 35

## Design

Dark theme with deep charcoal background (#1A1A2E) and violet/purple accent colors. Modern card-based UI with section grouping, optimized for tall narrow displays.

## Target Device

Samsung Galaxy A36 5G (6.7" 1080×2340, 19.5:9 aspect ratio, 120Hz Super AMOLED). All tap targets meet the minimum 48dp size requirement and the UI scrolls properly on tall displays.

## Build

```bash
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`
