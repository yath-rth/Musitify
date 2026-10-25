package com.yatharth.musitify.data.repository

/**
 * What [com.yatharth.musitify.data.viewmodel.HomeScreenViewModel] needs from Android's
 * `network.ActionController` (push the outbox / pull and apply remote actions).
 *
 * Bound to `network.ActionController` by `network.syncModule`, which is loaded after
 * `repositoryModule` and overrides the [NoOpSyncGateway] default.
 *
 * Desktop: both calls return a count (Android returns Unit) and throw on failure, so the UI can
 * report the outcome.
 */
interface SyncGateway {
    /** Pushes the outbox; returns how many actions the server accepted. */
    suspend fun pushActions(): Int

    /** Pulls and applies remote actions; returns how many new (non-echo) actions were received. */
    suspend fun pullActions(): Int
}

/** Default until sync is wired: does nothing. */
object NoOpSyncGateway : SyncGateway {
    override suspend fun pushActions() = 0
    override suspend fun pullActions() = 0
}
