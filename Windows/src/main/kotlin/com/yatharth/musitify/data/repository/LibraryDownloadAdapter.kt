package com.yatharth.musitify.data.repository

import com.yatharth.musitify.engine.DownloadLibrary
import com.yatharth.musitify.engine.DownloadedSong

/**
 * Engine [DownloadLibrary] backed by the repositories (replaces the persistence half of Android's
 * `SongRepository.downloadSong` / `PlaylistRepository.downloadPlaylist`).
 *
 * - [persistSong]: fresh UUID, downloadedAt = now, legacy-name merge, SONG_LIKED action.
 * - [persistPlaylist]: create-or-reuse the playlist by name, append the songs in playlist order
 *   (SONG_ADDED_TO_PLAYLIST per song, PLAYLIST_CREATED if new) and generate the cover.
 */
class LibraryDownloadAdapter(
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository,
) : DownloadLibrary {

    override suspend fun isInLibrary(url: String): Boolean =
        songRepository.getSongsByUrl(listOf(url)).isNotEmpty()

    override suspend fun persistSong(song: DownloadedSong) {
        songRepository.persistDownloadedSong(song)
    }

    override suspend fun persistPlaylist(name: String, songUrls: List<String>) {
        val byUrl = songRepository.getSongsByUrl(songUrls).associateBy { it.url }
        val ordered = LinkedHashMap<Int, com.yatharth.musitify.data.entity.Song>()
        songUrls.forEach { url -> byUrl[url]?.let { ordered[it.id] = it } }
        if (ordered.isEmpty()) return
        val playlistName = name.trim().ifEmpty { "Unknown Playlist" }
        playlistRepository.createPlaylistAndAddSongs(playlistName, ordered)
    }
}
