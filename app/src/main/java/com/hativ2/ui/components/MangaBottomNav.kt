package com.hativ2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String,
)

/**
 * Top-bordered nav row. The active item gets a thick black bar above
 * the icon (matches the mockup), bold label and full-opacity tint;
 * the inactive items are muted. Used by the global Scaffold on
 * top-level destinations (Hub, Dashboard, History, Charts).
 */
@Composable
fun MangaBottomNav(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MangaBorderWidth)
                .background(MaterialTheme.colorScheme.onSurface)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            items.forEach { item ->
                BottomNavCell(
                    item = item,
                    isActive = isRouteActive(currentRoute, item.route),
                    onClick = { onClick(item.route) }
                )
            }
        }
    }
}

private fun isRouteActive(currentRoute: String?, itemRoute: String): Boolean {
    if (currentRoute == null) return false
    if (currentRoute == itemRoute) return true
    // Match parameterized routes like "dashboard_detail/{dashboardId}"
    // against the bare base route registered in the nav item.
    val currentBase = currentRoute.substringBefore("/").substringBefore("?")
    val itemBase = itemRoute.substringBefore("/").substringBefore("?")
    return currentBase == itemBase
}

@Composable
private fun BottomNavCell(
    item: BottomNavItem,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val tint = if (isActive) accent else muted

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick)
    ) {
        // Active top indicator — thicker bar that hangs off the divider.
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(3.dp)
                .background(if (isActive) accent else Color.Transparent)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = tint
        )
    }
}
