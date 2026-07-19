package com.yatharth.musitify.util

object Formatter {

    fun formatTime(value: Long): String {
        val seconds = (value / 1000).toInt()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60

        return String.format("%d:%02d", minutes, remainingSeconds)
    }

}