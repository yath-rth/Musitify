# Musitify Android → Desktop port map: DATA LAYER

Source root: `Android/app/src/main/java/com/yatharth/musitify` (written as `src/` below).
Files covered: `MusitifyApp.kt`, `data/MusitifyDatabase.kt`, `data/DatabaseModule.kt`, `data/entity/*`, `data/dao/*`,
`data/migrations/*`, `data/repository/{Song,Playlist,Settings,Stats}Repository.kt`, `data/viewmodel/*`,
`util/{Formatter,UUIDConverter,LegacyFixer}.kt`, `service/IdManager.kt`. I also read `ActionRepository.kt`, `util/SongMedia.kt`,
`ui/elements/SortIcon.kt` (where `SortMode` lives), part of `MediaControllerRepository.kt` (where sessions get written) and
`MainActivity.kt` (startup), because the files above depend on them.

---

## 1. Database

```kotlin
@Database(
    entities = [Song::class, Playlist::class, SongToPlaylist::class, Settings::class, SyncAction::class, ListeningSession::class],
    version = 9
)
@TypeConverters(UUIDConverter::class)
abstract class MusitifyDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun songToPlaylistDao(): SongToPlaylistDao
    abstract fun settingsDao(): SettingsDao
    abstract fun syncActionDao(): SyncActionDao
    abstract fun listeningSessionDao(): ListeningSessionDao
}
```
- DB file name: `"musitify_database"`. `exportSchema` is not set, so it uses the default.
- Builder (DatabaseModule, Hilt `@Singleton`): `Room.databaseBuilder(context, MusitifyDatabase::class.java, "musitify_database").addMigrations(MIGRATION_1_2 … MIGRATION_8_9).build()`.
  It has **no** `fallbackToDestructiveMigration`, **no** callbacks and **no** `allowMainThreadQueries`.
- DAOs are provided unscoped as `db.xxxDao()`.

### Room KMP (desktop) equivalent
```kotlin
@Database(entities = [...], version = 9)
@TypeConverters(UUIDConverter::class)
@ConstructedBy(MusitifyDatabaseConstructor::class)
abstract class MusitifyDatabase : RoomDatabase() { /* same abstract DAOs */ }

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object MusitifyDatabaseConstructor : RoomDatabaseConstructor<MusitifyDatabase> { override fun initialize(): MusitifyDatabase }

fun buildDb(appDir: File) = Room.databaseBuilder<MusitifyDatabase>(name = File(appDir, "musitify_database").absolutePath)
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .build()
```
Migrations are only needed if an Android DB file is imported. In Room KMP they take `migrate(connection: SQLiteConnection)` with
`connection.execSQL(...)` and not `SupportSQLiteDatabase`. A fresh desktop DB can start at version 9 with no migrations.

---

## 2. Final schema (v9)

I checked the entities against the DDL in the migrations (3→4 rebuilt the tables, then 4→9 added things). They match. The final
schema has **no column DEFAULTs**: migration 1→2 added `DEFAULT ''` to `url` and `artist`, but the 3→4 rebuild dropped them.
No entity uses `@ColumnInfo`, so column names are the Kotlin property names exactly. Note the mixed case in `playListId`,
`song_uuid`, `playlist_uuid` and `updated_on`.

### Type converters
`UUIDConverter`, registered at DB level:
```kotlin
class UUIDConverter {
    @TypeConverter fun fromUUID(uuid: UUID): String = uuid.toString()
    @TypeConverter fun uuidFromString(string: String): UUID? = if (string.isNotEmpty()) UUID.fromString(string) else null
}
```
- Applies to `Song.uuid`, `Playlist.uuid`, `SongToPlaylist.song_uuid` and `SongToPlaylist.playlist_uuid`. They are stored as TEXT
  and are nullable; Room adds the null checks itself. An empty string reads back as null.
- `ActionType` (enum) uses Room's built-in enum converter, which stores `name` as TEXT. `Boolean` is stored as INTEGER 0/1.
- Desktop: `java.util.UUID` works on JVM as-is. You could switch to `kotlin.uuid.Uuid`, but the string format must stay the same.

### Table `Song`
| column | SQL type | null | notes |
|---|---|---|---|
| id | INTEGER PK AUTOINCREMENT | NOT NULL | autoGenerate, default 0 in Kotlin |
| name | TEXT | NOT NULL | normalized title |
| artist | TEXT | NOT NULL | |
| length | INTEGER | NOT NULL | ms |
| filePath | TEXT | NOT NULL | absolute path to `.mp3` |
| artPath | TEXT | NOT NULL | absolute path to `.webp` |
| timeHeard | INTEGER | NOT NULL | cumulative ms listened (Kotlin `Int`; the DAO adds a `Long`) |
| downloadedAt | INTEGER | NOT NULL | epoch ms |
| url | TEXT | NOT NULL | source URL; legacy rows are `'legacy_' || name` |
| uuid | TEXT | NULL | UUID |

Indices: `index_Song_url` UNIQUE(url) and `index_Song_uuid` UNIQUE(uuid).

### Table `Playlist`
| column | type | null |
|---|---|---|
| id | INTEGER PK AUTOINCREMENT | NOT NULL |
| name | TEXT | NOT NULL |
| timeHeard | INTEGER | NOT NULL (Kotlin default 0) |
| artPath | TEXT | NULL (default null) |
| createdAt | INTEGER | NOT NULL |
| uuid | TEXT | NULL |

Index: `index_Playlist_name` UNIQUE(name).

### Table `SongToPlaylist`
| column | type | null |
|---|---|---|
| id | INTEGER PK AUTOINCREMENT | NOT NULL |
| songId | INTEGER | NOT NULL, FK → Song(id) ON DELETE CASCADE ON UPDATE NO ACTION |
| playListId | INTEGER | NOT NULL, FK → Playlist(id) ON DELETE CASCADE ON UPDATE NO ACTION |
| position | INTEGER | NOT NULL (0-based) |
| addedOn | INTEGER | NOT NULL epoch ms |
| song_uuid | TEXT | NULL |
| playlist_uuid | TEXT | NULL |

There are **no indices**. Room warns about the unindexed FK columns. There is also **no unique constraint on (songId, playListId)**,
so duplicate rows are possible. To open an Android DB file, keep this shape exactly; adding indices changes the identity hash and
needs a migration.

### Table `Settings`
| column | type | null |
|---|---|---|
| id | INTEGER PK AUTOINCREMENT | NOT NULL |
| name | TEXT | NOT NULL |
| value | TEXT | NOT NULL |
| updated_on | INTEGER | NOT NULL |

Index: `index_Settings_name` UNIQUE(name). The DAO inserts with REPLACE, so this works as a key-value upsert: the old row is deleted
and a new id is assigned.

### Table `Actions` (entity `SyncAction`, also `@Serializable`)
| column | type | null | Kotlin default |
|---|---|---|---|
| localId | INTEGER PK AUTOINCREMENT | NOT NULL | 0 |
| actionUuid | TEXT | NOT NULL | `UUID.randomUUID().toString()` |
| type | TEXT (enum name) | NOT NULL | — |
| songUuid | TEXT | NULL | null |
| playlistUuid | TEXT | NULL | null |
| payload | TEXT (JSON string) | NULL | null |
| createdAt | INTEGER | NOT NULL | `System.currentTimeMillis()` |
| isSynced | INTEGER (bool) | NOT NULL | false |

Indices: `index_Actions_actionUuid` UNIQUE(actionUuid) and `index_Actions_isSynced` (isSynced).
`ActionType`: `SONG_LIKED, SONG_UNLIKED, SONG_RENAMED, PLAYLIST_CREATED, PLAYLIST_DELETED, PLAYLIST_RENAMED, SONG_ADDED_TO_PLAYLIST, SONG_REMOVED_FROM_PLAYLIST, PLAYLIST_SONG_REORDERED`.

### Table `ListeningSessions`
| column | type | null | Kotlin default |
|---|---|---|---|
| id | INTEGER PK AUTOINCREMENT | NOT NULL | 0 |
| songId | INTEGER | NULL | null (no FK, kept on purpose so stats survive deletes) |
| playlistId | INTEGER | NULL | null |
| songName | TEXT | NULL | null |
| artistName | TEXT | NULL | null |
| playlistName | TEXT | NULL | null |
| songArtPath | TEXT | NULL | null |
| songLength | INTEGER | NOT NULL | 0 |
| startTimeMs | INTEGER | NOT NULL | — |
| durationMs | INTEGER | NOT NULL | — |

Indices: `index_ListeningSessions_songId`, `index_ListeningSessions_playlistId` and `index_ListeningSessions_startTimeMs` (none unique).

### Exact final entity code (copy as-is to commonMain/jvmMain; drop the unused android imports)
```kotlin
package com.yatharth.musitify.data.entity

import androidx.room.*
import kotlinx.serialization.Serializable
import java.util.UUID

@Entity(indices = [Index(value = ["url"], unique = true), Index(value = ["uuid"], unique = true)])
data class Song(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val artist: String,
    val length: Long,
    val filePath: String,
    val artPath: String,
    val timeHeard: Int,
    val downloadedAt: Long,
    val url: String,
    var uuid: UUID? = null
)

@Entity(indices = [Index(value = ["name"], unique = true)])
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val timeHeard: Int = 0,
    val artPath: String? = null,
    val createdAt: Long,
    var uuid: UUID? = null
)

// Not an entity: result of PlaylistDao.getAllPlaylists()
data class PlaylistInfo(
    @Embedded val playlist: Playlist,
    val numOfSongs: Int,
    val playlistLength: Long      // SUM(song.length); SQL NULL for empty playlists -> Room maps to 0
)

@Entity(
    foreignKeys = [
        ForeignKey(entity = Song::class, parentColumns = ["id"], childColumns = ["songId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Playlist::class, parentColumns = ["id"], childColumns = ["playListId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class SongToPlaylist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val songId: Int,
    val playListId: Int,
    val position: Int,
    val addedOn: Long,
    var song_uuid: UUID? = null,
    var playlist_uuid: UUID? = null
)

@Entity(tableName = "Settings", indices = [Index(value = ["name"], unique = true)])
data class Settings(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val value: String,
    val updated_on: Long
)

enum class ActionType {
    SONG_LIKED, SONG_UNLIKED, SONG_RENAMED, PLAYLIST_CREATED, PLAYLIST_DELETED,
    PLAYLIST_RENAMED, SONG_ADDED_TO_PLAYLIST, SONG_REMOVED_FROM_PLAYLIST, PLAYLIST_SONG_REORDERED
}

@Entity(tableName = "Actions", indices = [Index(value = ["actionUuid"], unique = true), Index(value = ["isSynced"])])
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

@Entity(tableName = "ListeningSessions", indices = [Index("songId"), Index("playlistId"), Index("startTimeMs")])
data class ListeningSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val songId: Int? = null,
    val playlistId: Int? = null,
    val songName: String? = null,
    val artistName: String? = null,
    val playlistName: String? = null,
    val songArtPath: String? = null,
    val songLength: Long = 0,
    val startTimeMs: Long,
    val durationMs: Long
)
```

### Migration history (for reference only)
| v | change |
|---|---|
| 1→2 | drop `index_Song_name`; add Song.url and Song.artist (`TEXT NOT NULL DEFAULT ''`); `url = 'legacy_' || name`; unique `index_Song_url` |
| 2→3 | add `uuid` TEXT to Song and Playlist; add `song_uuid` and `playlist_uuid` to SongToPlaylist |
| 3→4 | rebuild Song, Playlist and SongToPlaylist (FK off): `''` uuids become NULL, defaults removed, FKs added; recreate `index_Song_url` |
| 4→5 | create Settings plus non-unique `index_Settings_name` |
| 5→6 | make `index_Settings_name` UNIQUE |
| 6→7 | UNIQUE `index_Song_uuid` and UNIQUE `index_Playlist_name` |
| 7→8 | create `Actions` plus its 2 indices |
| 8→9 | create `ListeningSessions` plus its 3 indices |

---

## 3. DAOs (every method)

### SongDao
| method | SQL / annotation | returns |
|---|---|---|
| `suspend fun addSong(song: Song): Long` | `@Insert(onConflict = IGNORE)` | rowId, or -1 on conflict (url/uuid) |
| `suspend fun addSongs(song: List<Song>)` | `@Insert(onConflict = IGNORE)` | Unit |
| `fun getAllSongs(): Flow<List<Song>>` | `SELECT * FROM Song ORDER BY downloadedAt` | Flow |
| `suspend fun getAllSongsSuspend(): List<Song>` | `SELECT * FROM Song ORDER BY downloadedAt` | suspend |
| `suspend fun getAllSongsWithoutLength(): List<Song>` | `SELECT * FROM Song WHERE length = 0 ORDER BY downloadedAt` | suspend |
| `suspend fun getSongById(id: Int): Song?` | `SELECT * FROM Song WHERE id = :id` | suspend |
| `suspend fun getSongByUuid(id: String): Song?` | `SELECT * FROM Song WHERE uuid = :id` | suspend |
| `suspend fun getSongByName(name: String): Song?` | `SELECT * FROM Song WHERE name = :name LIMIT 1` | suspend |
| `suspend fun getSongByUrl(url: String): Song?` | `SELECT * FROM Song WHERE url = :url` | suspend |
| `suspend fun updateSong(song: Song)` | `@Update` (by PK) | suspend |
| `suspend fun getSongsByIds(ids: List<Int>): List<Song>` | `SELECT * FROM Song WHERE id in (:ids) ORDER BY downloadedAt` | suspend |
| `suspend fun getSongsByUrls(urls: List<String>): List<Song>` | `SELECT * FROM Song WHERE url in (:urls) ORDER BY downloadedAt` | suspend |
| `fun searchSongs(query: String): Flow<List<Song>>` | `SELECT * FROM Song WHERE name LIKE '%' \|\| :query \|\| '%' ORDER BY name` | Flow (SQLite LIKE is case-insensitive for ASCII) |
| `suspend fun deleteSongById(id: Int)` | `DELETE FROM Song WHERE id = :id` | suspend (cascades to SongToPlaylist) |
| `suspend fun deleteSongByUuid(id: String)` | `DELETE FROM Song WHERE uuid = :id` | suspend |
| `suspend fun addListenTime(songId: Int, ms: Long)` | `UPDATE Song SET timeHeard = timeHeard + :ms WHERE id = :songId` | suspend |
| `suspend fun updateLength(songId: Int, length: Long)` | `UPDATE Song SET length = :length WHERE id = :songId` | suspend |
| `fun getTotalNumOfSongs(): Flow<Long>` | `SELECT COUNT(*) FROM Song` | Flow |

### PlaylistDao
| method | SQL | returns |
|---|---|---|
| `suspend fun addPlaylist(playlist: Playlist): Long` | `@Insert(IGNORE)` | rowId / -1 |
| `suspend fun addPlaylists(playlist: List<Playlist>)` | `@Insert(IGNORE)` | Unit |
| `fun getAllPlaylists(): Flow<List<PlaylistInfo>>` | `SELECT p.*, COUNT(sp.songId) as numOfSongs, SUM(song.length) as playlistLength FROM Playlist p LEFT JOIN SongToPlaylist sp ON sp.playListId = p.id LEFT JOIN Song song ON song.id = sp.songId GROUP BY p.id ORDER BY p.createdAt` | Flow |
| `suspend fun getAllPlaylistsFrom(names: List<String>): List<Playlist>` | `SELECT * FROM PLAYLIST where name in (:names)` | suspend |
| `suspend fun getAllPlaylistsSuspend(): List<Playlist>` | `SELECT p.* FROM Playlist p` | suspend |
| `fun getSongsForPlaylist(playlistId: Int): Flow<List<Song>>` | `SELECT s.* FROM Song s INNER JOIN SongToPlaylist sp ON sp.playListId = :playlistId WHERE s.id = sp.songId ORDER BY sp.position` | Flow |
| `suspend fun getSongsForPlaylistSuspend(playlistId: Int): List<Song>` | same SQL as above | suspend |
| `suspend fun deleteById(id: Int)` | `DELETE FROM Playlist WHERE id = :id` | suspend (cascades relations) |
| `suspend fun deleteByUuid(uuid: String)` | `DELETE FROM Playlist WHERE uuid = :uuid` | suspend |
| `suspend fun addListenTime(id: Int, ms: Long)` | `UPDATE Playlist SET timeHeard = timeHeard + :ms WHERE id = :id` | suspend |
| `suspend fun getPlaylistByName(name: String): Playlist?` | `SELECT * FROM Playlist WHERE name = :name` | suspend |
| `suspend fun getPlaylistByUuid(uuid: String): Playlist?` | `SELECT * FROM Playlist WHERE uuid = :uuid` | suspend |
| `suspend fun getPlaylistById(id: Int): Playlist?` | `SELECT * FROM Playlist WHERE id = :id` | suspend |
| `suspend fun updateArt(id: Int, artPath: String)` | `UPDATE Playlist SET artPath = :artPath where id = :id` | suspend |
| `suspend fun updatePlaylist(playlist: Playlist)` | `@Update` | suspend |

### SongToPlaylistDao
| method | SQL | returns |
|---|---|---|
| `suspend fun getAll(): List<SongToPlaylist>` | `SELECT * FROM SongToPlaylist` | suspend |
| `suspend fun addSongToPlaylist(songToPlaylist: SongToPlaylist)` | `@Insert(REPLACE)` | suspend |
| `suspend fun addSongsToPlaylist(songToPlaylist: List<SongToPlaylist>)` | `@Insert(REPLACE)` | suspend |
| `suspend fun getLastPosition(playlistId: Int): Int` | `SELECT COALESCE(MAX(position), -1) FROM SongToPlaylist WHERE playListId = :playlistId` | suspend |
| `suspend fun removeSong(songPlaylist: SongToPlaylist)` | `@Delete` (by PK id) | suspend |
| `suspend fun remove(songId: Int, playlistId: Int)` | `DELETE FROM SONGTOPLAYLIST WHERE songId = :songId AND playListId = :playlistId` | suspend |
| `suspend fun findBySongUuidAndPlaylistUuid(songUuid: String, playlistUuid: String): SongToPlaylist?` | `SELECT * FROM SONGTOPLAYLIST where song_uuid = :songUuid AND playlist_uuid = :playlistUuid` | suspend |
| `suspend fun updatePosition(songId: Int, playlistId: Int, newPosition: Int)` | `UPDATE SongToPlaylist SET position = :newPosition WHERE songId = :songId AND playListId = :playlistId` | suspend |
| `suspend fun getSongsInPlaylist(playlistId: Int): List<SongToPlaylist>` | `SELECT * FROM SongToPlaylist WHERE playListId = :playlistId ORDER BY position` | suspend |

### SettingsDao
| method | SQL | returns |
|---|---|---|
| `suspend fun updateSetting(setting: Settings): Long` | `@Insert(onConflict = REPLACE)` (upsert by unique name) | rowId |
| `suspend fun getSettingByName(name: String): Settings?` | `SELECT * FROM SETTINGS WHERE name = :name` | suspend |
| `suspend fun getSettingsById(id: Int): Settings?` | `SELECT * FROM Settings WHERE id = :id` | suspend |

### SyncActionDao
| method | SQL | returns |
|---|---|---|
| `suspend fun getUnsynced(): List<SyncAction>` | `SELECT * FROM Actions WHERE isSynced = 0 ORDER BY localId ASC` | suspend |
| `suspend fun getAll(): List<SyncAction>` | `SELECT * FROM Actions ORDER BY localId ASC` | suspend |
| `suspend fun getByActionUuid(actionUuid: String): SyncAction?` | `SELECT * FROM Actions WHERE actionUuid = :actionUuid` | suspend |
| `suspend fun getByActionUuids(actionUuids: List<String>): List<SyncAction?>` | `SELECT * FROM Actions WHERE actionUuid in (:actionUuids)` | suspend |
| `suspend fun insert(action: SyncAction): Long` | `@Insert(IGNORE)` | rowId / -1 |
| `suspend fun insertAll(actions: List<SyncAction>)` | `@Insert(IGNORE)` | Unit |
| `suspend fun markSynced(actionUuids: List<String>)` | `UPDATE Actions SET isSynced = 1 WHERE actionUuid IN (:actionUuids)` | suspend |
| `suspend fun pruneSynced()` | `DELETE FROM Actions WHERE isSynced = 1` | suspend |

### ListeningSessionDao
| method | SQL | returns |
|---|---|---|
| `suspend fun insert(session: ListeningSession): Long` | `@Insert` (ABORT) | rowId |
| `suspend fun insertAll(sessions: List<ListeningSession>)` | `@Insert` | Unit |
| `fun getAll(): Flow<List<ListeningSession>>` | `SELECT * FROM ListeningSessions ORDER BY startTimeMs ASC` | Flow |

For Room KMP, change `List<SyncAction?>` to `List<SyncAction>`. Nullable list elements are unusual, and KSP may reject them.

---

## 4. Repositories

All repositories are Hilt `@Singleton` with `@Inject constructor`.

### Helpers referenced
- `getBaseDir()` (SongRepository, private) = `context.getExternalFilesDir(null)!!.absolutePath`, or throws `IOException("External storage not available")`. All songs (`.mp3`) and art (`.webp`) live directly in this directory.
- Playlist covers live in `context.filesDir/playlist_covers/playlist_<id>.png`.
- `ConvertSong.invoke(pyResult): String` reads `result["audio"]` (the path yt-dlp downloaded), converts or waits for an `.mp3` beside it, and returns the **bare file name without extension** (`substringAfterLast("/")`). It returns `""` if there is no audio. The other agent maps it in detail; it is used here as `songPath`.
- Python (Chaquopy) module `downloader`:
  - `downloadSong(url, outputDir)` returns a dict-like with keys `title`, `duration` (seconds), `artist` and `audio` (path).
  - `get_playlist_urls(playlistUrl)` returns a dict with `name` and `urls` (a list).
- `SongNormalizer.normalize(raw).normalizedTitle` cleans the title (covered by the util map).
- `SongMedia.playCountOf(song) = round(timeHeard.toDouble() / length)`, or 0 if length == 0 (`roundToInt`).
- `SongMedia.cachePlaylistCover(context, id, artPaths)` builds a 512×512 ARGB 2×2 grid from the first 4 art paths. Each image is center-cropped to a square and drawn into quadrants TL, TR, BL, BR. Images that fail to decode are skipped, leaving that quadrant transparent. The result is written as PNG (quality 100) to `filesDir/playlist_covers/playlist_<id>.png`, which is returned.
- `SongMedia.getAudioDurationMs(path)` uses `MediaMetadataRetriever` METADATA_KEY_DURATION, or 0.

### ActionRepository (outbox; called by the repositories below)
- `record(type, songUuid?, playlistUuid?, payloadJson?)` inserts a `SyncAction` with a fresh actionUuid and createdAt = now, then logs a warning if the result is -1.
- Payload JSON per call:
  - `likedSong(song)` → SONG_LIKED, songUuid, `{url,title,artist,downloaded_at}`.
  - `likedSongs(list)` → the same payload, one row per song, via `insertAll`.
  - `unlikedSong(uuid?)` → SONG_UNLIKED, with no payload. Does nothing if uuid is null.
  - `recordPlaylistCreated(pUuid, name, createdAt)` → `{name, created_at}`.
  - `recordPlaylistDeleted(pUuid)` → no payload.
  - `recordSongAddedToPlaylist(song?, pUuid, position, added_on)` → `{url,title,artist,downloaded_at,position,added_on}`. Does nothing if song is null.
  - `recordSongRemovedFromPlaylist(sUuid, pUuid)` → no payload.
  - `recordSongRenamed(sUuid?, old, new)` → `{old_name,new_name}`. Does nothing if uuid is null.
  - `recordPlaylistRenamed(pUuid?, old, new)` → `{old_name,new_name}`.
  - `recordPlaylistSongReordered(pUuid, sUuid, from, to)` → `{from_position,to_position}`.
- Cursor: `getLastCursor()` reads setting `sync_cursor` (Long, default 0). `setCursor(c)` writes `sync_cursor`.
- `markSynced()` is **commented out** right now and does nothing ("Unmark when pushing… prod").
- `insertRemoteActions(list)` filters out actionUuids that already exist, inserts the rest and returns the new ones.

### SongRepository
Deps: `SongDao`, `ConvertSong`, `ActionRepository`, `Context`.

- `val allSongs: Flow<List<Song>> = songDao.getAllSongs()`
- `fun getFilteredSongs(query: String) = songDao.searchSongs(query)`

**`suspend fun downloadSong(url: String = "https://www.youtube.com/watch?v=_bSXY75R3Wc", downloaded_at: Long = now, songUUID: UUID = UUID.randomUUID(), record: Boolean = true): Song?`** runs on Dispatchers.IO:
1. `songDao.getSongByUrl(url)`. If a song exists, return it and do nothing else.
2. Start Python if needed, get module `downloader`, and call `downloadSong(url, getBaseDir())`.
3. Set `songPath = convertSong(result)`. Read `title` (default "unknown"), `duration` (seconds → ms) and `artist` (default "unknown"). Set `name = SongNormalizer.normalize(title).normalizedTitle`.
4. Legacy path: if `getSongByName(name)` exists **and its url contains "legacy"**, call `updateSong(existing.copy(url=url))`, **delete the just-downloaded** `<base>/<songPath>.mp3` and `.webp` files, and return `existing.copy(url=url)`. No action is recorded on this path.
5. Otherwise build `Song(name, artist, length = durationMs ?: 0, timeHeard = 0, filePath = "<base>/<songPath>.mp3", artPath = "<base>/<songPath>.webp", downloadedAt = downloaded_at, url, uuid = songUUID)` and call `addSong`. If the id is -1, re-fetch by url.
6. If `record` and the song is not null, call `actionRepository.likedSong(song)`.
7. Any exception is logged and the method returns `null`.

**`suspend fun downloadSongsFromSync(nSongs: List<NSong>, outputDir: String = getBaseDir(), maxConcurrent: Int = 5, onProgress: (completed, total) -> Unit = {_,_->}): Map<String, Song>`** runs on IO:
1. Start Python. Set `total = nSongs.size`.
2. Set `existingSongs = getSongsByUrls(nSongs.urls)`. `newSongs` is the nSongs whose url is not already there.
3. Run every `newSongs` entry in parallel with `async(IO)` under `Semaphore(maxConcurrent)`. Each one does the same steps as downloadSong (Python download, convert, normalize, legacy-name check that updates the url and deletes the new files). For a new song it uses `downloadedAt = nSong.downloaded_at` and `uuid = UUID.fromString(nSong.id)`, with files written to `outputDir`. On the legacy path it deletes files under getBaseDir(), not outputDir. The result is `Pair(nSong.id, song)`, or null on error.
4. In `finally`, the `AtomicInteger` counter is incremented and `onProgress(done, total)` is called. Because `total` counts all nSongs including existing ones, the progress count may never reach total.
5. Call `actionRepository.likedSongs(newly downloaded songs)`. This **always records**; there is no `record` flag.
6. Return `songMap + existingSongs.associateBy { it.uuid.toString() }`, keyed by uuid string.

**`suspend fun deleteSongById(id: Int, record: Boolean = true)`**
1. Get the song, or throw `IOException("Invalid song uuid")`.
2. `deleteSongById` (FK cascade removes the playlist relations).
3. If `record`, call `unlikedSong(uuid)`.
4. Delete `filePath` and `artPath` from disk if they exist.

**`suspend fun deleteSongByUUID(id: String, record: Boolean = true)`** does the same, but by uuid.

**`suspend fun getSongsByUrl(urls)`** = `songDao.getSongsByUrls`.

**`suspend fun updateSong(song: Song, record: Boolean = true)`**
1. If `record`, load the old row by id. If the name changed, call `recordSongRenamed(uuid, old.name, new.name)`.
2. Call `songDao.updateSong(song)`.

**`suspend fun fetchAllAndUpdate()`**: for every song with `length = 0`, call `updateLength(id, SongMedia.getAudioDurationMs(filePath))`. This is not called anywhere in the current tree.

**`suspend fun addSongFromSync(songUuid: String?, payload: String?)`**
1. Throw if either argument is null.
2. Parse `url` (required) and `downloaded_at` (Long, default now) from the payload.
3. Call `downloadSong(url, songUUID = fromString(songUuid), downloaded_at, record = false)`.

### PlaylistRepository
Deps: `SongDao`, `PlaylistDao`, `SongToPlaylistDao`, `SongRepository`, `ActionRepository`, `Context`.

- `val allPlaylists: Flow<List<PlaylistInfo>> = playlistDao.getAllPlaylists()`
- `fun songsInPlaylist(playlistId): Flow<List<Song>> = playlistDao.getSongsForPlaylist(id)`

**`suspend fun downloadPlaylist(playlistUrl: String, onProgress: (Int, Int) -> Unit = {_,_->})`** runs on IO:
1. Call Python `get_playlist_urls(url)`. Use `name` (trimmed, default "Unknown Playlist") and `urls`.
2. Call `songRepository.downloadSongsFromSync(urls.map { NSong(id = randomUUID, url = it, title = "", artist = "", downloaded_at = now) }, onProgress = onProgress)`. Re-key the result to `Map<Int(song.id), Song>`.
3. If the map is not empty, call `createPlaylistAndAddSongs(name, songs)`. Otherwise log an error and do not create a playlist.

**`suspend fun createPlaylistFromSync(playlistUuid: String?, payload: String?)`**
1. Throw on null.
2. Parse `name` (required) and `created_at` (default now).
3. Call `createPlaylist(name, uuid = fromString(playlistUuid), record = false, createdAt)`.

**`suspend fun createPlaylist(name: String, uuid: UUID? = null, record: Boolean = true, createdAt: Long = now): Playlist`** runs on IO:
- If `getPlaylistByName(name)` exists, return it. Nothing is recorded and the uuid is not changed.
- Otherwise insert `Playlist(name, timeHeard = 0, createdAt, uuid = uuid ?: random)`. If `record`, call `recordPlaylistCreated(uuid, name, createdAt)`. Return `copy(id = insertedId)`. The id is not checked for -1.

**`suspend fun getAllPlaylistsFrom(names)`** = DAO passthrough.

**`suspend fun generatePlaylistArt(playlistId: Int)`** runs on IO:
1. `getSongsForPlaylistSuspend(id)` returns Songs ordered by position.
2. `songDao.getSongsByIds(those.map { it.id })` re-queries them, which **re-sorts by downloadedAt**.
3. Call `getOrCreatePlaylistCover(id, songs)`, then `updateArt(id, file.path)`.

**`fun getOrCreatePlaylistCover(playlistId: Int, songs: List<Song>): File`**
- If `filesDir/playlist_covers/playlist_<id>.png` exists, return it. Because of this cache, the cover is never regenerated.
- Otherwise call `cachePlaylistCover(context, id, first-4 artPaths)`.

**`suspend fun createPlaylistAndAddSongs(name: String, songs: Map<Int, Song>)`** runs on IO: `createPlaylist(name)` (record = true), then `addSongsToPlaylist(playlist, songs)`, then `generatePlaylistArt(playlist.id)`.

**`suspend fun addSongPlaylistRelations(relations)`** = `songToPlaylistDao.addSongsToPlaylist` (REPLACE).

**`suspend fun addSongsToPlaylist(playlist: Playlist, songs: Map<Int, Song>)`**
1. Look up the playlist by **name**. If it is missing, throw `IllegalStateException("Playlist with name '$playlist.name' not found in database")`. The message is a literal template bug.
2. `validSongs` = entries where `getSongById(key) != null`. If that is empty, throw `IllegalStateException("No valid songs to add. All referenced songs may have been deleted.")`.
3. Set `target` = the existing DB playlist if its id differs from the argument, otherwise the argument. Set `position = getLastPosition(target.id) + 1`.
4. For each valid songId at index i:
   - call `recordSongAddedToPlaylist(song, target.uuid, position + i, now)`
   - build `SongToPlaylist(songId, target.id, position + i, addedOn = now, song_uuid = song.uuid, playlist_uuid = target.uuid)`.
5. Batch-insert the rows with REPLACE. Nothing checks for duplicates, so adding a song that is already in the playlist creates a second row.

**`suspend fun getAllSongRelations()`** = `songToPlaylistDao.getAll()`.

**`suspend fun updatePlaylist(playlist, record = true)`**: if `record` and the name changed, call `recordPlaylistRenamed(uuid, old, new)`. Then `updatePlaylist`.

**`suspend fun deletePlaylistById(id, record = true)`**
1. Get the playlist, or throw `IOException("Invalid playlist id")`.
2. If `record`, call `recordPlaylistDeleted(uuid)`.
3. `deleteById` (cascade). The cover PNG file is **not** deleted.

**`suspend fun deletePlaylistByUuid(uuid: String?, record = true)`**
1. Throw if null.
2. Get the playlist, or throw.
3. If `record`, call `recordPlaylistDeleted`.
4. `deleteByUuid`.

**`suspend fun addSongToPlaylistSync(songUuid?, playlistUuid?, payload?)`** runs on IO:
1. Resolve the song and the playlist by uuid, or throw `IOException`.
2. Parse `position` from the payload. The value is **unused**.
3. Call `addSongsToPlaylist(playlist, mapOf(song.id to song))`. This **records a SONG_ADDED_TO_PLAYLIST action again**; there is no record flag.

**`suspend fun removeSong(songId: Int, playlistId: Int)`**
1. Get the song and the playlist, or throw `IOException`.
2. `findBySongUuidAndPlaylistUuid(song.uuid, playlist.uuid)`, or throw `"Song not in playlist"`.
3. Call `recordSongRemovedFromPlaylist`.
4. `removeSong(row)`. Positions are **not** compacted afterwards.

**`suspend fun removeSongFromPlaylistSync(songUuid?, playlistUuid?)`** does the same lookup, then `removeSong`. No action is recorded.

**`suspend fun reorderSongsInPlaylist(playlistId: Int, fromIndex: Int, toIndex: Int, record: Boolean = true)`** runs on IO:
1. Get the rows with `getSongsInPlaylist(playlistId)` (ordered by position).
2. If either index is out of bounds, return silently.
3. Load the playlist and the moved song.
4. On a mutable copy, call `removeAt(from)` then `add(to, item)`.
5. For each index, call `updatePosition(songId, playlistId, index)`. Positions are rewritten as 0..n-1. This runs one UPDATE per row and is not wrapped in a transaction.
6. If `record` and both are non-null, call `recordPlaylistSongReordered(pUuid, sUuid, from, to)`.

**`suspend fun reorderSongInPlaylistSync(songUuid?, playlistUuid?, payload?)`** runs on IO:
1. Throw if any argument is null.
2. Resolve the song and the playlist.
3. Parse `from_position` and `to_position` (default 0).
4. Call `reorderSongsInPlaylist(playlist.id, from, to, record = false)`.

### SettingsRepository
Deps: `Context` (unused), `SettingsDao`.

- `suspend fun insertSetting(name, value): Long` → `updateSetting(Settings(name, value, updated_on = now))` (REPLACE upsert).
- `suspend fun getSettingByName(name): Settings?`
- `suspend fun getSettingById(id: Int): Settings?`
- `suspend fun hasUserOpenedApp(): Boolean` → setting `"hasOpened"`, with `.value.toBoolean()`. Null gives false.
- `suspend fun openedApp()` writes `hasOpened = "true"`.

### StatsRepository
Deps: `ListeningSessionDao`, `SongDao`, `PlaylistDao`, `SettingsRepository`.

- `val sessions: Flow<List<ListeningSession>> = listeningSessionDao.getAll()` (ordered by startTimeMs ASC)
- `val totalCount: Flow<Long> = songDao.getTotalNumOfSongs()`

**`suspend fun backfillLegacyListenTime()`**
1. If setting `legacy_listen_backfill` == `"done"`, return.
2. For every song with timeHeard > 0, build `ListeningSession(songId, songName = name, artistName = artist, songArtPath = artPath, songLength = length, startTimeMs = 0L, durationMs = timeHeard)`.
3. For every playlist with timeHeard > 0, build `ListeningSession(playlistId, playlistName = name, startTimeMs = 0L, durationMs = timeHeard)`.
4. If either list is non-empty, `insertAll`.
5. Set `legacy_listen_backfill = "done"`.

Constants: `LEGACY_BACKFILL_KEY = "legacy_listen_backfill"` and `LEGACY_TIMESTAMP = 0L`.

**How sessions are written** (MediaControllerRepository, documented elsewhere): every **7 s** while playing, `delta = (currentPos − lastPos).coerceIn(0, 15_000)` is accumulated. A flush then does:
- `songDao.addListenTime`
- `playlistDao.addListenTime` (if there is an active playlist)
- an insert of `ListeningSession(songId, playlistId, songName, artist, playlistName, artPath, songLength, startTimeMs = now − ms, durationMs = ms)`.

So **one session row is a ~7 s chunk, not a full play**.

### Stats (`StatsComputer.compute(sessions, now)`), the exact logic
All the stats are computed in Kotlin from the full session list. No SQL aggregation is involved.

Data classes:
```kotlin
data class DailyStat(val dayStartMs: Long, val totalMs: Long)
data class SongStat(val songId: Int?, val name: String, val artistName: String, val artPath: String?, val totalMs: Long, val fullPlays: Int, val sessionCount: Int)
data class ArtistStat(val name: String, val totalMs: Long, val songCount: Int)
data class PlaylistStat(val playlistId: Int?, val name: String, val totalMs: Long, val sessionCount: Int)
data class StatsSnapshot(
  totalListenMs=0, todayListenMs=0, thisWeekListenMs=0, thisMonthListenMs=0, listensToday=0, listensThisMonth=0,
  totalSessions=0, averageSessionMs=0, mostActiveDay: DailyStat?=null, currentStreak=0, longestStreak=0,
  averageDailyListenMs=0, completionRate=0.0, dailySeries=emptyList(), topSongsAllTime=emptyList(),
  topSongsThisMonth=emptyList(), topArtists=emptyList(), topPlaylists=emptyList()
) { val hasAnyListenTime get() = totalListenMs > 0 }
```
- If there are no sessions, return `StatsSnapshot()`.
- Constants: `DAILY_CHART_DAYS = 14` and `DAY_MS = 86_400_000`.
- Time windows use the default timezone and locale of `java.util.Calendar.getInstance()`:
  - `todayStart` = today at 00:00:00.000.
  - `weekStart` = today at 00:00 with `DAY_OF_WEEK = firstDayOfWeek` (locale dependent: Sunday in the US, Monday in most other locales).
  - `monthStart` = day 1 of the month at 00:00.

The loop over each session `s`:
- **Only when `s.songId != null`:**
  - `total += dur`
  - if `start >= todayStart`: `today += dur`, `todayCount++`
  - if `start >= weekStart`: `week += dur`
  - if `start >= monthStart`: `month += dur`, `monthCount++`
  - `dailyMap[startOfDay(start)] += dur`
  - `songAgg[songId] += s`
- **Artist key:** if `songName == null`, `continue`. This **also skips the playlist aggregation** below. Otherwise the key is `artistName` if it is not blank, else `"Unknown Artist"`, and `artistAgg[key] += s`.
- **Playlist key:** `playlistName ?: playlistId?.let { "Playlist #$it" }`. If both are null, `continue`. Otherwise `playlistAgg[(playlistId, name)] += s`.

  As a result, legacy playlist-only rows (songName is null) are never counted in topPlaylists. Only song sessions played inside a playlist are counted.

Derived values:
| stat | formula |
|---|---|
| totalListenMs | Σ dur of song sessions (legacy song rows at t = 0 included) |
| todayListenMs / thisWeekListenMs / thisMonthListenMs | Σ dur of song sessions with start ≥ the window start |
| listensToday / listensThisMonth | **count of song-session rows** (7 s chunks) in the window |
| dailySeries | 14 entries, i = 0..13: `dayStart = todayStart − (13 − i)·DAY_MS`, `totalMs = dailyMap[dayStart] ?: 0`. Fixed 24 h steps, so keys can be missed around DST changes |
| totalSessions | number of sessions with songId ≠ null |
| averageSessionMs | `total / totalSessions` (Long division), or 0 |
| mostActiveDay | the dailyMap entry with the max value (can be the 1970 legacy day) |
| currentStreak | count back from `todayStart` in `DAY_MS` steps while `dailyMap[day] > 0`. If today has no listening, the streak is 0 |
| longestStreak | **bug, kept as-is:** iterates the sorted dailyMap keys and does `temp++` for every key with value > 0, never checking that days are consecutive. Every value is > 0, so the result equals **the number of distinct days that have listening** (legacy day included) |
| averageDailyListenMs | `total / (number of dailyMap entries with value > 0)` |
| completionRate | over song sessions with `songLength > 0`: `count(dur >= songLength·0.8) / count · 100` (Double, percent). Live sessions are 7 s chunks, so in practice only legacy rows can count as completed |
| topSongsAllTime | for each `songAgg` group, `toSongStat`; sort desc by `fullPlays`; take 10 |
| topSongsThisMonth | the same, using only sessions with start ≥ monthStart (empty groups dropped); sort desc by fullPlays; take 10 |
| topArtists | `ArtistStat(name, Σdur, songCount = distinct non-null songName count)`; sort desc by totalMs; take 10 |
| topPlaylists | `PlaylistStat(id, name, Σdur, sessionCount = rows)`; sort desc by totalMs; take 10 |

`toSongStat(songId, list)`:
- `latest` = the session with the max startTimeMs (return null if the list is empty).
- `totalMs` = Σ dur.
- `fullPlays` = `(totalMs / latest.songLength).toInt()` if songLength > 0, else `list.size`.
- `name` = `latest.songName ?: "Song #$id" ?: "Unknown Song"`.
- `artistName` = `latest.artistName ?: "Unknown Artist"`.
- `artPath` = `latest.songArtPath`.
- `sessionCount` = `list.size`.

---

## 5. ViewModels

All are `@HiltViewModel` using `viewModelScope`. Every `stateIn` uses `SharingStarted.WhileSubscribed(5000)`.

### AddSongsViewModel(downloadsManager: DownloadsManager)
```kotlin
sealed interface AddSongsUiState { object Idle; data class Queued(val isPlaylist: Boolean); data class Error(val message: String) }
```
- `state: StateFlow<AddSongsUiState>`, initially `Idle`.
- `jobs: StateFlow<List<DownloadJob>> = downloadsManager.jobs`.
- `addSingleSong(url)`: if the url is blank, set `Error("URL is empty")`. Otherwise `Queued(false)` if `downloadsManager.enqueueSong(url)` succeeds, else `Error("Already in the queue")`.
- `addPlaylist(url)`: same as above with `enqueuePlaylist`, giving `Queued(true)`.
- `cancel(id: Long)`, `retry(id: Long)` and `clearFinished()` delegate to DownloadsManager.
- `resetState()` sets `Idle`.

### HomeScreenViewModel(songRepository, playlistRepository, syncController, actionController)
State:
| name | type | initial | source |
|---|---|---|---|
| allSongs | StateFlow<List<Song>> | emptyList | `songRepository.allSongs` |
| playlists | StateFlow<List<PlaylistInfo>> | emptyList | `playlistRepository.allPlaylists` |
| playlistId | StateFlow<Int?> | null | `_playlistId` |
| songsInPlaylist | StateFlow<List<Song>> | emptyList | `_playlistId.flatMapLatest { null → flowOf(empty) else songsInPlaylist(id) }` |
| state | StateFlow<PlaylistDialogUiState> | Idle | |
| songSortMode | StateFlow<SortMode> | **PLAYS_DESC** | |
| playlistSortMode | StateFlow<SortMode> | **PLAYS_DESC** | |
| sortedSongs | StateFlow<List<Song>> | emptyList | `combine(allSongs, songSortMode)` |
| sortedPlaylists | StateFlow<List<PlaylistInfo>> | emptyList | `combine(playlists, playlistSortMode)` |
| searchQuery | StateFlow<String> | "" | |
| searchResults | StateFlow<List<Song>> | emptyList | `_searchQuery.debounce(300.ms).flatMapLatest { blank → allSongs (by downloadedAt) else searchSongs(q) (by name) }` |

```kotlin
sealed interface PlaylistDialogUiState { object Idle; object Loading; object Success; data class Error(val message: String) }
enum class SortMode { DOWNLOADED_DESC, DOWNLOADED_ASC, PLAYS_DESC, PLAYS_ASC; fun next() = entries[(ordinal + 1) % entries.size] }
```
- Toggle cycle: DOWNLOADED_DESC → DOWNLOADED_ASC → PLAYS_DESC → PLAYS_ASC → back to DOWNLOADED_DESC.
- `SortMode` is defined in `ui/elements/SortIcon.kt`. Its icons are Schedule or Headphones for the field, plus ArrowDownward or ArrowUpward.

Sorting:
- Songs: DOWNLOADED_* sorts by `downloadedAt`. PLAYS_* sorts by `playCountOf(song)` = `round(timeHeard/length)`.
- Playlists: DOWNLOADED_* sorts by `playlist.createdAt`. PLAYS_* sorts by raw `playlist.timeHeard`.
- The sorts are stable, applied to the DAO order (downloadedAt / createdAt).

Functions:
- `loadPlaylist(id)` sets `_playlistId = id`. `clearSelectedPlaylist()` sets it to null.
- `createPlaylistWithSongs(name: String, songs: Set<Song>)`:
  1. If the name is blank or songs is empty, set `Error("Name and at least one song required")` and stop.
  2. Otherwise launch: set `Loading`, call `playlistRepository.createPlaylistAndAddSongs(name, songs.associateBy { it.id })`, then set `Success`. On an exception, set `Error(e.message ?: "Unknown error")`.
- `addSongsToPlaylist(playlist, songs: Set<Song>)`:
  1. If songs is empty, set `Error("Select at least one song")`.
  2. Otherwise `Loading`, then `playlistRepository.addSongsToPlaylist(...)`, then `Success`, or `Error`.
  3. The cover is **not** regenerated.
- `onSongSortModeToggle()` / `onPlaylistSortModeToggle()` call `.next()`.
- `onSearchQueryChange(q)` sets the query. `resetState()` sets `Idle`.
- `deleteSong(index: Int)` calls `songRepository.deleteSongById(id = index)`. The parameter is a song **id**, despite its name.
- `deleteSongFromPlaylist(index: Int, playlistId: Int)` calls `playlistRepository.removeSong(songId = index, playlistId)`.
- `renameSong(song, newName)` calls `songRepository.updateSong(song.copy(name = newName))`.
- `renamePlaylist(playlist, newName)` calls `playlistRepository.updatePlaylist(playlist.copy(name = newName))`.
- `deletePlaylist(index: Int)` calls `playlistRepository.deletePlaylistById(id = index)`.
- `syncLibrary()` calls `actionController.pushActions()` then `actionController.pullActions()`. The `syncController.sendData()` call is commented out.
- `fetchLibrary()` calls `actionController.pullActions()`. The `syncController.receiveData()` call is commented out.
- `reorderSongsInPlaylist(fromIndex, toIndex)` uses the current `_playlistId` (returns if it is null) and calls `playlistRepository.reorderSongsInPlaylist(id, from, to)`.
- Exceptions thrown inside launched coroutines (delete, rename, remove) are **not caught** and will crash the app.

`flatMapLatest` and `debounce` need `@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)` on desktop, unless the opt-in is set at module level.

### PlaybackControllerViewModel(mediaControllerRepository)
- `controller` = `mediaControllerRepository.controller`, a `StateFlow<MediaController?>`.
- `uiState` = `mediaControllerRepository.uiState`, a `StateFlow<PlayerUiState>`:
  - `isPlaying = false`
  - `currentSongId: Int? = null`
  - `currentTitle: String? = null`
  - `repeatMode: Int = 0`
  - `shuffleMode = false`
  - `positionMs = 0`
  - `durationMs = 0`
  - It is polled every 500 ms.
- `sleepTimerState` = `mediaControllerRepository.sleepTimerState`.
- `playSongs(songs: List<Song>, startIndex: Int, playlistId: Int?)` calls `startPlayback(...)`.
- `toggleRepeatMode()` cycles OFF(0) → ALL(2) → ONE(1) → OFF. These are Media3 constants: OFF = 0, ONE = 1, ALL = 2.
- `toggleShuffleMode()` flips `shuffleModeEnabled`.
- `pause()` toggles play/pause: it pauses if playing, otherwise plays.
- `next()` calls `seekToNext()`. `previous()` calls `seekToPrevious()`. Media3 `seekToPrevious` restarts the current track if position > 3 s, otherwise it moves to the previous item.
- `startSleepTimer(duration: SleepTimerDuration)` and `cancelSleepTimer()` delegate to the repository.

### StatsViewModel(statsRepository)
- `songCount: StateFlow<Long>`, initially `0L` (from `totalCount`).
- `stats: StateFlow<StatsSnapshot>`, initially `StatsSnapshot()`, built as `combine(sessions, refreshTrigger) { s, _ -> StatsComputer.compute(s, System.currentTimeMillis()) }`. The work runs on the collector's dispatcher, which is Main.
- `init`: launch `backfillLegacyListenTime()`, then `refresh()`.
- `refresh()` does `refreshTrigger.value++` (a `MutableStateFlow<Int>` starting at 0). The screen calls it when it reopens so the time windows recompute.

---

## 6. Utilities in scope

### Formatter (`object`)
- `formatTime(ms: Long): String`: `s = (ms/1000).toInt()`, then `String.format("%d:%02d", s/60, s%60)`. Examples: `"3:07"`, `"65:00"`. Minutes are not capped at hours. The format uses the default locale.
- `formatTimeToMinutes(ms)`: `min = ms/60000` and `h = min/60`.
  - h == 0 → `"$min mins"`
  - h == 1 → `"1 hr ${min-60} mins"`
  - otherwise → `"$h hrs ${min-h*60} mins"`
- `formatListenTime(ms)`:
  - If ms ≤ 0, return `"0 mins"`.
  - Otherwise `min = ms/60000`, `h = min/60`, `rem = min%60`.
  - `hourPart` is `""`, `"1 hr"` or `"$h hrs"`.
  - `minutePart = "$rem mins"` if `rem != 0 || h == 0`, else `""`.
  - Non-blank parts are joined with a space. Examples: `"45 mins"`, `"1 hr 12 mins"`, `"5 hrs"`.
- `formatDayNumber(v: Long) = v.toString()`.

### LegacyFixer (`@Singleton`; deps: SongDao, PlaylistDao, SongToPlaylistDao, SettingsDao)
It is injected in MainActivity, but **neither function is called right now**.
- `generateUUIDForLibrary()` (IO):
  1. Load all songs, playlists and relations. Assign a new random UUID to every song and every playlist (mutating `var uuid`).
  2. For each relation: if its playlist or song is missing, call `songToPlaylistDao.remove(songId, playListId)`. Otherwise set `song_uuid` and `playlist_uuid`.
  3. Call `songDao.addSongs(songs)` and `playlistDao.addPlaylists(playlists)`. **Both use IGNORE**, so existing rows are NOT updated and this is effectively a no-op for them.
  4. Insert the valid relations with `addSongsToPlaylist` (REPLACE). This works.
- `updateAllMusicNames()` (IO):
  1. Normalize every song name with `SongNormalizer.normalize(name).normalizedTitle`, then group by the new name.
  2. Unique names are updated as they are.
  3. For a duplicate group, take the row with the min id. If another row in the group has a url without "legacy", copy that url onto the min-id row and update it. The other duplicates are left untouched.
  4. Updates use `updateSong` (UPDATE, not REPLACE).
  5. It logs with tag `"Legacy"`.

### IdManager (`@Singleton`; deps: Song/Playlist/SongToPlaylist/Settings DAOs, of which only SettingsDao is used)
- `suspend fun generateDeviceId(): String` creates a random UUID string, upserts setting `device_id`, and returns it.

### MusitifyApp
- `@HiltAndroidApp class MusitifyApp : Application()`. It injects `MediaControllerRepository` and calls `mediaControllerRepository.connect()` in `onCreate`.
- Desktop: do this in `main()` before `application { }` (build the graph, connect the player).

### Startup flow (MainActivity, for context)
In `lifecycleScope`, if `!settingsRepository.hasUserOpenedApp()`, call `idGenerator.generateDeviceId()` and then `settingsRepository.openedApp()`. The UI shows `StatsScreen` when `showStats` is true (rememberSaveable), otherwise `HomeScreen(onOpenStats)`.

---

## 7. Settings keys (whole tree grepped)
| key | value | written by | read by |
|---|---|---|---|
| `hasOpened` | `"true"` | `SettingsRepository.openedApp()` (MainActivity first launch) | `SettingsRepository.hasUserOpenedApp()` (`toBoolean()`) |
| `device_id` | UUID string | `IdManager.generateDeviceId()`; also lazily by `ActionController` and `SyncController` via `insertSetting("device_id", id)` when missing | `ActionController`, `SyncController` |
| `sync_cursor` | Long as string (server sequence) | `ActionRepository.setCursor()` | `ActionRepository.getLastCursor()` (default 0) |
| `legacy_listen_backfill` | `"done"` | `StatsRepository.backfillLegacyListenTime()` | same |

No other setting names exist. Values are always strings, and `updated_on` is the write time in epoch ms.

---

## 8. Android-specific APIs → desktop equivalents
| Android API (where) | desktop / JVM replacement |
|---|---|
| Hilt: `@HiltAndroidApp`, `@HiltViewModel`, `@Module/@InstallIn/@Provides`, `@ApplicationContext`, `javax/jakarta.inject.@Singleton/@Inject` (everywhere) | Manual DI (an `AppContainer` object with lazy singletons), or Koin (`koin-core` + `koin-compose`). The `javax.inject` annotations can stay harmlessly if `javax.inject` is a dependency |
| `android.content.Context` (repos, DatabaseModule) | Remove it. Pass an `AppPaths` object instead |
| `context.getExternalFilesDir(null)` (SongRepository base dir for mp3/webp) | e.g. `File(System.getenv("APPDATA"), "Musitify/music")`, or `~/.musitify/music`. Create it with `mkdirs()` |
| `context.filesDir/playlist_covers` (PlaylistRepository, SongMedia) | `AppPaths.coversDir` = `.../Musitify/playlist_covers` |
| `Room.databaseBuilder(context, Class, name)`, `SupportSQLiteDatabase` migrations | Room KMP `Room.databaseBuilder<MusitifyDatabase>(name = absPath)` + `BundledSQLiteDriver` + `setQueryCoroutineContext(Dispatchers.IO)` + `@ConstructedBy`. Migrations use `SQLiteConnection.execSQL` |
| `android.util.Log.d/e/w` (Song/Playlist/Action repos, LegacyFixer) | `println` / `System.err`, or SLF4J/Logback, or Kermit |
| Chaquopy `Python.start(AndroidPlatform(context))`, `PyObject.callAttr` (Song/Playlist repos, ConvertSong) | Run `yt-dlp.exe` (plus ffmpeg) through `ProcessBuilder` with `--dump-json`/`-J` for title, duration, artist and playlist entries (`--flat-playlist -J` for `get_playlist_urls`). Convert with ffmpeg to mp3 and write the thumbnail as webp. The file layout must match: `<base>/<name>.mp3` + `<base>/<name>.webp` |
| `android.graphics.Bitmap/Canvas/Rect/BitmapFactory`, `Bitmap.compress(PNG)` (SongMedia.cachePlaylistCover) | `java.awt.image.BufferedImage(512,512,TYPE_INT_ARGB)` + `Graphics2D.drawImage(src, dx1,dy1,dx2,dy2, sx1,sy1,sx2,sy2, null)` + `ImageIO.write(img,"png",file)`. Reading webp needs TwelveMonkeys `imageio-webp`; another option is Skia (`org.jetbrains.skia.Image.makeFromEncoded` + `Surface`/`Canvas`), which ships with Compose Desktop |
| `android.media.MediaMetadataRetriever` (SongMedia.getAudioDurationMs) | `ffprobe -show_entries format=duration`, JAudioTagger (`AudioFileIO.read(f).audioHeader.trackLength`), or the player's own length (vlcj) |
| `androidx.lifecycle.ViewModel` / `viewModelScope` | `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose` (KMP; `viewModel { }` in Compose Desktop), or plain classes with `CoroutineScope(SupervisorJob() + Dispatchers.Main)` (needs `kotlinx-coroutines-swing` for Main) |
| `collectAsStateWithLifecycle` (imported in StatsViewModel, unused) | `collectAsState()`, or lifecycle-runtime-compose KMP |
| Media3 `MediaController`, `Player.REPEAT_MODE_*`, `seekToNext/Previous` (PlaybackControllerViewModel) | vlcj (`MediaListPlayer`) or JavaFX MediaPlayer behind your own `PlayerController` interface. Re-create the repeat constants 0/1/2 and the >3 s restart rule for previous |
| `android.app.Application.onCreate` (MusitifyApp) | `fun main()` startup |
| Unused android imports: `android.R.attr.data`, `androidx.room.util.TableInfo` (Settings.kt), `android.R.attr.name` (SettingsRepository), `android.graphics.PorterDuff` (1_2.kt) | Delete them |
| `java.util.UUID`, `java.util.Calendar`, `java.io.File`, `String.format`, kotlinx.serialization, coroutines | Work unchanged on JVM desktop |

---

## 9. Quirks to keep (or fix on purpose) for 1:1 behaviour
1. `downloadSongsFromSync` always records SONG_LIKED, so a remote-sync download records actions as well (only `addSongFromSync` → `downloadSong(record=false)` avoids it).
2. `addSongToPlaylistSync` records SONG_ADDED_TO_PLAYLIST again (an echo loop) and ignores the payload `position`.
3. `addSongsToPlaylist` can create duplicate relations, since the table has no unique (songId, playListId).
4. `removeSong` leaves gaps in `position`. A reorder renumbers 0..n-1.
5. A playlist cover is generated once, at create time, from the first 4 songs ordered by **downloadedAt**. The cache file is never refreshed, and deleting a playlist leaves its PNG behind.
6. `createPlaylist` with an existing name returns the existing playlist, so duplicate names merge.
7. Stats: `longestStreak` equals the number of distinct listening days, `listensToday`/`listensThisMonth` count 7 s chunks, completionRate is per chunk, and the legacy backfill sits at epoch 0 (the 1970 local day).
8. `ActionRepository.markSynced` is disabled, so the outbox never gets marked as synced.
9. `LegacyFixer.generateUUIDForLibrary` uses IGNORE inserts, which do not update songs or playlists.
10. The Settings REPLACE upsert changes the row `id` on every write.
