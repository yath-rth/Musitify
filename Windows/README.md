# Musitify for Windows

Desktop port of the Android app in `../Android`: same features and Tracknix design, built with Compose Multiplatform Desktop.

## Run

```
./gradlew run
```

The first build downloads `yt-dlp.exe`, `ffmpeg.exe` and `ffprobe.exe` into `bin/` (git-ignored). If GitHub is unreachable, drop those three files into `bin/` by hand.

## Build

```
./gradlew createDistributable   # portable app: build/compose/binaries/main/app/Musitify/Musitify.exe
./gradlew packageMsi            # installer:    build/compose/binaries/main/msi/Musitify-1.0.0.msi
./gradlew test
```

## Where things live

| What | Where |
|---|---|
| Database, covers, settings | `%APPDATA%\Musitify\` |
| MP3s + thumbnails | `%USERPROFILE%\Music\Musitify\` (change in Settings) |
| Sync server | Settings → Server URL (default `http://localhost:8080`) |

## Desktop differences from Android

- Right-click replaces long-press; mouse wheel / drag scrolls the carousels.
- Closing the window keeps music playing in the tray; quit from the tray menu.
- Media keys work globally. In-app: Space play/pause, ←/→ seek 5s, Ctrl+←/→ prev/next, Ctrl+R repeat, Esc back.
- Wide windows show the player in a right-side panel.
- Settings dialog (gear icon): server URL, music folder, Sync now, device id.
- Fixed Android bugs: sync now marks actions synced, saves its cursor, applies renames and skips echoes; missing artists fall back to the uploader; failed downloads show as failed; cancel stops yt-dlp.

App map used for the port: `docs/map/`.
