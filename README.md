# 🎵 Music Player App

A sleek, modern, and feature-rich Android Music Player application built with **Kotlin**, **Android Jetpack**, and **Material Design 3**.

---

## ✨ Features

- 🎶 **Local Music Library**: Automatically fetches and organizes all audio files stored on your device using `MediaStore API`.
- 🔍 **Real-Time Search**: Search songs instantly by song title, artist name, or album.
- 🎨 **Modern Dark UI**: Designed with a sleek Dark Glassmorphism palette, Material Card Views, and smooth UI transitions.
- 🖼️ **Album Artwork Support**: Dynamic asynchronous cover art loading with memory caching (`LruCache`).
- ⏯️ **Playback Controls**: Play, Pause, Next, Previous, Fast Forward/Seek bar synchronization.
- 🔀 **Shuffle & Repeat Modes**: Support for Shuffle playback and Repeat modes (Repeat All, Repeat One, Repeat Off).
- 📱 **Edge-to-Edge Experience**: Full support for system bars and window insets across all modern Android versions.
- 🛡️ **Scoped Storage & Android 13+ Support**: Fully compatible with Android 10 to Android 15 using Content URIs and runtime audio permissions (`READ_MEDIA_AUDIO`).

---

## 🚀 Tech Stack & Libraries

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [XML Layouts](https://developer.android.com/guide/topics/ui) with [Material Design 3](https://m3.material.io/)
- **Architecture**: Modern Android Activity & RecyclerView Architecture
- **Media Engine**: Android `MediaPlayer` & `MediaStore API`
- **Data Transfer**: Kotlin `@Parcelize`
- **Compatibility**: Min SDK 24 (Android 7.0) | Target SDK 35 (Android 15)

---

## 🛠️ Project Structure

```
MUSICPLAYER/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/musicplayer/
│   │   │   │   ├── AlbumArtUtils.kt       # Async Album Art Loader with LRU Cache
│   │   │   │   ├── MainActivity.kt        # Music Library & Real-time Search Screen
│   │   │   │   ├── PlayerActivity.kt      # Modern Fullscreen Player & Playback Controls
│   │   │   │   ├── Song.kt                # Song Data Model (Parcelable)
│   │   │   │   └── SongAdapter.kt         # RecyclerView Adapter for Song Items
│   │   │   └── res/                       # Layouts, Drawables, Themes, & Strings
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
├── .gitignore
└── README.md
```

---

## 🔒 Permissions Requested

- `READ_MEDIA_AUDIO` (for Android 13 / API 33+)
- `READ_EXTERNAL_STORAGE` (for Android 12 / API 32 and below)

---

## 📱 How to Build and Run

1. Clone or download the repository into Android Studio.
2. Open the project in **Android Studio (Ladybug or newer)**.
3. Sync Gradle project files (`Gradle Sync`).
4. Select an emulator or connected physical Android device running Android 7.0+ (API 24+).
5. Click **Run** (`Shift + F10` or green play button).

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
