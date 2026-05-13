package com.appvoyager.litememo.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appvoyager.litememo.R

@Composable
fun CalendarPlaceholderScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.calendar_placeholder_title),
        body = stringResource(R.string.calendar_placeholder_body),
        modifier = modifier
    )
}

@Composable
fun SettingsPlaceholderScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.settings_placeholder_title),
        body = stringResource(R.string.settings_placeholder_body),
        modifier = modifier
    )
}

@Composable
private fun PlaceholderScreen(title: String, body: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
