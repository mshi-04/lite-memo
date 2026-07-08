package com.appvoyager.litememo.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.appvoyager.litememo.R
import com.appvoyager.litememo.ui.theme.LiteMemoTheme
import kotlinx.coroutines.launch

@Composable
fun TutorialScreen(onCompleteTutorial: () -> Unit, modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(pageCount = { tutorialPages.size })
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCompleteTutorial) {
                    Text(text = stringResource(R.string.tutorial_skip))
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                TutorialPage(page = tutorialPages[page])
            }

            TutorialNavigation(
                currentPage = pagerState.currentPage,
                pageCount = tutorialPages.size,
                onPreviousClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                },
                onNextClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                onCompleteTutorial = onCompleteTutorial
            )
        }
    }
}

@Composable
private fun TutorialPage(page: TutorialPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(page.titleResId),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(page.bodyResId),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TutorialNavigation(
    currentPage: Int,
    pageCount: Int,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onCompleteTutorial: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            IconButton(
                onClick = onPreviousClick,
                enabled = currentPage > 0
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.tutorial_previous_page)
                )
            }
        }

        PageIndicator(
            currentPage = currentPage,
            pageCount = pageCount
        )

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (currentPage == pageCount - 1) {
                Button(onClick = onCompleteTutorial) {
                    Text(text = stringResource(R.string.tutorial_start))
                }
            } else {
                IconButton(onClick = onNextClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.tutorial_next_page)
                    )
                }
            }
        }
    }
}

@Composable
private fun PageIndicator(currentPage: Int, pageCount: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val color = if (index == currentPage) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color = color, shape = CircleShape)
            )
        }
    }
}

private data class TutorialPage(
    val icon: ImageVector,
    @get:StringRes val titleResId: Int,
    @get:StringRes val bodyResId: Int
)

private val tutorialPages = listOf(
    TutorialPage(
        icon = Icons.Default.Edit,
        titleResId = R.string.tutorial_page_welcome_title,
        bodyResId = R.string.tutorial_page_welcome_body
    ),
    TutorialPage(
        icon = Icons.Default.Search,
        titleResId = R.string.tutorial_page_organize_title,
        bodyResId = R.string.tutorial_page_organize_body
    ),
    TutorialPage(
        icon = Icons.Default.DateRange,
        titleResId = R.string.tutorial_page_calendar_title,
        bodyResId = R.string.tutorial_page_calendar_body
    )
)

@Preview(showBackground = true)
@Composable
private fun TutorialScreenPreview() {
    LiteMemoTheme {
        TutorialScreen(onCompleteTutorial = {})
    }
}
