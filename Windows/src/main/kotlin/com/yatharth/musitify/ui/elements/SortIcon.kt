package com.yatharth.musitify.ui.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yatharth.musitify.ui.theme.cardBorder
import com.yatharth.musitify.ui.theme.text

enum class SortMode(val label: String) {
    DOWNLOADED_DESC("NEWEST"),
    DOWNLOADED_ASC("OLDEST"),
    PLAYS_DESC("MOST PLAYED"),
    PLAYS_ASC("LEAST PLAYED");

    fun next(): SortMode {
        val values = entries
        return values[(ordinal + 1) % values.size]
    }
}

/** Sort button that says the current order in words. Click cycles to the next order. */
@Composable
fun SortIcon(mode: SortMode, onClick: () -> Unit) {
    CustomButton(
        onClick = onClick,
        modifier = Modifier.semantics {
            contentDescription = "Sorted by ${mode.label.lowercase()}. Click to change."
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Sort,
                contentDescription = null,
                tint = cardBorder,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = mode.label,
                color = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
