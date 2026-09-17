# Musitify -- Technical & Architectural Reference Manual

**Package**: `com.yatharth.musitify`  
**Target Platform**: Android (Architecture designed for Windows Porting compatibility)  
**Design DNA**: Tracknix (Neo-Brutalist, Hard Shadows, Monospace Typography, Dynamic 3D Transforms)

---

## 1. Executive Summary

**Musitify** is a high-performance, offline-first personal music manager, downloader, and player. It operates as a self-hosted client-server ecosystem, allowing users to build a local media library by pulling audio streams directly from YouTube, converting them on-device to high-quality MP3 files, organizing them with custom playlists, and seamlessly synchronizing library state and files across multiple devices over a bidirectional REST and transactional action-sync API.

---

## 2. Architecture & Design Patterns

The codebase adheres to a highly modular, decoupled architecture, using standard Clean Architecture principles adapted for Android:

```
[ UI Layer (Compose / ViewModels) ]
                │
                ▼
  [ Domain / Repository Layer ]
    ┌───────────┴───────────┐
    ▼                       ▼
[ Local DB (Room) ]   [ External Engines (Chaquopy/Python/FFmpeg) ]
    │                       │
    ▼                       ▼
[ SQLite Storage ]    [ Network APIs (OkHttp / Sync Server) ]
```

### 2.1 MVVM (Model-View-ViewModel)
- **View**: Implemented entirely via Jetpack Compose. Screens and UI components are state-driven, listening to unidirectional state streams (`StateFlow`) exposed by the ViewModels.
- **ViewModel**: Standard Android Architecture ViewModels lifecycle-bound and injected via Dagger Hilt. ViewModels act as state machines and dispatchers, converting asynchronous database Flows and repository actions into UI State.
- **Model**: Consists of robust Room entities, custom network payload serialization data classes, and platform-agnostic business logic.

### 2.2 Repository Pattern
- Direct database and API details are hidden from the UI layer behind clean repository abstractions:
  - `SongRepository`: Orchestrates download queuing, concurrency limiting, local file storage, database persistence, and file deletion.
  - `PlaylistRepository`: Manages playlist structures, database links, and dynamic multi-image collage art generation.
  - `SettingsRepository`: Manages persistent, key-value application settings stored in a dedicated database table.
  - `MediaControllerRepository`: Binds the UI layer to the active background audio player service, managing playback states and tracking accurate listening statistics.
  - `ActionRepository`: Records every mutation (likes, playlist edits, additions) to a transactional outbox queue (`SyncAction`) to support incremental delta syncs.

---

## 3. Core Functional Pillars & Deep Technical Deep-Dive

### 3.1 YouTube Downloader & Media Processor (`app/src/main/python/` & `util/`)
Musitify employs a hybrid native/interpreted architecture to download and process audio:

1. **Stream Extraction (Python & yt-dlp)**:
   - Uses **Chaquopy** (v17.0.0) to embed a Python 3.14 interpreter within the Android JVM run-time.
   - Python code dynamically invokes **yt-dlp** inside the JVM to pull the optimal audio stream and raw album art thumbnail from a YouTube link, as defined in `downloader.py`. It also parses metadata (duration, title, artist) and returns it as a structured Python dictionary (`PyObject`) to Kotlin.
2. **Asynchronous Format Conversion (FFmpegKit)**:
   - Extracted streams are converted to high-fidelity MP3 files locally.
   - `ConvertSong.kt` wraps the native C++ library **FFmpegKit** (`com.arthenica:ffmpeg-kit-next:8.1.0`).
   - Runs the synchronous shell-like command:
     ```bash
     -vn -acodec libmp3lame -q:a 0
     ```
     This strips potential video channels, encodes using the high-quality LAME MP3 encoder at variable bitrates, and normalizes output.
3. **Metadata Normalization (`SongNormalizer.kt`)**:
   - Cleans messy YouTube video titles by stripping noise (e.g., `"(Official Video)"`, `"[Lyrics]"`, `"(HD)"`), extracting version markers (e.g., `"Remaster "`, `"Live"`, `"Acoustic"`), normalizing Unicode text encoding, and outputting clean, standardized artist and title fields.

---

### 3.2 Offline Storage & Database Schema (`data/`)
The local store relies on Android **Room Database** (v2.8.4) over SQLite. Over the course of 8 consecutive schema migrations, the database structure has evolved into a robust relational model.

#### Relational Schema Model:
```
  ┌──────────────┐          ┌──────────────────┐          ┌──────────────────┐
  │     Song     │          │  SongToPlaylist  │          │     Playlist     │
  ├──────────────┤ 1      * ├──────────────────┤ *      1 ├──────────────────┤
  │ id (PK)      ├─────────►│ id (PK)          │◄─────────┤ id (PK)          │
  │ name         │          │ songId (FK)      │          │ name             │
  │ artist       │          │ playListId (FK)  │          │ timeHeard        │
  │ length       │          │ position         │          │ artPath          │
  │ filePath     │          │ addedOn          │          │ createdAt        │
  │ artPath      │          │ song_uuid        │          │ uuid             │
  │ timeHeard    │          │ playlist_uuid    │          └──────────────────┘
  │ downloadedAt │          └──────────────────┘
  │ url (Unique) │
  │ uuid         │
  └──────────────┘
```

- **`Song` Table**: Stores song metadata, playback times, and local storage references (`filePath`, `artPath`). Has unique indexes on `url` and `uuid`.
- **`Playlist` Table**: Stores name, creation times, play stats, and a path to the generated 2x2 collage artwork. Has a unique index on `uuid` and `name`.
- **`SongToPlaylist` Table**: A join table mapping songs to playlists. Implements `CASCADE` delete actions on foreign keys. Tracks relative track positions.
- **`Settings` Table**: A key-value database table store (`id`, `name`, `value`, `updated_on`) for persistence. Holds first-run checks, the local device's custom UUID, and sync-cursor timestamps.
- **`Actions` (`SyncAction`) Table**: Implements an transactional sync outbox queue. Stores events like `SONG_LIKED`, `PLAYLIST_CREATED`, or `SONG_ADDED_TO_PLAYLIST` alongside their JSON-serialized payloads to support asynchronous synchronization.

---

### 3.3 Media Playback Engine (`music/` & `service/`)
Audio playback is built on Jetpack **Media3 ExoPlayer** and is highly decoupled from the UI:

- **`MusicPlayerManager`**:
  - A dedicated Android `MediaSessionService` foreground service.
  - Instantiates and owns a single `ExoPlayer` instance and registers a `MediaSession` allowing the OS to interact with active playback.
  - Registered in the `AndroidManifest.xml` with `foregroundServiceType="mediaPlayback"` and requested permissions (`FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS`).
  - This architecture ensures playback survives backgrounding, provides native lock screen controls, standard media notifications, Bluetooth commands, and automatically pauses on headphone disconnect (audio focus handling).
- **Listening Progress Tracking**:
  - The UI/ViewModel layer does not interface directly with the player. Instead, `MediaControllerRepository` connects to the running service.
  - A polling loop executes every `500ms` when actively playing to fetch position metrics.
  - Every `7 seconds` of sustained playback, accumulated listening milliseconds are written to the SQLite database for both the specific track and playlist, allowing accurate listening stats and play count calculations (`playCount = timeHeard / length`).

---

### 3.4 Bidirectional Network Synchronization (`network/` & `service/`)
To support cross-device parity, Musitify features two independent synchronization engines over HTTP API calls made via **OkHttp** and serialized using **Kotlinx Serialization**:

1. **Full Snapshot Synchronization (`SyncController.kt`)**:
   - Compiles the entire local database state (all songs, playlists, and associations) into a single massive JSON payload and pushes it to `/sync/bootstrap`.
   - On pull, the device receives a full database state, downloads any missing MP3 tracks from source, and inserts missing records.
2. **Incremental Transactional Sync (`ActionController.kt`)**:
   - Implements an outbox pattern. Local edits are stored as transaction records (`SyncAction`).
   - `/actions` endpoints accept pending actions, while the client pulls remote actions since its last registered cursor.
   - **`RemoteChangesManager`** processes incoming remote actions, performing idempotent inserts, deletes, or relationships locally.

---

### 3.5 UI & UX Design System (The "Tracknix" DNA)
The interface is written entirely in Jetpack Compose, emphasizing a rigid, flat, bold retro-futuristic aesthetic.

#### Key Design System Components (`ui/elements/`):
- **3D Snap Carousels (`MusicCarousel.kt`, `PlaylistCarousel.kt`)**:
  - Custom horizontal scrolling lists using `LazyRow` combined with custom graphics transforms.
  - Elements dynamically scale down, rotate slightly on the Y-axis (3D cover flow), and dim their alpha as their distance increases from the viewport center.
- **Spinning Vinyl Record Animation (`MusicRecord.kt` & `PlaybackScreen.kt`)**:
  - A custom `Canvas` drawing element. It draws a black record disc with standard concentric grooved rings, a white center-label, and overlays the current song's album art.
  - Built using Compose's `rememberInfiniteTransition` to rotate the vinyl a full 360 degrees every 6 seconds.
- **Neo-Brutalist Components (`ShadowBox.kt`, `CustomButton.kt`)**:
  - Reusable container components featuring solid thick black borders (`2.dp` to `4.dp`), a flat sage/coral palette, and an offset, hard-edged solid shadow (no blurs or drop-shadows).
  - Handles bounciness using gesture-driven visual press-offsets (translates elements slightly down and right on press to "collapse" into their shadow).
- **Dynamic Monospace Typography (`Type.kt`)**:
  - Configures the custom `JetBrains Mono NL` font family across multiple weights (ExtraLight, Regular, ExtraBold).
  - Features adaptive viewport-width scaling to ensure typographical elements display cleanly across varying mobile form-factors.

---

## 4. Directory Structure

```
app/src/main/
 ├── AndroidManifest.xml                        # Process, Activity, Service declarations
 ├── keepRules/rules.keep                       # R8/ProGuard rules
 ├── python/
 │    └── downloader.py                         # yt-dlp downloader script
 ├── res/                                       # Fonts, Images, Launcher icons, System xmls
 └── java/com/yatharth/musitify/
      ├── MusitifyApp.kt                        # Custom Application subclass, starts Media3 Controller
      ├── MainActivity.kt                       # App entry point, bootstrap UUID generator
      │
      ├── data/                                 # --- DATA LAYER ---
      │    ├── dao/                             # SQLite DAOs
      │    │    ├── SongDao.kt
      │    │    ├── PlaylistDao.kt
      │    │    ├── SongToPlaylistDao.kt
      │    │    ├── SettingsDao.kt
      │    │    └── SyncActionDao.kt
      │    ├── entity/                          # Room Entity definitions
      │    │    ├── Song.kt
      │    │    ├── Playlist.kt
      │    │    ├── SongToPlaylist.kt
      │    │    ├── Settings.kt
      │    │    └── SyncAction.kt
      │    ├── migrations/                      # v1 to v8 migrations
      │    ├── repository/                      # Repositories (Db and Engine aggregators)
      │    └── viewmodel/                       # Business logic state machines (Hilt ViewModels)
      │
      ├── network/                              # --- NETWORK SYNC LAYER ---
      │    ├── model/                           # JSON serialization schemas
      │    ├── SyncController.kt                # Full DB snapshot sync client
      │    └── ActionController.kt              # Incremental transaction sync client
      │
      ├── service/                              # --- CORE BACKGROUND SERVICES ---
      │    ├── IdManager.kt                     # Local device & entity UUID bootstrapping
      │    └── RemoteChangesManager.kt          # Applies pulled sync transactions to local storage
      │
      ├── music/                                # --- AUDIO ENGINE ---
      │    └── MusicPlayerManager.kt            # MediaSessionService & ExoPlayer instance
      │
      ├── util/                                 # --- SYSTEM ENGINE UTILITIES ---
      │    ├── ConvertSong.kt                   # FFmpegKit wrapper for local MP3 encoding
      │    ├── SongMedia.kt                     # Bitmap manipulation & 2x2 cover collage creation
      │    ├── SongNormalizer.kt                # Clean-up titles via RegEx normalizers
      │    └── Formatter.kt                     # Time conversion helpers
      │
      └── ui/                                   # --- UI & UX LAYER ---
           ├── theme/                           # Color, Type, Theme, DesignSystem
           ├── screens/                         # HomeScreen, PlaybackScreen
           └── elements/                        # PlaybackControls, SongItemUI, ShadowBox, Buttons, Custom Dialogs
```

---

## 5. Porting Foundations (Windows Preparation)

This technical layout has been explicitly mapped to support an efficient porting process to Windows desktop:
- **UI Code Compatibility**: The custom Tracknix Canvas rendering, Custom Buttons, Custom Switches, and 3D Carousel math are pure, platform-independent Compose logic. Porting to Compose Multiplatform requires minor dependency swaps (e.g., swapping Android Activity for JVM Window, updating Coil to v3 for Desktop, and swapping Hilt for manual/Koin DI).
- **Engine Re-implementation**: Platform-dependent elements (ExoPlayer and Chaquopy Python integrations) are abstracted in isolation. On Windows, they will be elegantly swapped for `ProcessBuilder` system pipes running local `ffplay` and native command-line Python scripts respectively.
- **Relational Integrity**: Using standard Kotlin/JVM Room database compatibility maintains exact schema and sync compatibility across both platforms.
