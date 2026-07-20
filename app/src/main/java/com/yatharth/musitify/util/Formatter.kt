package com.yatharth.musitify.util

object Formatter {

    fun formatTime(value: Long): String {
        val seconds = (value / 1000).toInt()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60

        return String.format("%d:%02d", minutes, remainingSeconds)
    }

    fun formatTimeToMinutes(value: Long): String {
        val seconds = (value / 1000)
        val minutes = seconds / 60
        val hours = minutes / 60

        return when (hours) {
            0L -> "${(minutes).toInt()} mins"
            1L -> "${hours.toInt()} hr ${(minutes - hours * 60).toInt()} mins"
            else -> "${hours.toInt()} hrs ${(minutes - hours * 60).toInt()} mins"
        }
    }

}