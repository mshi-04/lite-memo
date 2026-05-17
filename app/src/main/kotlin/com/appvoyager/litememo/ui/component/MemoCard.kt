package com.appvoyager.litememo.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.appvoyager.litememo.R
import com.appvoyager.litememo.ui.state.MemoUiModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MemoCard(memo: MemoUiModel, modifier: Modifier = Modifier) {
    val accentColor = memoAccentColor(memo)

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        color = accentColor,
                        size = size.copy(width = 4.dp.toPx())
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 18.dp, top = 14.dp, end = 14.dp, bottom = 14.dp)
            ) {
                Text(
                    text = memo.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = memo.body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MemoTag(label = memo.tagName)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = updatedAtLabel(memo.updatedAtMillis),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoTag(label: String?) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Text(
            text = label ?: stringResource(R.string.unorganized_label),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun memoAccentColor(memo: MemoUiModel): Color {
    if (memo.isImportant) return MaterialTheme.colorScheme.error
    return memo.tagColorArgb?.let { Color(it.toULong()) } ?: MaterialTheme.colorScheme.primary
}

@Composable
private fun updatedAtLabel(updatedAtMillis: Long): String {
    val zoneId = remember { ZoneId.systemDefault() }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("H:mm") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("M/d") }
    val today = remember { LocalDate.now(zoneId) }
    val updatedAt = remember(updatedAtMillis, zoneId) {
        Instant.ofEpochMilli(updatedAtMillis).atZone(zoneId)
    }

    return when (updatedAt.toLocalDate()) {
        today -> timeFormatter.format(updatedAt)
        today.minusDays(1) -> stringResource(R.string.yesterday_label)
        else -> dateFormatter.format(updatedAt)
    }
}
