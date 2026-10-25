# Engine map — downloading, media processing, playback

Source (Android, read-only reference): `E:\Musitify\Android\app\src\main\`

| File | Role |
|---|---|
| `python/downloader.py` | yt-dlp wrapper (Chaquopy, Python 3.14, `yt-dlp==2026.8.18.122307.dev0`) |
| `java/.../util/ConvertSong.kt` | ffmpeg-kit (`com.arthenica:ffmpeg-kit-next:8.1.0`) → MP3 conversion |
| `java/.../util/SongMedia.kt` | play count, playlist collage art, audio duration probe |
| `java/.../util/SongNormalizer.kt` | title normalization (used as stored song name + dedupe key) |
| `java/.../service/DownloadsManager.kt` | download job queue (2 parallel workers) |
| `java/.../service/SleepTimerManager.kt` | sleep timer |
| `java/.../music/MusicPlayerManager.kt` | Media3 `MediaSessionService` hosting ExoPlayer |
| `java/.../data/repository/MediaControllerRepository.kt` | queue building, UI state, position polling, listen-time tracking |
| `java/.../data/repository/SongRepository.kt` | `downloadSong`, `downloadSongsFromSync`, delete, duration backfill |
| `java/.../data/repository/PlaylistRepository.kt` (supporting) | `downloadPlaylist`, playlist creation, cover generation |
| `java/.../data/viewmodel/PlaybackControllerViewModel.kt` (supporting) | repeat/shuffle toggle, play/pause, next/prev |

Desktop target: Compose Multiplatform Desktop (Kotlin/JVM, Windows), bundled `yt-dlp.exe` + `ffmpeg.exe`, JavaFX `MediaPlayer`.

---

## 1. Download pipeline

### 1.1 Entry points

```
UI (AddSongDialog) → AddSongsViewModel.addSingleSong(url) / addPlaylist(url)
   - url.isBlank() → AddSongsUiState.Error("URL is empty")
   - DownloadsManager.enqueueSong/enqueuePlaylist(url) returns false → Error("Already in the queue")
   - else → Queued(isPlaylist)
DownloadsManager worker → SongRepository.downloadSong(url)                      (SONG)
                        → PlaylistRepository.downloadPlaylist(url, onProgress)    (PLAYLIST)
Sync (SyncController.fetchLibrary) → SongRepository.downloadSongsFromSync(newSongs)
Remote changes (RemoteChangesManager) → SongRepository.addSongFromSync(uuid, payload)
                                          → downloadSong(url, songUUID, downloaded_at, record=false)
```

### 1.2 DownloadsManager (job queue)

- Singleton, `CoroutineScope(SupervisorJob() + Dispatchers.Default)`.
- `DownloadType { SONG, PLAYLIST }`, `DownloadStatus { QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED }`.
- `DownloadJob(id: Long, url: String, type, status = QUEUED, completed: Int = 0, total: Int = 0 /*0 = indeterminate*/, error: String? = null)`; `isActive = status in {QUEUED, RUNNING}`.
- Exposed state: `jobs: StateFlow<List<DownloadJob>>` (in-memory only; lost on app restart — nothing persisted).
- Queue: `Channel<Long>(UNLIMITED)` of job ids; **`MAX_PARALLEL = 2`** worker coroutines each `for (id in queue) process(id)`.
- IDs: `AtomicLong(1)`, incrementing.
- **Dedupe on enqueue**: `url.trim()`; rejected if any job with the identical trimmed string is QUEUED or RUNNING. No URL canonicalization (`youtu.be/X` ≠ `youtube.com/watch?v=X`). Finished jobs with same URL don't block.
- `process(id)`: skip if job missing or status ≠ QUEUED (cancelled while waiting) → set RUNNING → launch child task:
  - SONG: `songRepository.downloadSong(job.url)`
  - PLAYLIST: `playlistRepository.downloadPlaylist(job.url, onProgress = { c, t -> completed=c, total=t })`
  - success → `COMPLETED`, `completed = total = (if total > 0) total else 1`.
  - `CancellationException` → `CANCELLED` (rethrown).
  - other `Exception` → `FAILED`, `error = e.message ?: "Unknown error"`.
  - `running[id] = task; task.join(); running.remove(id)`.
- `cancel(id)`: running → `task.cancel()`; else if QUEUED → mark CANCELLED (worker later skips it).
- `retry(id)`: only from FAILED/CANCELLED → reset to QUEUED, `completed=0,total=0,error=null`, re-send id to channel.
- `clearFinished()`: keep only active jobs.

**Behavioral quirks to preserve or consciously fix:**
1. `downloadSong` catches *all* exceptions internally and returns `null` → a failed single-song download is reported as **COMPLETED** (1/1). Only playlist jobs can reach FAILED (failure in `get_playlist_urls`, or `addSongsToPlaylist` throwing).
2. Cancellation is cooperative only. The Chaquopy `callAttr` is a blocking call inside `withContext(IO)`; cancel does not stop yt-dlp. The song download + DB insert finish anyway, then the job flips to CANCELLED. For playlists, songs not yet past `semaphore.withPermit` are skipped; in-flight ones finish. Desktop can do better by `Process.destroy()`-ing the yt-dlp/ffmpeg subprocess (recommended).
3. Single-song progress is indeterminate (`total = 0`) until completion.

### 1.3 `downloader.py` — `downloadSong(url, output_path, retries=3, delay=3)`

yt-dlp options (Python API, `yt_dlp.YoutubeDL(ydl_opts)`):

```python
ydl_opts = {
    "outtmpl": f"{output_path}/%(title)s-%(uploader)s-%(release_date)s.%(ext)s",
    "format": "bestaudio/best",
    "writethumbnail": True,
    # "quiet": True,            (commented out → yt-dlp prints progress to stdout)
    "no_warnings": True,
}
```

- No postprocessors (no `FFmpegExtractAudio`, no thumbnail conversion, no embedding). Audio is saved in its native container (YouTube: usually `.webm` (opus) or `.m4a`). Thumbnail is written next to it with the same base name, native format (YouTube: `.webp`).
- `noplaylist` not set (default False) → a URL containing `&list=` would download the whole playlist and `prepare_filename(info)` would be wrong. In practice single-video URLs are passed.
- `%(release_date)s` is frequently missing → literal `NA` (e.g. `Song Title-Uploader Name-NA.webm`).
- Steps per attempt:
  1. `info = ydl.extract_info(url, download=True)`
  2. `audio_path = ydl.prepare_filename(info)` (full path incl. extension)
  3. `base = os.path.splitext(audio_path)[0]`; thumbnail = first existing of `base + ".webp"`, `".jpg"`, `".png"` (else `None`).
  4. Artist fallback chain (verbatim):
     ```python
     artist = (
             info.get("artist")
             or str(info.get("artists"))
             or info.get("uploader")
             or "Unknown Artist"
     )
     ```
     **Bug-for-bug note:** `str(None)` is `"None"` (truthy), so `uploader` / `"Unknown Artist"` are **never reached**. Effective chain:
     - `info["artist"]` if truthy (yt-dlp sets this for YouTube Music tracks, comma-joined e.g. `"A, B"`)
     - else `str(info["artists"])` → Python list repr e.g. `"['A', 'B']"` if present
     - else the string `"None"`.
     For a 1:1 port reproduce this; recommended fix: `artist ?: artists?.joinToString(", ") ?: uploader ?: "Unknown Artist"` (decide with owner; synced data from other devices will contain `"None"`).
  5. Return dict: `{"audio": audio_path, "title": info["title"], "artist": artist, "thumbnail": thumbnail_path, "duration": info["duration"]}` (duration in seconds, int for YouTube).
- Retries: up to **3 attempts**, `time.sleep(3)` between attempts (no backoff growth, no sleep after last). After the last failure the last exception is re-raised. (yt-dlp's own internal defaults also apply: `retries=10`, `fragment_retries=10`.)
- `main()/test_download()` are dev-only test harness (downloads `https://youtu.be/NxgyfY70-xQ` into `./downloads`) — not used by the app.

### 1.4 `downloader.py` — `get_playlist_urls(playlist_url)`

```python
ydl_opts = {"extract_flat": True, "quiet": True}
info = ydl.extract_info(playlist_url, download=False)
urls = [entry["url"] for entry in info["entries"] if entry]
playlist_name = info.get("title", "Unknown Playlist")
return {"name": playlist_name, "urls": urls}
```

- No retry. Flat extraction: entries' `url` for YouTube is `https://www.youtube.com/watch?v=<id>`.
- Kotlin side: `name = result["name"].toString().trim() ?: "Unknown Playlist"` (note: Python `None` title → `"None"` string).

### 1.5 `ConvertSong.invoke(result)` — ffmpeg conversion

1. `inputPath = result["audio"]`; null → log, return `""`.
2. `name = inputPath.substringBeforeLast(".").substringAfterLast("/")` → base file name without extension (Android `/` separator; **on Windows use `File(inputPath).nameWithoutExtension`**).
3. `outputPath = inputPath.substringBeforeLast(".") + ".mp3"`.
4. Wait for input file: loop while `!exists() || length() == 0`, `Thread.sleep(200)`, timeout after >5000 ms → return `""`.
5. ffmpeg command (ffmpeg-kit string, verbatim):
   ```
   -i "<inputPath>" -vn -acodec libmp3lame -q:a 0 "<outputPath>"
   ```
   = drop video/cover streams, LAME VBR quality 0 (~220–260 kbps), no tags/metadata options beyond ffmpeg defaults (ffmpeg copies global metadata like title/artist from the source container by default). No `-y`.
6. Success → delete the original input file and `<inputPath>.part` if present. Failure → log all ffmpeg logs; **original kept; name still returned** (so the DB row will point at a non-existent `.mp3`).
7. Returns `name` (base name). Thumbnail is *not* touched (stays e.g. `.webp`).

### 1.6 `SongRepository.downloadSong(url, downloaded_at = now, songUUID = random, record = true): Song?` (on `Dispatchers.IO`)

1. **Dedupe by URL**: `songDao.getSongByUrl(url)` exists → return it (no download).
2. Start Python if needed; `downloader.downloadSong(url, getBaseDir())`.
   - `getBaseDir()` = `context.getExternalFilesDir(null).absolutePath` (e.g. `/storage/emulated/0/Android/data/com.yatharth.musitify/files`) — all songs + thumbnails live flat in this dir.
3. `songPath = convertSong.invoke(result)` (base name, no ext).
4. `songName = result.title ?: "unknown"`, `artist = result.artist ?: "unknown"`, `durationMs = duration * 1000`.
5. `normalizedSongName = SongNormalizer.normalize(songName).normalizedTitle` — **the stored `Song.name` is the normalized title** (lowercase, ASCII-folded, punctuation stripped).
6. **Legacy merge**: `exists = songDao.getSongByName(normalizedSongName)` (`SELECT * FROM Song WHERE name = :name LIMIT 1`). If found **and** `exists.url.contains("legacy")` → `updateSong(exists.copy(url = url))`, delete the freshly downloaded `<base>/<songPath>.mp3` and `<base>/<songPath>.webp`, return the updated existing song (no new row, no sync action).
7. Else build `Song(name = normalizedSongName, artist, length = durationMs ?: 0, timeHeard = 0, filePath = "<base>/<songPath>.mp3", artPath = "<base>/<songPath>.webp", downloadedAt = downloaded_at, url, uuid = songUUID)`.
   - **artPath always assumes `.webp`** even if the thumbnail was `.jpg`/`.png` (python's `thumbnail` value is ignored).
8. `songDao.addSong(song)` (`OnConflictStrategy.IGNORE`; `Song.url` and `Song.uuid` have unique indices). `-1` → re-fetch by URL.
9. `record && inserted != null` → `actionRepository.likedSong(inserted)` (sync action).
10. Any exception → log, return `null`.

### 1.7 `SongRepository.downloadSongsFromSync(nSongs, outputDir = baseDir, maxConcurrent = 5, onProgress)`

- `total = nSongs.size`.
- Dedupe: `existingSongs = songDao.getSongsByUrls(nSongs.map{url})`; `newSongs` = those whose url isn't among existing.
- **Concurrency: `Semaphore(5)`** — up to 5 simultaneous yt-dlp downloads per call (each call runs inside one DownloadsManager worker, so worst case 2 playlist jobs × 5 = 10 concurrent, or 5 + 1).
- Per new song (async + withPermit): same steps as 1.6 steps 2–8 (no URL pre-check, uses `nSong.downloaded_at`, `uuid = UUID.fromString(nSong.id)`). Legacy merge returns `nSong.id → exists.copy(url)`. Exception → `null`. `finally { onProgress(completedCount.incrementAndGet(), total) }`.
- Progress quirk: `total` includes already-existing songs which never tick, so `completed` tops out at `newSongs.size`; DownloadsManager then snaps to `total/total` on COMPLETED.
- After all: `actionRepository.likedSongs(songMap.values)` (new + legacy-merged songs).
- Returns `Map<String /*uuid string*/, Song>` = `songMap (new, in input order) + existingSongs.associateBy{uuid}`.

### 1.8 `PlaylistRepository.downloadPlaylist(playlistUrl, onProgress)`

1. `get_playlist_urls(url)` → `name` (trimmed), `urls`.
2. Wrap each URL into `NSong(id = randomUUID, url, title = "", artist = "", downloaded_at = now)`.
3. `downloadSongsFromSync(...)` → re-key to `Map<Int /*song.id*/, Song>`. **Order** = newly downloaded songs in playlist order, then pre-existing songs (DB order) appended at the end.
4. If non-empty → `createPlaylistAndAddSongs(name, songs)`:
   - `createPlaylist(name)`: reuses an existing playlist **with the same name**, else inserts new (`timeHeard=0`, createdAt now, random uuid) + records sync action.
   - `addSongsToPlaylist`: filter songs that still exist (throws if none); positions start at `getLastPosition(playlistId) + 1`, incrementing in map order; records `recordSongAddedToPlaylist` per song. (No dedupe of songs already in that playlist.)
   - `generatePlaylistArt(playlistId)` (see §3).
   Empty → log, no playlist.

### 1.9 Other SongRepository bits (download-adjacent)

- `deleteSongById / deleteSongByUUID(record = true)`: delete row, record `unlikedSong(uuid)` if `record`, delete `filePath` and `artPath` files.
- `fetchAllAndUpdate()`: for every song with `length = 0`, `SongMedia.getAudioDurationMs(filePath)` → `updateLength`.
- `addSongFromSync(uuid, payload)`: payload JSON `{"url": ..., "downloaded_at": "<ms>"}` → `downloadSong(url, songUUID = uuid, downloaded_at, record = false)`.
- `updateSong(song, record)`: records rename action if name changed.

### 1.10 Desktop command lines (equivalent output)

Base dir suggestion: `%LOCALAPPDATA%\Musitify\songs` (any flat folder; replaces `getExternalFilesDir(null)`). Run both tools via `ProcessBuilder` with explicit arg lists (no shell quoting), `redirectErrorStream(true)`, drain output on a thread, and use the bundled paths.

**Single song download** (one attempt; wrap in 3 attempts × 3 s sleep to match Python):

```
yt-dlp.exe
  --no-playlist                                   (recommended; Python lacked it but always got single URLs)
  -f "bestaudio/best"
  --write-thumbnail
  --no-warnings
  --ffmpeg-location "<bundle>\ffmpeg.exe"         (harmless; no postprocessing is requested)
  -o "<baseDir>\%(title)s-%(uploader)s-%(release_date)s.%(ext)s"
  --no-simulate --dump-single-json                (download AND print final info JSON to stdout)
  --newline --progress                            (optional: line-based progress on stderr/stdout if you want % reporting)
  --encoding utf-8
  "<url>"
```

Parse the JSON (last stdout line / whole stdout; use `--progress-template` + `--quiet` or separate streams to keep JSON clean — simplest: `-q --no-simulate -J` so stdout is only JSON):
- audio path = `requested_downloads[0].filepath` (fallback `_filename` / `filename`)
- `title`, `duration`, `artist`, `artists`, `uploader`
- thumbnail = first existing `<base>.webp|.jpg|.png` (or `requested_downloads[0]`… thumbnails aren't listed there; just probe the disk like Python).
- artist rule (1:1): `artist` if non-empty string → else if `artists` is a JSON array → Python repr `"['" + join("', '") + "']"` → else `"None"`.

Note: yt-dlp on Windows auto-applies Windows filename sanitization (`:` `?` `"` `*` `|` `<` `>` `\` `/` → full-width look-alikes); Android only replaced `/`. File names therefore differ between platforms — fine, paths are per-device; never rebuild a path from the title.

**Playlist listing:**

```
yt-dlp.exe --flat-playlist -J -q --no-warnings --encoding utf-8 "<playlistUrl>"
```
→ `title` (fallback `"Unknown Playlist"`), `entries[].url` (skip null entries).

**Conversion** (after the 200 ms × 5 s file-ready wait):

```
ffmpeg.exe -hide_banner -nostdin -n -i "<base>.<ext>" -vn -acodec libmp3lame -q:a 0 "<base>.mp3"
```
- Core args `-i … -vn -acodec libmp3lame -q:a 0 …` are verbatim from Android. `-hide_banner -nostdin` are cosmetic/safety (prevents a hang on an overwrite prompt); `-n` (never overwrite) mirrors ffmpeg-kit's non-interactive refusal. Use `-y` instead if you'd rather overwrite stale output.
- Exit code 0 → delete `<base>.<ext>` and `<base>.<ext>.part`. Non-zero → keep original, log output.
- If the source is already `.mp3` the output equals the input and ffmpeg fails — same as Android; consider skipping conversion in that case.

**Duration probe** (`getAudioDurationMs` replacement): no ffprobe is bundled → either `ffmpeg.exe -hide_banner -i "<file>"` and parse `Duration: HH:MM:SS.xx` from stderr (exit code 1 is expected), or JavaFX `Media(uri).getDuration()` after `MediaPlayer.onReady`, or a tag library (jaudiotagger / mp3agic).

JavaFX note: JavaFX `MediaPlayer` cannot play opus/webm, so the MP3 conversion is mandatory on desktop too.

---

## 2. SongNormalizer (verbatim)

```kotlin
private val COSMETIC_NOISE = mutableListOf<Pattern>(
    Pattern.compile("\\(official.*?\\)|\\[official.*?]", Pattern.CASE_INSENSITIVE),
    Pattern.compile("\\(lyric.*?\\)|\\[lyric.*?]", Pattern.CASE_INSENSITIVE),
    Pattern.compile("\\(audio\\)|\\[audio]", Pattern.CASE_INSENSITIVE),
    Pattern.compile("\\(hd\\)|\\[hd]|\\(hq\\)|\\[hq]", Pattern.CASE_INSENSITIVE),
    Pattern.compile("\\(explicit\\)|\\[explicit]", Pattern.CASE_INSENSITIVE) // explicit tag ≠ different song
)

private val VERSION_MARKER: Pattern = Pattern.compile(
    "\\((remaster(ed)?( \\d{4})?|live|acoustic|deluxe|radio edit|extended|instrumental|clean|demo)\\)",
    Pattern.CASE_INSENSITIVE
)
```

`normalize(rawTitle): NormalizedSong(normalizedTitle, versionTag)` — order applied:

1. `Normalizer.normalize(rawTitle, Normalizer.Form.NFKD)`
2. `.replace("\\p{M}".toRegex(), "")` — strip combining marks (accents)
3. `.lowercase(Locale.getDefault())` — (locale-sensitive! Turkish locale would map `I`→`ı`; on desktop consider keeping `Locale.getDefault()` for parity or `Locale.ROOT` for safety)
4. `s.replace("\\(feat\\..*?\\)|\\[feat\\..*?\\]|\\(ft\\..*?\\)".toRegex(), "")` — remove `(feat. …)`, `[feat. …]`, `(ft. …)` (note: no `[ft. …]`, requires the dot)
5. Version tag extraction: first `VERSION_MARKER` match → `group(1).replace("\\s+\\d{4}".toRegex(), "").trim { it <= ' ' }` (e.g. `remastered 2009` → `remastered`); none → `""`.
6. `VERSION_MARKER.matcher(s).replaceAll("")` — remove all version markers.
7. For each `COSMETIC_NOISE` pattern in list order: `replaceAll("")`.
8. `s.replace("[^a-z0-9\\s]".toRegex(), "")` — drop everything except a–z, 0–9, whitespace (non-Latin scripts are wiped entirely → can become `""`).
9. `.replace("\\s+".toRegex(), " ")`
10. `.trim { it <= ' ' }`
11. `s.replace("nana", "")` — literal substring removal (no re-collapse/trim of whitespace afterwards; e.g. `"banana split"` → `"ba split"`).

Notes: the `versionTag` is computed but callers only use `normalizedTitle` (stored as `Song.name`; also used by `LegacyFixer.updateAllMusicNames()` to rename all songs and by the legacy-merge lookup). Parentheses/brackets that aren't matched are removed by step 8 but their content stays.

Java regex semantics map 1:1 onto Kotlin/JVM — copy the file unchanged into the desktop project (`java.text.Normalizer` and `java.util.regex` are available on JVM).

---

## 3. SongMedia

### 3.1 `playCountOf(song)`
`(if (song.length != 0L) song.timeHeard.toDouble() / song.length else 0.0).roundToInt()` — plays = total listened ms ÷ track length ms, rounded half-up (Kotlin `roundToInt` = `Math.round`).

### 3.2 Playlist collage
- `createPlaylistCoverBitmap(artPaths, outputSize = 512)`: `Bitmap.createBitmap(512, 512, ARGB_8888)` (transparent initial).
- `half = outputSize / 2 = 256`. Quadrants in order: TL `Rect(0,0,256,256)`, TR `Rect(256,0,512,256)`, BL `Rect(0,256,256,512)`, BR `Rect(256,256,512,512)`.
- `artPaths.take(4)`; for each index: `BitmapFactory.decodeFile(path)`; null (missing/undecodable) → skip, quadrant stays **transparent**. Fewer than 4 songs → remaining quadrants transparent (no repeat/tiling, no 1-image full-bleed special case).
- Source rect = centered square crop: `size = min(w,h)`, `left = (w-size)/2`, `top = (h-size)/2` → scaled into 256×256 dest, `paint = null` (no filtering flag → Android default, nearest-ish; use bilinear on desktop, visually fine).
- `cachePlaylistCover(context, playlistId, artPaths)`: writes **PNG, quality 100** to `<context.filesDir>/playlist_covers/playlist_<playlistId>.png` (dir created), returns File.
- Caller `PlaylistRepository.getOrCreatePlaylistCover(playlistId, songs)`: **if the file already exists it is returned as-is (never regenerated)**; else uses first `min(4, songs.size)` songs' `artPath`. Songs come from `playlistDao.getSongsForPlaylistSuspend(id)` → `songDao.getSongsByIds(ids)` (order of the `IN` query — not guaranteed playlist order). `generatePlaylistArt` then stores the path via `playlistDao.updateArt(id, path)`. Called after a playlist download (`createPlaylistAndAddSongs`); check other callers in the UI/library map.
- Desktop: thumbnails are mostly **WebP** — `javax.imageio` cannot decode WebP. Use Skia (bundled with Compose Desktop): `org.jetbrains.skia.Image.makeFromEncoded(bytes)` → draw onto `Surface.makeRasterN32Premul(512,512)` with `canvas.drawImageRect(img, Rect.makeXYWH(left,top,size,size), Rect.makeXYWH(dx,dy,256,256))` → `surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG)`. (Alternative: TwelveMonkeys `imageio-webp`.) Path: `%APPDATA%\Musitify\playlist_covers\playlist_<id>.png` (replaces `filesDir`).

### 3.3 `getAudioDurationMs(filePath)`
`MediaMetadataRetriever.setDataSource` + `METADATA_KEY_DURATION.toLongOrNull() ?: 0` → see §1.10 for desktop replacement.

---

## 4. Playback engine

### 4.1 Architecture (Android)
- `MusicPlayerManager : MediaSessionService` (manifest: `foregroundServiceType="mediaPlayback"`, not exported, intent filter `androidx.media3.session.MediaSessionService`). `onCreate`: `ExoPlayer.Builder(this).build()` (**all defaults**), `MediaSession.Builder(this, player).setSessionActivity(launch-app PendingIntent, FLAG_IMMUTABLE)`. `onDestroy` releases player + session. Media3 1.10.1 auto-produces the media notification (play/pause, prev, next, seek bar) and lock-screen / Bluetooth controls.
- `MediaControllerRepository` (singleton, `CoroutineScope(Dispatchers.Main + SupervisorJob())`) connects a `MediaController` in `MusitifyApp.onCreate()` via `connect()`. On connect: **`repeatMode = REPEAT_MODE_ALL`**, publish controller, init uiState (`repeatMode`, `shuffleMode`, `isPlaying`), attach listener. `setupSleepTimer` is an empty stub.
- `release()`: flush listen time + release controller future (not called anywhere in app code found).

Desktop: no service/session split needed — one `PlayerEngine` singleton owning JavaFX `MediaPlayer` + queue state. Initialize JavaFX toolkit once (`Platform.startup {}`, or create a `javafx.embed.swing.JFXPanel()`), and marshal all MediaPlayer calls onto the FX thread (`Platform.runLater`).

### 4.2 Queue building — `startPlayback(songs, startIndex, playlistId)`
1. `activePlaylistId = playlistId`, `activePlaylistName = null`, `songDetailsById` = snapshot of passed songs by id.
2. Async: `activePlaylistName = playlistDao.getPlaylistById(id)?.name`.
3. MediaItems: `uri = song.filePath`, metadata `title = song.name` (normalized lowercase title), `artworkUri = song.artPath.toUri()`, extras `songId`. **Artist is not set in metadata.**
4. `setMediaItems(items, startIndex, 0L)`; `prepare()`; `play()`. Replaces the whole queue every time; no add-to-queue / play-next / queue editing (TODO in code).
- Callers (HomeScreen): play all songs list or a playlist's songs from a tapped index; "currently playing" in UI is detected by `song.name == uiState.currentTitle` (name match, not id).

### 4.3 Controls (PlaybackControllerViewModel)
- `pause()`: toggle — `isPlaying ? pause() : play()`.
- `next()`: `seekToNext()`; `previous()`: `seekToPrevious()`.
- `toggleRepeatMode()`: OFF(0) → ALL(2) → ONE(1) → OFF. UI icon dim when `repeatMode == 0`.
- `toggleShuffleMode()`: flip `shuffleModeEnabled`.
- No seek-by-drag (FillBar is display only — TODO), no volume control.

ExoPlayer semantics to emulate on desktop:
- **Auto-advance at track end**: OFF → next item; after last item playback ends (state ENDED, `isPlaying=false`). ALL → wrap to first (in current order). ONE → restart same item (fires a media-item transition with reason REPEAT).
- **`seekToNext()`**: if a next item exists → go to it. Navigation treats ONE as OFF (Media3 `getRepeatModeForNavigation`), i.e. in ONE mode next moves to the next track; on the last track with OFF/ONE → no-op; with ALL → wraps to first.
- **`seekToPrevious()`**: if `currentPosition <= 3000 ms` (Media3 default `maxSeekToPreviousPositionMs`) and a previous item exists → previous item; otherwise seek to 0 of current. Previous-exists follows the same rules (ALL wraps from first to last; OFF/ONE none at first).
- **Shuffle**: ExoPlayer `DefaultShuffleOrder` — a random permutation of the queue generated when items are set; toggling shuffle does not reshuffle, it just switches navigation to follow the permutation starting from the current item. Next/prev/auto-advance/wrap use the permutation when enabled. Desktop: on queue set, build `shuffleOrder = indices.shuffled()`; keep it until the next `startPlayback`.
- After ENDED (repeat OFF), `play()` via the session restarts the current (last) item from 0.

### 4.4 Exposed state — `PlayerUiState`
```kotlin
data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentSongId: Int? = null,   // never assigned anywhere (always null)
    val currentTitle: String? = null, // from media metadata title (= Song.name)
    val repeatMode: Int = 0,          // Player constants: 0 OFF, 1 ONE, 2 ALL
    val shuffleMode: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0
)
```
Plus `controller: StateFlow<MediaController?>` and `sleepTimerState`.

Listener updates:
- `onIsPlayingChanged(true)` → `isPlaying=true`, start position polling + listen tracking. `false` → stop polling, `flushListenTime()`, stop tracking.
- `onMediaItemTransition(item)` → `flushListenTime()`, `lastTrackedSongId = extras.songId`, `accumulatedMs = 0`, `lastPositionMs = 0`, uiState `currentTitle = item.title`, `positionMs = 0`.
- `onRepeatModeChanged`, `onShuffleModeEnabledChanged` → mirror into uiState.

**Position polling**: while playing, every **500 ms**: `positionMs = currentPosition`, `durationMs = duration.coerceAtLeast(0)` unless `C.TIME_UNSET` (keep previous). Stops when paused (position freezes at last poll). Desktop: `MediaPlayer.currentTime.toMillis()`, `media.duration` (UNKNOWN until ready → keep previous).

Note: `isActive` is imported from `kotlinx.coroutines.NonCancellable.isActive` (always true) — loops only exit through cancellation at `delay()`. Harmless; on desktop use the coroutine's own `isActive`.

### 4.5 Listen-time tracking (exact rules)
State: `lastTrackedSongId`, `accumulatedMs`, `lastPositionMs`, `activePlaylistId`, `activePlaylistName`, `songDetailsById`.

- Tracking job (started on every `isPlaying=true`, cancelling any previous): loop `{ delay(7 s); pos = currentPosition; delta = (pos - lastPositionMs).coerceIn(0, 15_000); accumulatedMs += delta; lastPositionMs = pos; flushListenTime() }`.
  - Backward seek → delta 0. Forward seek/skip-ahead counts at most 15 s per tick.
  - Time between the last tick and a pause is *not* lost: `lastPositionMs` persists across pause, so the first tick ≥7 s after resume counts it (still capped 15 s).
  - Time between the last tick and a **track transition** is lost (transition resets `lastPositionMs=0`, and `accumulatedMs` is always 0 between ticks, so the flush on pause/transition is effectively a no-op).
- `flushListenTime()`: return if no `lastTrackedSongId` or `accumulatedMs <= 0`; `ms = accumulatedMs; accumulatedMs = 0`; on IO:
  1. `UPDATE Song SET timeHeard = timeHeard + :ms WHERE id = :songId`
  2. if `activePlaylistId != null`: `UPDATE Playlist SET timeHeard = timeHeard + :ms WHERE id = :id` (credited to the playlist the queue was started from, even for songs later removed from it)
  3. insert `ListeningSession(songId, playlistId, songName = snapshot.name, artistName = snapshot.artist, playlistName, songArtPath = snapshot.artPath, songLength = snapshot.length ?: 0, startTimeMs = now - ms, durationMs = ms)` — i.e. one ~7 s row per tick (denormalized so stats survive deletes).
- Not counted while paused; position-based, so playback speed/latency doesn't matter.

### 4.6 Audio focus / becoming-noisy / MediaSession metadata
- ExoPlayer built with defaults → `handleAudioFocus = false` (no `setAudioAttributes(..., true)`), `setHandleAudioBecomingNoisy` not set (false), no wake mode. So the app **does not** duck/pause for other audio and **does not** pause on headphone unplug. Desktop: nothing required (Windows has no audio focus); optional: pause on default output device change.
- MediaSession metadata = title (normalized name) + artwork URI only; Media3 default notification, lock-screen and hardware media-button handling.
- Desktop equivalents (optional, not 1:1 required): Windows System Media Transport Controls (SMTC) via a WinRT bridge/JNA library to show title/artwork and receive media keys; or global media-key capture (e.g. JNativeHook / `RegisterHotKey` for VK_MEDIA_*); tray icon for background playback. App-window close behaviour replaces the foreground service lifecycle.

### 4.7 JavaFX implementation sketch (to match ExoPlayer)
- One `MediaPlayer` per track (`Media(File(path).toURI().toString())`); on track change `dispose()` old, create new, `play()`.
- `setOnEndOfMedia` → auto-advance per §4.3 (ONE: `seek(Duration.ZERO)`; or `cycleCount` handling). Fire the "transition" hook (flush + reset tracking, `currentTitle`, `positionMs=0`) on every item change *including* ONE-repeat.
- `statusProperty` PLAYING ↔ others → drive `isPlaying` (start/stop 500 ms poll + 7 s tracker).
- `setOnError` / `Media.onError` (e.g. missing file because ffmpeg failed): ExoPlayer would surface a player error and stop; decide skip-to-next vs stop.
- `durationMs` from `media.duration` once READY.

---

## 5. Sleep timer

- Options (`SleepTimerDuration`, label): `FIVE_MINUTES(5,"5 minutes")`, `TEN_MINUTES(10,"10 minutes")`, `FIFTEEN_MINUTES(15,"15 minutes")`, `THIRTY_MINUTES(30,"30 minutes")`, `FORTY_FIVE_MINUTES(45,"45 minutes")`, `SIXTY_MINUTES(60,"60 minutes")`; `toMillis() = minutes*60*1000`.
- State: `SleepTimerState(isActive = false, duration: SleepTimerDuration? = null, remainingMs = 0)` as `StateFlow`; surfaced via `MediaControllerRepository.sleepTimerState` → ViewModel → `SleepTimerDialog` (shows `MM:SS` = `remainingMs/60000`, `(remainingMs%60000)/1000`, "Timer Active", "CANCEL TIMER"; else duration buttons).
- `startTimer(duration, onComplete)`: `cancelTimer()` first (only one timer), set active state, coroutine on `Dispatchers.Default`: wall-clock `endTime = now + totalMs`; loop every **1000 ms**: `remaining = (endTime - now).coerceAtLeast(0)` → state; when `remaining <= 0` → reset state to default, invoke `onComplete`, stop.
- `onComplete` (from `MediaControllerRepository.startSleepTimer`) = `controller.pause()` — **immediate pause, no fade, not "end of current song"**, queue/position retained. Runs regardless of whether music is playing (counts down while paused too).
- `cancelTimer()`: cancel job, reset state, clear callback.
- `extendTimer(additionalMinutes)`: adds to `remainingMs` in state only — **ineffective** because the loop recomputes from the fixed `endTime` next tick; also not called from UI. Port as-is or fix by moving `endTime` (no visible behaviour depends on it).
- Desktop: identical pure-Kotlin coroutine code; `onComplete` → `PlayerEngine.pause()` (on FX thread).

---

## 6. Android API → desktop equivalent

| Android | Used for | Desktop (Kotlin/JVM, Windows) |
|---|---|---|
| Chaquopy `Python.start/getModule/callAttr`, `PyObject.callAttr("get", key)` | yt-dlp in-process | `ProcessBuilder(yt-dlp.exe …)` + parse `-J` JSON with kotlinx.serialization |
| yt-dlp Python API options | download | CLI flags in §1.10 |
| `com.arthenica.ffmpegkit.FFmpegKit.execute`, `ReturnCode.isSuccess`, `getAllLogsAsString` | MP3 conversion | `ProcessBuilder(ffmpeg.exe …)`, `waitFor() == 0`, captured output |
| `Context.getExternalFilesDir(null)` | song + thumbnail dir | `%LOCALAPPDATA%\Musitify\songs` (or user-selectable) |
| `Context.filesDir` | `playlist_covers/` | `%APPDATA%\Musitify\playlist_covers` |
| `android.graphics.Bitmap/Canvas/Rect/BitmapFactory`, `Bitmap.compress(PNG,100)` | collage | Skia (`org.jetbrains.skia.Surface/Image/Canvas`) — decodes WebP; or AWT `BufferedImage` + TwelveMonkeys WebP plugin |
| `MediaMetadataRetriever` | duration backfill | ffmpeg `-i` stderr parse / JavaFX `Media.duration` / jaudiotagger |
| `android.util.Log` | logging | SLF4J/println/java.util.logging |
| Media3 `ExoPlayer` | playback | JavaFX `MediaPlayer` + own queue/repeat/shuffle logic (§4.3) |
| Media3 `MediaSessionService`, `MediaSession`, `MediaController`, `SessionToken`, `ListenableFuture` | background service, notification, lock-screen, controller IPC | not needed; singleton engine in-process; optional SMTC/tray/global media keys |
| `Player.REPEAT_MODE_*`, `C.TIME_UNSET` | constants | own enum/ints (keep 0/1/2 if persisted or shown) |
| `MediaItem`/`MediaMetadata` (title, artworkUri, extras songId) | queue items | data class `QueueItem(songId, title, filePath, artPath)` |
| `PendingIntent` session activity | tap notification → open app | tray/SMTC click → bring window to front |
| `androidx.core.net.toUri` | artwork URI | `File(path).toURI()` |
| Manifest perms `FOREGROUND_SERVICE(_MEDIA_PLAYBACK)`, `POST_NOTIFICATIONS`, `INTERNET`, `usesCleartextTraffic` | service/notification/network | none (Windows firewall prompt possible for HTTP sync, not for yt-dlp) |
| Hilt `@Singleton`/`@Inject`, `@ApplicationContext` | DI | manual singletons or Koin |
| Room DAOs (`addListenTime`, `getSongByUrl`, `getSongByName`, `getSongsByUrls`, `addSong` IGNORE, `updateArt`, `updateLength`) | persistence | same SQL on SQLDelight / Room-KMP / Exposed (see data map) |
| `Dispatchers.Main` | controller callbacks | `Dispatchers.Main` (Swing via kotlinx-coroutines-swing) + `Platform.runLater` for JavaFX calls |

---

## 7. Known bugs / quirks summary (decide: preserve vs fix)

1. Artist fallback never reaches `uploader`/`"Unknown Artist"` (`str(None)` = `"None"`); list-valued `artists` stored as Python repr.
2. Failed single downloads report COMPLETED; DownloadsManager can't show song errors.
3. ffmpeg failure still creates a DB row pointing at a missing `.mp3`; file-wait timeout creates `filePath = "<base>/.mp3"`.
4. `artPath` always `.webp` regardless of actual thumbnail extension.
5. Cancel doesn't stop an in-flight download (desktop can kill the process).
6. Playlist progress `total` includes pre-existing songs; pre-existing songs are appended *after* new ones in playlist positions; playlist with same name is merged into.
7. Playlist cover never regenerated once the PNG exists; quadrant source order = DB `IN` order.
8. `currentSongId` never set; UI matches now-playing by normalized name (duplicates collide).
9. Listen time between last 7 s tick and a track change is dropped.
10. `extendTimer` is a no-op.
11. `"nana"` substring removal in normalizer mangles titles (`banana` → `ba`).
12. Normalizer removes all non-ASCII letters — non-Latin titles normalize to `""` and collide in `getSongByName` legacy lookup (only matters if the existing song's url contains `"legacy"`).
