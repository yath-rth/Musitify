package com.yatharth.musitify.network

import com.yatharth.musitify.data.repository.SyncGateway
import com.yatharth.musitify.service.RemoteChangesManager
import okhttp3.OkHttpClient
import org.koin.dsl.module

/**
 * Sync bindings. Must be loaded **after** `repositoryModule` so this [SyncGateway] overrides its
 * no-op default (Koin's `allowOverride` is true by default; the later definition wins).
 * The legacy [SyncController] is intentionally not bound (unused, as on Android).
 */
val syncModule = module {
    single { OkHttpClient() }
    single { RemoteChangesManager(get(), get(), get(), get(), get()) }
    single { ActionController(get(), get(), get(), get()) }
    single<SyncGateway> { get<ActionController>() }
}
