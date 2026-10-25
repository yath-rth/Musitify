package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class SortMode {
    DOWNLOADED_DESC, DOWNLOADED_ASC, PLAYS_DESC, PLAYS_ASC;

    fun next(): SortMode {
        val values = entries
        return values[(ordinal + 1) % values.size]
    }
}

@Composable
fun SortIcon(mode: SortMode, onClick: () -> Unit) {
    val (fieldIcon, arrowIcon) = when (mode) {
        SortMode.DOWNLOADED_DESC -> Icons.Default.Schedule to Icons.Default.ArrowDownward
        SortMode.DOWNLOADED_ASC -> Icons.Default.Schedule to Icons.Default.ArrowUpward
        SortMode.PLAYS_DESC -> Icons.Default.Headphones to Icons.Default.ArrowDownward
        SortMode.PLAYS_ASC -> Icons.Default.Headphones to Icons.Default.ArrowUpward
    }

    CustomCircularButton(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(fieldIcon, contentDescription = "Sort field", modifier = Modifier.size(18.dp))
            Icon(arrowIcon, contentDescription = "Sort direction", modifier = Modifier.size(14.dp))
        }
    }
}
