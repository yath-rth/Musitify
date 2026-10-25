package com.yatharth.musitify.data.repository

/**
 * What [com.yatharth.musitify.data.viewmodel.HomeScreenViewModel] needs from Android's
 * `network.ActionController` (push the outbox / pull and apply remote actions).
 *
 * The network port should bind its ActionController as this interface in Koin, replacing the
 * [NoOpSyncGateway] default from `repositoryModule` (Koin 4 lets a later module override it).
 */
interface SyncGateway {
    suspend fun pushActions()
    suspend fun pullActions()
}

/** Default until sync is ported: does nothing. */
object NoOpSyncGateway : SyncGateway {
    override suspend fun pushActions() = Unit
    override suspend fun pullActions() = Unit
}
