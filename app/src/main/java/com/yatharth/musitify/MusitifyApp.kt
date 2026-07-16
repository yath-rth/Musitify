package com.yatharth.musitify

import android.app.Application
import com.yatharth.musitify.data.repository.MediaControllerRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MusitifyApp : Application() {
    @Inject lateinit var mediaControllerRepository: MediaControllerRepository

    override fun onCreate() {
        super.onCreate()

        mediaControllerRepository.connect()
    }
}