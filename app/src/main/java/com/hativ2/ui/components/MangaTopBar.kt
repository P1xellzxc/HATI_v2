package com.hativ2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Reusable top app bar matching the mockup style: optional left
 * slot (back / logo), uppercase title with optional subtitle, optional
 * right slot, and a 2dp solid bottom divider.
 *
 * Use this for every screen instead of Material's TopAppBar — it
 * keeps the narrative manga look consistent across the app.
 */
@Composable
fun MangaTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    centerTitle: Boolean = false,
    left: @Composable () -> Unit = {},
    right: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left slot — back button or logo cluster.
            Box(contentAlignment = Alignment.CenterStart) { left() }

            // Centered title block.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                horizontalAlignment = if (centerTitle) Alignment.CenterHorizontally else Alignment.Start
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = if (centerTitle) TextAlign.Center else TextAlign.Start
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = if (centerTitle) TextAlign.Center else TextAlign.Start
                    )
                }
            }

            // Right slot — actions.
            Box(contentAlignment = Alignment.CenterEnd) { right() }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MangaBorderWidth)
                .background(MaterialTheme.colorScheme.onSurface)
        )
    }
}
