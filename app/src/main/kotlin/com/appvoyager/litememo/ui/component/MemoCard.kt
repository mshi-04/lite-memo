package com.appvoyager.litememo.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.appvoyager.litememo.ui.state.TagUiModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MemoCard(
    memo: MemoUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFavoriteToggle: (() -> Unit)? = null
) {
    val accentColor = memoAccentColor(memo)

    Card(
        onClick = onClick,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = memo.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (onFavoriteToggle != null) {
                        IconButton(onClick = onFavoriteToggle) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = if (memo.isFavorite) {
                                    stringResource(R.string.remove_favorite_memo)
                                } else {
                                    stringResource(R.string.add_favorite_memo)
                                },
                                tint = if (memo.isFavorite) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                }
                            )
                        }
                    }
                }
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
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (memo.tags.isEmpty()) {
                            MemoTag(tag = null)
                        } else {
                            memo.tags.forEach { tag ->
                                MemoTag(tag = tag)
                            }
                        }
                    }
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
private fun MemoTag(tag: TagUiModel?) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (tag != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(tag.colorArgb.toInt()))
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = tag?.name ?: stringResource(R.string.unorganized_label),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun memoAccentColor(memo: MemoUiModel): Color {
    if (memo.isFavorite) return MaterialTheme.colorScheme.error
    return memo.tags.firstOrNull()?.colorArgb?.let { Color(it.toInt()) }
        ?: MaterialTheme.colorScheme.primary
}

@Composable
private fun updatedAtLabel(updatedAtMillis: Long): String {
    val zoneId = remember { ZoneId.systemDefault() }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("H:mm") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("M/d") }
    val today = LocalDate.now(zoneId)
    val updatedAt = remember(updatedAtMillis, zoneId) {
        Instant.ofEpochMilli(updatedAtMillis).atZone(zoneId)
    }

    return when (updatedAt.toLocalDate()) {
        today -> timeFormatter.format(updatedAt)
        today.minusDays(1) -> stringResource(R.string.yesterday_label)
        else -> dateFormatter.format(updatedAt)
    }
}
