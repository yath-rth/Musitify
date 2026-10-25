package com.yatharth.musitify.service

/*
 * Android kept these types in `com.yatharth.musitify.service` (SleepTimerManager.kt,
 * DownloadsManager.kt). On desktop they live in the engine; the aliases let screens ported 1:1
 * from Android keep their `com.yatharth.musitify.service.*` imports.
 */

typealias SleepTimerDuration = com.yatharth.musitify.engine.SleepTimerDuration
typealias SleepTimerState = com.yatharth.musitify.engine.SleepTimerState
typealias DownloadJob = com.yatharth.musitify.engine.DownloadJob
typealias DownloadStatus = com.yatharth.musitify.engine.DownloadStatus
typealias DownloadType = com.yatharth.musitify.engine.DownloadType
