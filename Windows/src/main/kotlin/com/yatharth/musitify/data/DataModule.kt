package com.yatharth.musitify.data

import org.koin.dsl.module

/** Koin replacement for Android's Hilt DatabaseModule. */
val dataModule = module {
    single { MusitifyDatabase.build() }
    single { get<MusitifyDatabase>().songDao() }
    single { get<MusitifyDatabase>().playlistDao() }
    single { get<MusitifyDatabase>().songToPlaylistDao() }
    single { get<MusitifyDatabase>().settingsDao() }
    single { get<MusitifyDatabase>().syncActionDao() }
    single { get<MusitifyDatabase>().listeningSessionDao() }
}
