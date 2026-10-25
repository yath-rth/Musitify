# Sync map (Android → Desktop reimplementation reference)

Source root: `Android/app/src/main/java/com/yatharth/musitify`

Files covered (all read in full):
`network/SyncController.kt`, `network/ActionController.kt`, `network/model/{ActionPayloads,LibrarySyncPayloads,NAction,NSong,NPlaylist,MPlaylist_Song}.kt`,
`service/RemoteChangesManager.kt`, `service/IdManager.kt`, `data/repository/ActionRepository.kt`, `data/dao/SyncActionDao.kt`,
`data/entity/SyncAction.kt`, plus the parts of `SongRepository`, `PlaylistRepository`, `SettingsRepository`, `SettingsDao`,
`Settings`, `Song`, `Playlist`, `SongToPlaylist`, their DAOs, `UUIDConverter`, migrations 7→8, `MainActivity`,
`HomeScreenViewModel`, `HomeScreen`, `DownloadsManager`, `LegacyFixer` that touch sync.

Libraries: OkHttp `com.squareup.okhttp3:okhttp:5.4.0`, `kotlinx-serialization-json:1.11.0`, Kotlin serialization plugin 2.4.10.
Manifest: `INTERNET` permission, `android:usesCleartextTraffic="true"` (plain HTTP).

---

## 1. Two sync systems

| System | Class | Base URL | Status |
|---|---|---|---|
| **Action log (event sourcing)** – the current one | `ActionController` | `http://localhost:8080/actions` | Code is live, but **no UI calls it** (see §9) |
| **Library snapshot ("bootstrap")** – legacy | `SyncController` | `http://localhost:8080/sync` | **Disabled**: calls are commented out |

Both use a private `OkHttpClient()` with default settings (no timeouts set, so OkHttp's 10 s connect/read/write defaults apply), and the media type
`"application/json; charset=utf-8"`. Neither sends auth headers or custom headers. OkHttp adds its usual headers (`User-Agent: okhttp/5.4.0`,
`Accept-Encoding: gzip`, `Content-Type: application/json; charset=utf-8` on POST). The host is hard-coded `localhost:8080`. On an Android device that
needs `adb reverse tcp:8080 tcp:8080`. On desktop it reaches the local machine directly.

### Json configuration (important for wire compatibility)

- `ActionController` and `SyncController` use the **default `Json` object** (`kotlinx.serialization.json.Json`, imported as the companion):
  - `ignoreUnknownKeys = false`: **any extra key in a server response makes decoding throw**. The exception is caught and logged, and the whole pull is dropped.
  - `encodeDefaults = false`: **properties whose value equals their declared default are left out when encoding** (see §3.1).
  - `explicitNulls = true`, `coerceInputValues = false`, `isLenient = false`, and `classDiscriminator = "type"`, though nothing uses it
    because **there is no polymorphism**. `type` is a plain enum property.
  - Enums serialize as their constant **name** string (`"SONG_LIKED"`). An unknown enum string in a response makes decoding throw.
- `ActionRepository` declares `private val json = Json { ignoreUnknownKeys = true }`, but **nothing uses it**.
- Action payloads are built with `buildJsonObject { put(...) }.toString()` (compact JSON, keys in insertion order). They are parsed with
  `Json.parseToJsonElement(payload).jsonObject` and `jsonPrimitive.content`.

---

## 2. Payload classes (verbatim)

### `data/entity/SyncAction.kt` (Room entity, also the push wire type)
```kotlin
package com.yatharth.musitify.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

enum class ActionType {
    SONG_LIKED,
    SONG_UNLIKED,
    SONG_RENAMED,
    PLAYLIST_CREATED,
    PLAYLIST_DELETED,
    PLAYLIST_RENAMED,
    SONG_ADDED_TO_PLAYLIST,
    SONG_REMOVED_FROM_PLAYLIST,
    PLAYLIST_SONG_REORDERED
}

@Entity(
    tableName = "Actions",
    indices = [
        Index(value = ["actionUuid"], unique = true),
        Index(value = ["isSynced"])
    ]
)
@Serializable
data class SyncAction(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val actionUuid: String = UUID.randomUUID().toString(),
    val type: ActionType,
    val songUuid: String? = null,
    val playlistUuid: String? = null,
    val payload: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
```
`ActionType` has no `@Serializable` or `@SerialName`. It uses the default enum serializer, so names go on the wire as-is.

### `network/model/ActionPayloads.kt`
```kotlin
package com.yatharth.musitify.network.model

import com.yatharth.musitify.data.entity.SyncAction
import kotlinx.serialization.Serializable

@Serializable
data class PushActionsPayload(
    val deviceId: String,
    val actions: List<SyncAction>
)

@Serializable
data class FetchActionsPayload(
    val actions: List<NAction>
)
```

### `network/model/NAction.kt`
```kotlin
package com.yatharth.musitify.network.model

import com.yatharth.musitify.data.entity.ActionType
import com.yatharth.musitify.data.entity.SyncAction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NAction(
    @SerialName("localId") val localId: Int = 0,
    @SerialName("actionUuid") val uuid: String = "",
    @SerialName("type") val type: ActionType = ActionType.SONG_LIKED,
    @SerialName("songUuid") val songUuid: String? = null,
    @SerialName("playlistUuid") val playlistUuid: String? = null,
    @SerialName("payload") val payload: String? = null,
    @SerialName("createdAt") val createdAt: Long = System.currentTimeMillis()
) {
    fun toSyncAction(): SyncAction = SyncAction(
        actionUuid = uuid,
        type = type,
        songUuid = songUuid,
        playlistUuid = playlistUuid,
        payload = payload,
        createdAt = createdAt,
        isSynced = true
    )
}
```
Note: `toSyncAction()` drops `localId`, so the resulting row gets a fresh autoincrement id. NAction has **no `isSynced` or `deviceId` field**. If the
server sends either one, decoding fails because `ignoreUnknownKeys` is false.

### `network/model/LibrarySyncPayloads.kt`
```kotlin
package com.yatharth.musitify.network.model

import kotlinx.serialization.Serializable

@Serializable
data class PushLibraryPayload(
    val device_id: String,
    val playlists: List<NPlaylist>,
    val songs: List<NSong>,
    val playlist_songs: List<MPlaylist_Song>,
)

@Serializable
data class FetchLibraryPayload(
    val playlists: List<NPlaylist>,
    val likedSongs: List<NSong>,
    val playlist_songs: List<MPlaylist_Song>,
)
```

### `network/model/NSong.kt`
```kotlin
@Serializable
data class NSong(
    val id: String,
    val title: String,
    val artist: String,
    val url: String,
    val downloaded_at: Long
)
```

### `network/model/NPlaylist.kt`
```kotlin
import androidx.media3.extractor.mp4.Track   // unused import
@Serializable
data class NPlaylist(
    val id : String,
    val name: String,
    val created_on: Long
)
```

### `network/model/MPlaylist_Song.kt`
```kotlin
@Serializable
data class MPlaylist_Song(
    val song_id: String,
    val playlist_id: String,
    val position: Int,
    val added_on: Long
)
```
(All of them are in package `com.yatharth.musitify.network.model` and import `kotlinx.serialization.Serializable`.)

---

## 3. Action-log endpoints (`ActionController`)

### 3.1 `POST http://localhost:8080/actions`: `pushActions()`

1. Reads the `device_id` setting. If it is missing, it generates `UUID.randomUUID().toString()`, stores it with `settingsRepository.insertSetting("device_id", id)`, and uses that.
2. `unsyncedActions = syncActionDao.getUnsynced()`, which runs `SELECT * FROM Actions WHERE isSynced = 0 ORDER BY localId ASC`.
3. Builds `PushActionsPayload(deviceId = device_id, actions = unsyncedActions)` and encodes it with the default `Json`.
4. Sends the POST **even if `actions` is empty**.
5. If the response is non-2xx, it throws `IOException("Sync failed: <code> <body>")`, which is caught and logged with `Log.e`. Nothing else happens.
6. On 2xx it reads the body as a string and `println`s it. **The response is not parsed**, so its shape doesn't matter.
7. Then it calls `actionRepository.markSynced(uuids)`, **which is a no-op**:
   ```kotlin
   suspend fun markSynced(actionUuids: List<String>) {
       //IMPORTANT: Unmark when pushing or creating a prod version
   //        if (actionUuids.isNotEmpty()) syncActionDao.markSynced(actionUuids)
   }
   ```
   As a result, **every push re-sends every action ever recorded locally**. The server must dedupe on `actionUuid`.
   (Intended behaviour: `UPDATE Actions SET isSynced = 1 WHERE actionUuid IN (...)`.)

**Request body JSON.** The keys are camelCase, because `PushActionsPayload` and `SyncAction` have no `@SerialName`:
```json
{
  "deviceId": "3f0c…-uuid",
  "actions": [
    {
      "localId": 12,
      "actionUuid": "a1b2…",
      "type": "SONG_ADDED_TO_PLAYLIST",
      "songUuid": "…",
      "playlistUuid": "…",
      "payload": "{\"url\":\"https://www.youtube.com/watch?v=…\",\"title\":\"…\",\"artist\":\"…\",\"downloaded_at\":1729000000000,\"position\":3,\"added_on\":1729000000000}",
      "createdAt": 1729000000000
    }
  ]
}
```
Encoding quirks caused by `encodeDefaults = false`:
- `localId`: always present, because DB rows have a value of 1 or more, which differs from the default 0.
- `actionUuid`: always present. The default expression is re-evaluated to a fresh random UUID, so it is never equal.
- `type`: always present (it has no default).
- `songUuid`, `playlistUuid`, `payload`: **left out when null**. They are never sent as `null`.
- `createdAt`: present in practice. The comparison is against `System.currentTimeMillis()` at encode time.
- `isSynced`: **left out**, because unsynced rows equal the default `false`. It would only appear as `true` if a synced row were ever pushed.
- `payload` is a **JSON string containing serialized JSON**, not a nested object.

A desktop client that wants byte-level parity should use `Json { encodeDefaults = false }` with the same data class. The simplest equivalent is to
emit `localId, actionUuid, type, [songUuid], [playlistUuid], [payload], createdAt` and leave out the nulls and `isSynced`.

### 3.2 `GET http://localhost:8080/actions?lastFetchedActionId=<n>`: `pullActions()`

1. `lastFetchedId = actionRepository.getLastCursor()` returns the `sync_cursor` setting as a Long, or `0L`.
2. The URL is built with `HttpUrl.newBuilder().addQueryParameter("lastFetchedActionId", lastFetchedId.toString())`.
   The only query param is `lastFetchedActionId` (decimal). No body, no device id.
3. If the response is non-2xx, it throws `IOException("Fetch failed <body>")`, which is caught and logged. Nothing is applied.
4. The body is decoded as `FetchActionsPayload` with the default `Json` (strict). The expected response is:
   ```json
   {
     "actions": [
       {
         "localId": 57,
         "actionUuid": "…",
         "type": "PLAYLIST_CREATED",
         "songUuid": null,
         "playlistUuid": "…",
         "payload": "{\"name\":\"Gym\",\"created_at\":1729000000000}",
         "createdAt": 1729000000000
       }
     ]
   }
   ```
   - The top-level object must have exactly `actions`. **No other keys** are allowed (for example `cursor` or `count` would break decoding).
   - Each action may contain only `localId` (Int, the **server sequence id**), `actionUuid`, `type`, `songUuid`, `playlistUuid`, `payload` (string or null) and `createdAt`.
     Every field is optional. A missing `type` defaults to `SONG_LIKED` (dangerous). Explicit `null` is fine for the nullable fields.
   - `payload` must be a JSON **string**. A nested object fails to decode.
5. `syncActions = nActions.map { it.toSyncAction() }`, with `isSynced = true`.
6. `actionRepository.insertRemoteActions(syncActions)` is **commented out**, so remote actions are *not* stored in the local Actions table and are
   not deduplicated against local history.
7. If the list is non-empty, it calls `remoteChangesManager.apply(syncActions)`, which applies them sequentially in server order (§5).
8. `maxId = nActions.maxOfOrNull { it.localId }`. The `actionRepository.setCursor(maxId.toLong())` call is **commented out**.

The server is presumably expected to return actions with server id `> lastFetchedActionId`, and probably to exclude the caller's own device. The client
cannot know about its own device because no device id is sent on GET. The client does nothing to filter its own echoed actions.

### 3.3 Cursor handling

```kotlin
suspend fun getLastCursor(): Long =
    settingsRepository.getSettingByName("sync_cursor")?.value?.toLongOrNull() ?: 0L

suspend fun setCursor(cursor: Long) {
    settingsRepository.insertSetting("sync_cursor", cursor.toString())
}
```
- It is stored in the `Settings` table (`id`, `name` UNIQUE, `value` TEXT, `updated_on` ms) under the name **`sync_cursor`**. The value is a decimal string.
- The intended semantics are "max server `localId` seen". It is **never written** at the moment (the call is commented out), so the client **always sends
  `lastFetchedActionId=0`** and re-pulls and re-applies the whole server log on every pull.
- `SettingsRepository.insertSetting` does `@Insert(onConflict = REPLACE)` with `id = 0`. The unique index on `name` makes it delete and re-insert the row (upsert).

---

## 4. Action types: recording (outbox)

Every recording goes through `ActionRepository.record()`, except `likedSongs()`, which calls `syncActionDao.insertAll()` directly. Both use `@Insert(onConflict = IGNORE)`.
A new row gets `actionUuid = UUID.randomUUID()`, `createdAt = now`, and `isSynced = false`.

All payloads are compact JSON strings. Numbers are JSON numbers and strings are JSON strings.

| Type | songUuid | playlistUuid | payload JSON (key order as sent) | Recorded by (record=true path) | UI trigger |
|---|---|---|---|---|---|
| `SONG_LIKED` | song.uuid (null if legacy song with no uuid → field omitted) | – | `{"url","title","artist","downloaded_at"}` (`title` = local `Song.name`, already normalized) | `ActionRepository.likedSong(song)` ← `SongRepository.downloadSong(record=true)` after a new song is inserted (not on the early "url already exists" return, not on the legacy-url-merge branch). Bulk: `likedSongs(list)` ← `SongRepository.downloadSongsFromSync` (always records for every newly obtained song, including the legacy-merge ones) | Download song (DownloadsManager `DownloadType.SONG`), download playlist (`DownloadType.PLAYLIST` → `downloadPlaylist` → `downloadSongsFromSync`) |
| `SONG_UNLIKED` | song.uuid.toString() (`"null"` string if uuid null) | – | none | `unlikedSong` ← `SongRepository.deleteSongById(record=true)` / `deleteSongByUUID(record=true)` (recorded **after** the DB delete) | Long-press song → delete confirm (`HomeScreenViewModel.deleteSong`) |
| `SONG_RENAMED` | song.uuid | – | `{"old_name","new_name"}` | `recordSongRenamed` ← `SongRepository.updateSong(record=true)` only if the name changed (recorded before the update) | Rename song dialog (`renameSong`) |
| `PLAYLIST_CREATED` | – | playlist.uuid | `{"name","created_at"}` | `recordPlaylistCreated` ← `PlaylistRepository.createPlaylist(record=true)` only when no playlist with that **name** exists | Create playlist dialog (`createPlaylistAndAddSongs`), playlist download |
| `PLAYLIST_DELETED` | – | playlist.uuid | none | `recordPlaylistDeleted` ← `deletePlaylistById(record=true)` / `deletePlaylistByUuid(record=true)` (recorded **before** the delete) | Playlist delete (`deletePlaylist`) |
| `PLAYLIST_RENAMED` | – | playlist.uuid | `{"old_name","new_name"}` | `recordPlaylistRenamed` ← `PlaylistRepository.updatePlaylist(record=true)` if the name changed | Rename playlist dialog |
| `SONG_ADDED_TO_PLAYLIST` | song.uuid.toString() (`"null"` if null) | playlist.uuid | `{"url","title","artist","downloaded_at","position","added_on"}` | `recordSongAddedToPlaylist` ← `PlaylistRepository.addSongsToPlaylist` once per valid song (no record flag, **always records**). `position = getLastPosition(playlistId)+1+i` (0-based; empty playlist → 0), `added_on = now` (separate `currentTimeMillis()` from the relation row's `addedOn`). Skipped if `songs[songId]` is null | Create playlist with songs, add-to-playlist dialog, playlist download, **and remote apply of SONG_ADDED_TO_PLAYLIST (echo)** |
| `SONG_REMOVED_FROM_PLAYLIST` | song.uuid | playlist.uuid | none | `recordSongRemovedFromPlaylist` ← `PlaylistRepository.removeSong(songId, playlistId)` (recorded before the delete; throws "Song not in playlist" first if the relation is missing) | Remove song from playlist (`deleteSongFromPlaylist`) |
| `PLAYLIST_SONG_REORDERED` | moved song.uuid | playlist.uuid | `{"from_position","to_position"}` (0-based **list indexes** into the playlist ordered by `position`, not stored position values) | `recordPlaylistSongReordered` ← `reorderSongsInPlaylist(record=true)`, only when indexes are in range and the playlist and song are found; recorded after positions are rewritten 0..n-1 | Drag reorder in the playlist view (`HomeScreen` line ~315 → `reorderSongsInPlaylist`) |

Recording happens in the repositories, not in the UI. A Desktop port should record at the same repository-level points.

Unused DAO helpers: `getAll()`, `getByActionUuid`/`hasAction`, `pruneSynced()` (`DELETE FROM Actions WHERE isSynced = 1`), `insertRemoteActions` (dedupes by actionUuid,
then `insertAll`).

### Actions table schema (migration 7→8)
```sql
CREATE TABLE IF NOT EXISTS `Actions` (
  `localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  `actionUuid` TEXT NOT NULL,
  `type` TEXT NOT NULL,          -- enum name
  `songUuid` TEXT,
  `playlistUuid` TEXT,
  `payload` TEXT,                -- JSON string
  `createdAt` INTEGER NOT NULL,  -- epoch ms
  `isSynced` INTEGER NOT NULL    -- 0/1
);
CREATE UNIQUE INDEX IF NOT EXISTS `index_Actions_actionUuid` ON `Actions` (`actionUuid`);
CREATE INDEX IF NOT EXISTS `index_Actions_isSynced` ON `Actions` (`isSynced`);
```

---

## 5. Applying pulled actions: `RemoteChangesManager.apply(actions)`

The actions are processed in a simple `for` loop in response order, and each one is wrapped in `try/catch (Exception)`. A failure is logged
(`Log.d("RemoteChanges", msg)`) and the loop **skips to the next action**. There is no transaction, no retry, and no dedupe by actionUuid. Remote
applies pass `record = false` where the API allows it, so most of them don't echo into the outbox. The exception is the echo case flagged below.

### SONG_LIKED → `SongRepository.addSongFromSync(songUuid, payload)`
1. If `songUuid` or `payload` is null, it throws `IOException`.
2. It parses the payload. `url` is required (otherwise it throws). `downloaded_at` is read as `jsonPrimitive.content.toLongOrNull()` and falls back to `now`.
   **`title` and `artist` from the payload are ignored.**
3. It calls `downloadSong(url, downloaded_at, songUUID = UUID.fromString(songUuid), record = false)`:
   - **Idempotency:** if a song with the same `url` already exists, it returns that song immediately. The local uuid is **not** reconciled with the remote one.
   - Otherwise it downloads through Python `downloader.downloadSong(url, baseDir)` (yt-dlp) and takes the title, artist and duration from the download result.
     The title goes through `SongNormalizer.normalize(...).normalizedTitle`.
   - Legacy merge: if there is a song with the same normalized name whose url contains `"legacy"`, it updates that row's url, deletes the freshly
     downloaded `.mp3`/`.webp`, and returns. The old row keeps its old uuid.
   - Otherwise it inserts a `Song` with the remote uuid (`addSong` uses IGNORE; on -1 it falls back to lookup by url).
   - Download errors are caught inside, and the method returns null. Nothing is recorded.

### SONG_UNLIKED → `songRepository.deleteSongByUUID(id = action.songUuid.toString(), record = false)`
- If there is no song with that uuid (including a null uuid, which becomes the string `"null"`), it throws "Invalid song uuid", which is logged and skipped. **This
  makes the action idempotent.**
- Otherwise it runs `DELETE FROM Song WHERE uuid = ?`, which cascades to the `SongToPlaylist` rows through the FK on `songId`, and deletes the audio file and art file.

### SONG_RENAMED: **not implemented** (logs only). TODO in the code.

### PLAYLIST_CREATED → `PlaylistRepository.createPlaylistFromSync(playlistUuid, payload)`
1. If `playlistUuid` or `payload` is null, it throws.
2. `name` is required. `created_at` uses `toLongOrNull()` and falls back to `now`.
3. It calls `createPlaylist(name, uuid = UUID.fromString(playlistUuid), record = false, createdAt)`.
   - **Idempotency is by name:** if a playlist with that name exists, it returns the existing one, whose uuid may differ from the remote one.
   - Otherwise it inserts `Playlist(name, timeHeard = 0, createdAt, uuid)`. Cover art is not generated here.

### PLAYLIST_DELETED → `deletePlaylistByUuid(uuid = playlistUuid, record = false)`
- A null uuid throws. If no playlist has the uuid, it throws "Invalid playlist uuid", which is skipped (idempotent).
- `DELETE FROM Playlist WHERE uuid = ?`, which cascades to the relations. The cached cover PNG is not removed.

### PLAYLIST_RENAMED: **not implemented** (logs only).

### SONG_ADDED_TO_PLAYLIST → `addSongToPlaylistSync(songUuid, playlistUuid, payload)`
1. Looks up the song by uuid. A null or missing song throws "Song doesnt exist". The song is **not** downloaded from the payload's url.
2. Looks up the playlist by uuid. A missing playlist throws.
3. Parses `position` (`content.toInt()`, which throws on a non-int, and falls back to 0). **The parsed value is then unused.**
4. Calls `addSongsToPlaylist(playlist, mapOf(song.id to song))`:
   - It re-validates that the playlist exists **by name** and that the song exists.
   - It appends at `getLastPosition + 1` (the payload position is ignored), with `addedOn = now`.
   - **It records a new `SONG_ADDED_TO_PLAYLIST` action (echo bug).** `addSongsToPlaylist` has no `record` flag.
   - It inserts with `REPLACE`. `SongToPlaylist` has **no unique constraint on (songId, playListId)**, so every re-apply **creates a duplicate
     relation row**. Because the cursor is always 0, every pull re-applies this action.
   - It does not regenerate playlist art.

### SONG_REMOVED_FROM_PLAYLIST → `removeSongFromPlaylistSync(songUuid, playlistUuid)`
- Looks up the song and playlist by uuid (throws if missing).
- Finds the relation by `song_uuid` and `playlist_uuid` (the denormalized uuid columns on `SongToPlaylist`; the first match).
  If there is none, it throws "Song not in playlist", which is skipped (idempotent).
- Deletes that single relation row. The remaining positions are **not** compacted. Nothing is recorded.

### PLAYLIST_SONG_REORDERED → `reorderSongInPlaylistSync(songUuid, playlistUuid, payload)`
1. If any of the three is null, it throws. If the song or playlist is missing by uuid, it throws.
2. Reads `from_position` and `to_position` (`toInt()`, default 0). **`songUuid` is only used for the existence check.** The move is index-based.
3. Calls `reorderSongsInPlaylist(playlist.id, from, to, record = false)`. It loads the relations ordered by `position`; if either index is out of range,
   it does nothing. Otherwise it does `removeAt(from)` then `add(to, item)` and rewrites positions to `0..n-1` (keyed by songId and playlistId).
   - **This is not idempotent.** Re-applying it moves the item again.

---

## 6. device_id

- Setting name: **`device_id`** in the `Settings` table. The value is `UUID.randomUUID().toString()` (v4, lowercase, with hyphens).
- `MainActivity.onCreate` checks `settingsRepository.hasUserOpenedApp()` (setting `hasOpened` == `"true"`). If it is false, it calls
  `IdManager.generateDeviceId()` and then `settingsRepository.openedApp()`, which writes `hasOpened="true"`.
  - `IdManager.generateDeviceId()` writes `Settings(name="device_id", value=uuid, updated_on=now)` through REPLACE, so it **overwrites** any existing value.
- Fallback: `pushActions()` and `SyncController.sendData()` create and store one lazily if the setting is missing.
- It is sent only in the POST bodies: `"deviceId"` (actions) and `"device_id"` (bootstrap). It is **not** sent on GETs.
- `IdManager` also injects `SongDao`, `PlaylistDao` and `SongToPlaylistDao`, but doesn't use them.

## 7. UUID generation for songs, playlists and relations

- Room stores UUIDs as TEXT through `UUIDConverter` (`uuid.toString()`, and an empty string reads back as null). Columns: `Song.uuid` (UNIQUE index),
  `Playlist.uuid` (no unique index; `Playlist.name` is UNIQUE), `SongToPlaylist.song_uuid` and `playlist_uuid` (denormalized copies).
- Songs: `downloadSong(songUUID: UUID = UUID.randomUUID())` assigns a new random v4 for user downloads. A remote apply uses the remote uuid.
  `downloadPlaylist` gives each track `NSong(id = UUID.randomUUID().toString())`, and `downloadSongsFromSync` stores `UUID.fromString(nSong.id)`.
- Playlists: `createPlaylist(uuid ?: UUID.randomUUID())`.
- Relations: `song_uuid` and `playlist_uuid` are copied from the song and playlist at insert time.
- Actions: `actionUuid = UUID.randomUUID().toString()` (the data-class default).
- Legacy rows (from before migration 2→3) can have a null uuid. `LegacyFixer.generateUUIDForLibrary()` exists, but nothing calls it, and it would not
  work anyway because it uses IGNORE inserts on existing rows. A null uuid is therefore sent as an omitted field (SONG_LIKED, SONG_RENAMED and
  PLAYLIST_* use `uuid?.toString()`) or as the literal string `"null"` (SONG_UNLIKED, SONG_ADDED_TO_PLAYLIST, SONG_REMOVED_FROM_PLAYLIST,
  PLAYLIST_DELETED and PLAYLIST_SONG_REORDERED use `uuid.toString()`).

---

## 8. Legacy bootstrap endpoints (`SyncController`). **Disabled.**

### `POST http://localhost:8080/sync/bootstrap`: `sendData()`
Gets or creates `device_id` (same as §6). The body is `PushLibraryPayload`, and the field order on the wire follows the class: `device_id, playlists, songs, playlist_songs`.
```json
{
  "device_id": "…",
  "playlists": [ { "id": "<playlist uuid>", "name": "…", "created_on": 1729000000000 } ],
  "songs": [ { "id": "<song uuid>", "title": "<Song.name>", "artist": "…", "url": "…", "downloaded_at": 1729000000000 } ],
  "playlist_songs": [ { "song_id": "<uuid>", "playlist_id": "<uuid>", "position": 0, "added_on": 1729000000000 } ]
}
```
- The data comes from `songRepository.allSongs.first()`, `playlistRepository.allPlaylists.first()` and `getAllSongRelations()`.
- The ids use `uuid.toString()`, so a null uuid is sent as `"null"`.
- If the response is non-2xx, it throws and the error is logged. On 2xx the body is `println`ed and not parsed.

### `GET http://localhost:8080/sync`: `receiveData()`
No query params. The response is decoded strictly as `FetchLibraryPayload`:
```json
{ "playlists": [NPlaylist…], "likedSongs": [NSong…], "playlist_songs": [MPlaylist_Song…] }
```
Note that the songs key here is **`likedSongs`**, not `songs`.

Apply steps:
1. Finds existing songs by url. `newSongs` are the `likedSongs` whose `id` is not among the existing songs' uuids.
2. Calls `downloadSongsFromSync(newSongs)`. It skips urls that already exist, downloads the rest with 5 running at once, and inserts them with the remote uuid.
   It **records SONG_LIKED for every one** and returns a map from uuid to Song (new and existing).
3. Playlists: it calls `getAllPlaylistsFrom(payload.playlists.map { it.id })`. That DAO query actually filters by **name** (`WHERE name IN (:names)`), which is a bug,
   so it normally matches nothing. Every playlist then goes to `createPlaylist(name, UUID.fromString(id))` with `record = true` by default. That
   dedupes by name, and it records PLAYLIST_CREATED for the new ones.
4. Relations: it resolves the song by uuid (from the step 2 map) and the playlist by uuid. Unresolved relations or relations with id ≤ 0 are skipped.
   It inserts `SongToPlaylist(position, addedOn = added_on, uuids)` through `addSongPlaylistRelations` (REPLACE, no action recorded).
5. It calls `generatePlaylistArt(id)` for every playlist.

---

## 9. Triggers, UI and bootstrap status

`HomeScreenViewModel`:
```kotlin
fun syncLibrary() {
    viewModelScope.launch {
//            syncController.sendData()
        actionController.pushActions()
        actionController.pullActions()
    }
}

fun fetchLibrary() {
    viewModelScope.launch {
//            syncController.receiveData()
        actionController.pullActions()
    }
}
```
- **Bootstrap sync (`sendData`/`receiveData`) is disabled.** It is commented out, and nothing else calls it.
- **`syncLibrary()` and `fetchLibrary()` are never called from any composable.** A search of `ui/` and the rest of the source found no reference, and there is
  no sync or fetch button in `HomeScreen.kt` (the only `Refresh` icon is the download-retry button). Sync is not run automatically either: there is no
  call at startup, no WorkManager job and no timer. **In the current build, no UI path triggers network sync at all.** The wiring is there but dormant.
- The intended flow is: Sync = push the outbox, then pull and apply. Fetch = pull and apply only.

## 10. Known issues to decide on for the Desktop port (all present in Android today)

1. `markSynced` is a no-op, so the whole outbox is re-pushed every time. Turn it on, or rely on server-side dedupe by `actionUuid`.
2. The cursor is never saved, so the full log is re-pulled every time. Combined with point 3, playlist relations are duplicated on every pull.
3. Applying `SONG_ADDED_TO_PLAYLIST` echoes a new local action, ignores the payload `position`, and inserts duplicate relations because there is no unique (song, playlist) constraint.
4. Remote actions are not stored in the Actions table, so the client never filters its own echoed actions (unless the server does).
5. SONG_RENAMED and PLAYLIST_RENAMED are recorded but not applied.
6. Identity is matched by url for songs and by name for playlists, not by uuid, so uuids can diverge between devices.
7. The strict Json decoding means any extra response field breaks the pull.
8. Reorder is index-based and not idempotent.
9. SONG_ADDED_TO_PLAYLIST doesn't download a missing song even though the payload includes its `url`.
For wire compatibility, the Desktop client must send and accept exactly the JSON shapes in §3 and §8. Fixing points 1 to 4 on the client side doesn't change the wire format.
