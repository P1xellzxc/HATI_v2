package com.hativ2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class CategoryChipItem(
    val value: String,
    val label: String,
    val emoji: String,
    val selectedColor: Color,
)

/**
 * 3-per-row category chip grid matching Mockup 3. The selected
 * chip fills with its category color (Food → peach, Transport →
 * blue, etc.) and gets a 2dp black border; all others stay
 * outlined-only.
 *
 * Uses a manual two-Row layout rather than LazyVerticalGrid so it
 * can be embedded inside a verticalScroll Column without nested-
 * scroll conflicts.
 */
@Composable
fun MangaCategoryChipGrid(
    categories: List<CategoryChipItem>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 3,
) {
    val rows = categories.chunked(columns)
    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxWidth()) {
        rows.forEachIndexed { index, row ->
            if (index > 0) Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { cat ->
                    CategoryChip(
                        item = cat,
                        isSelected = cat.value == selected,
                        onClick = { onSelect(cat.value) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Pad short rows so chips keep equal width.
                repeat(columns - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    item: CategoryChipItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(
                if (isSelected) item.selectedColor else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(MangaCornerRadius)
            )
            .border(
                MangaBorderWidth,
                MaterialTheme.colorScheme.onSurface,
                RoundedCornerShape(MangaCornerRadius)
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
