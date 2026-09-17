# Musitify — Features

A list of all features implemented in Musitify so far.

---

## 1. Music Downloading

- Download audio from any **YouTube video URL** or **YouTube playlist URL**.
- Powered by `yt-dlp` running inside the app via **Chaquopy** (Python 3.14).
- Automatically converts downloaded audio to **MP3** using **FFmpeg** (`libmp3lame`, quality 0).
- Downloads **thumbnail** (`.webp`) alongside the audio file.
- Artist extraction with fallback chain: `artist` → `artists` → `uploader` → "Unknown Artist".
- **Song name normalization** — strips YouTube title noise (e.g. "(Official Video)", "(Lyric)", "(feat.)") and extracts version tags (live, remastered, acoustic, etc.).
- Download progress indicator in the UI (`X/Y` songs).
- Retry logic with backoff (3 retries, 3s delay) for resilient downloads.
- **Deduplication** — skips downloading songs already in the library (matched by URL).

## 2. Local Music Library (Room Database)

- Full **Room database** (v2.8.4) with 5 entities:
  - `Song` — name, artist, file path, art path, length, listen time, download date, URL, UUID.
  - `Playlist` — name, art path, listen time, created date, UUID.
  - `SongToPlaylist` — join table with position ordering, FK cascades.
  - `Settings` — key/value store (device ID, sync cursor, first-open flag).
  - `SyncAction` — outbox table for CQRS-style sync actions.
- **8 migration versions** from initial schema to current state.
- Unique indices on song URL, song UUID, playlist name, and action UUID for integrity and dedup.

## 3. Playback (Media3 / ExoPlayer)

- **Foreground MediaSessionService** — background playback with notification and lock-screen controls.
- **ExoPlayer** integration via `MediaControllerRepository` singleton, connected at app startup.
- **Playback controls**: play/pause, next, previous, repeat mode toggle (Off → All → One).
- **Animated vinyl record UI** — continuously rotating album art displayed on a Canvas-drawn vinyl with grooves.
- **Progress bar** (`FillBar`) showing current position / duration with real-time updates.
- **Listen-time tracking (scrobbling)** — accumulates per-song listen time via 7s polling intervals (capped at 15s per poll to guard against jumps). Also tracks per-playlist listen time.

## 4. Playlists

- **Create playlists** by name (deduplicates if name already exists).
- **Add songs to playlists** — multi-select with search filter, ordered by position.
- **Remove songs from playlists**.
- **Delete playlists** (with confirmation dialog).
- **Auto-generated cover art** — 512x512 collage of up to 4 songs' album art in a quadrant grid, cached to disk.
- Playlist-level listen time tracking.

## 5. Sorting & Search

- **Sort modes** (cycling): downloaded date (newest/oldest), play count (most/least).
- **Sort icon** — dynamic icon (clock / headphones) with direction arrow.
- **Search** — debounced (300ms) LIKE query on song name.
- **Play count** — computed as `round(timeHeard / length)` (full listens), displayed in UI.

## 6. Cloud Sync

Two sync architectures exist in the codebase:

### Action-Based Sync (active)
- Every local mutation (like/unlike song, playlist create/delete, add/remove from playlist) is recorded as a **SyncAction** in an outbox table with a unique UUID.
- **Push** — POSTs unsynced actions to server, marks them synced.
- **Pull** — fetches remote actions using a cursor, applies them locally (download new liked songs, delete unliked songs, create/delete playlists).
- `RemoteChangesManager` handles applying remote actions.

### Full Library Bootstrap Sync (disabled)
- Pushes entire library (songs, playlists, relations) as a single payload.
- Pulls entire library and reconciles by URL/UUID.
- Currently commented out in the UI in favor of action-based sync.

## 7. Design System & Theming

- **"Tracknix" design aesthetic** — offset shadow boxes, thick borders (1.5–2dp), minimal rounding, sage-green palette.
- **Custom color palette** — coral, green, sage background, near-black text, light sage cards.
- **JetBrains Mono** font family (regular, extrabold, extralight) with adaptive scaling based on screen width.
- **Custom UI components**: `ShadowBox`, `CustomButton` / `CustomCircularButton` (press animation with offset), `CustomSwitch`, `CustomAlertBox`, `FillBar`, `SortIcon`.
- Light and dark Material 3 color schemes.

## 8. Home Screen & Navigation

- **Single-activity, single-screen** architecture — no Navigation Compose; all UI is modal-driven.
- **Playlist carousel** — horizontal snap-fling row with 3D cover-flow effect (scale, rotationY, alpha based on distance from center). Long-press to delete.
- **Song carousel** — same 3D cover-flow treatment for songs, showing album art, name, duration, and play count. Long-press to delete, tap to play.
- **FABs** — sync (push) and fetch (pull) library buttons.
- **Dialogs**: AddSongDialog (URL input), PlaylistDialog (create / add to playlist with multi-select), CustomAlertBox (confirm delete with optional "don't show again").

## 9. First-Run & Settings

- **Device ID generation** — unique UUID created on first launch, stored in Settings.
- **`hasOpened` flag** — tracks whether the app has been opened before for first-launch setup.
- Generic key/value settings store for future extensibility.

## 10. Networking

- **OkHttp** (v5.4.0) for all HTTP calls (sync push/pull).
- **Coil** for async image loading in Compose.
- **kotlinx-serialization** for JSON serialization of network models and sync actions.
- Cleartext traffic enabled for local dev server (`http://localhost:8080`).

---

## Tech Stack Summary

| Component        | Technology                              |
|------------------|-----------------------------------------|
| Language         | Kotlin + Compose                        |
| UI               | Jetpack Compose, Material 3             |
| DI               | Hilt                                    |
| Database         | Room 2.8.4                              |
| Playback         | Media3 ExoPlayer 1.10.1                 |
| Download         | yt-dlp via Chaquopy (Python 3.14)       |
| Conversion       | FFmpeg (ffmpeg-kit-next 8.1.0)          |
| Networking       | OkHttp 5.4.0                            |
| Serialization    | kotlinx-serialization 1.11.0            |
| Image Loading    | Coil 2.7.0                              |
| Min SDK          | 24                                      |
| Target SDK       | 37                                      |
