package com.yatharth.musitify.util

object Formatter {

    /** "1 song", "2 songs". Pass [plural] for irregular words. */
    fun count(n: Number, singular: String, plural: String = singular + "s"): String =
        "$n ${if (n.toLong() == 1L) singular else plural}"

    private fun mins(n: Long) = count(n, "min")

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
            0L -> mins(minutes)
            1L -> "1 hr ${mins(minutes - 60)}"
            else -> "$hours hrs ${mins(minutes - hours * 60)}"
        }
    }

    /**
     * Human-friendly listen duration used in stats, e.g. "45 mins",
     * "1 hr 12 mins", "5 hrs". Omits trailing zero minutes.
     */
    fun formatListenTime(value: Long): String {
        if (value <= 0) return "0 mins"
        // Some listening, but under a minute: "0 mins" reads as nothing at all.
        if (value < 60000) return "<1 min"
        val minutes = value / 60000
        val hours = minutes / 60
        val remMinutes = minutes % 60

        val hourPart = when (hours) {
            0L -> ""
            1L -> "1 hr"
            else -> "$hours hrs"
        }
        val minutePart = if (remMinutes != 0L || hours == 0L) mins(remMinutes) else ""

        return listOf(hourPart, minutePart)
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

    /** Short label for a day-of-month value, e.g. "12". */
    fun formatDayNumber(value: Long): String = value.toString()

}