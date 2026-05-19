package com.hativ2.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hativ2.R
import com.hativ2.domain.model.DashboardWithStats
import com.hativ2.ui.MainViewModel
import com.hativ2.ui.components.AddDashboardDialog
import com.hativ2.ui.components.EditDashboardDialog
import com.hativ2.ui.components.MangaBorderWidth
import com.hativ2.ui.components.MangaButton
import com.hativ2.ui.components.MangaCornerRadius
import com.hativ2.ui.components.MangaDashedCard
import com.hativ2.ui.components.MangaDeleteDialog
import com.hativ2.ui.components.MangaIconButton
import com.hativ2.ui.components.MangaTopBar
import com.hativ2.ui.theme.AccentPositive
import com.hativ2.ui.theme.MangaBlack
import com.hativ2.ui.theme.NotionGreen
import com.hativ2.ui.theme.NotionMuted
import com.hativ2.ui.theme.NotionRed
import com.hativ2.ui.theme.NotionYellow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DashboardListScreen(
    viewModel: MainViewModel = viewModel(),
    onDashboardClick: (String) -> Unit,
    onOpenSettings: () -> Unit = {},
) {
    val dashboardsWithStats by viewModel.dashboardsWithStats.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<DashboardWithStats?>(null) }
    var showEditDialog by remember { mutableStateOf<DashboardWithStats?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    if (showAddDialog) {
        AddDashboardDialog(
            onDismiss = { showAddDialog = false },
            onCreate = { title, type, color ->
                viewModel.createDashboard(title, type, color)
                showAddDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.volume_created, title)
                    )
                }
            }
        )
    }

    showDeleteDialog?.let { dashboard ->
        MangaDeleteDialog(
            title = stringResource(R.string.volume_delete_title),
            text = stringResource(R.string.volume_delete_body, dashboard.title),
            onConfirm = {
                val title = dashboard.title
                viewModel.deleteDashboard(dashboard.id)
                showDeleteDialog = null
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.volume_deleted, title)
                    )
                }
            },
            onDismiss = { showDeleteDialog = null }
        )
    }

    showEditDialog?.let { dashboard ->
        EditDashboardDialog(
            title = dashboard.title,
            type = dashboard.dashboardType,
            color = dashboard.themeColor,
            onDismiss = { showEditDialog = null },
            onSave = { newTitle, newType, newColor ->
                viewModel.updateDashboard(dashboard.id, newTitle, newType, newColor)
                showEditDialog = null
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MangaTopBar(
                title = "HATI",
                left = {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(MangaCornerRadius))
                            .border(MangaBorderWidth, MangaBlack, RoundedCornerShape(MangaCornerRadius))
                    )
                },
                right = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MangaIconButton(
                            icon = Icons.Default.Info,
                            onClick = onOpenSettings,
                            contentDescription = stringResource(R.string.cta_help),
                        )
                        MangaButton(
                            onClick = { showAddDialog = true },
                            backgroundColor = NotionYellow,
                            contentColor = MangaBlack,
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = stringResource(R.string.cta_new_volume_short).uppercase(),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .widthIn(max = 600.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.hub_headline),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.hub_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(dashboardsWithStats, key = { it.id }) { dashboard ->
                    val index = dashboardsWithStats.indexOf(dashboard)
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(dashboard.id) {
                        delay(index * 50L)
                        visible = true
                    }
                    val alpha by animateFloatAsState(
                        targetValue = if (visible) 1f else 0f,
                        animationSpec = tween(400, easing = FastOutSlowInEasing),
                        label = "itemAlpha"
                    )
                    val offsetY by animateDpAsState(
                        targetValue = if (visible) 0.dp else 24.dp,
                        animationSpec = tween(400, easing = FastOutSlowInEasing),
                        label = "itemOffset"
                    )

                    Box(modifier = Modifier.alpha(alpha).offset(y = offsetY)) {
                        VolumeCard(
                            dashboard = dashboard,
                            onClick = { onDashboardClick(dashboard.id) },
                            onDelete = { showDeleteDialog = dashboard },
                            onEdit = { showEditDialog = dashboard }
                        )
                    }
                }

                item {
                    NewVolumeDashedCard(onClick = { showAddDialog = true })
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

/**
 * Mockup 1 layout: full-width card with a top "cover" section
 * (corner edit / trash buttons + centered hero emoji + balance pill)
 * and a bottom "spine" section with title / type / total. Rule of
 * thirds — the hero claims ~⅓ of the card vertically, info ⅔.
 */
@Composable
fun VolumeCard(
    dashboard: DashboardWithStats,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    val coverTint = MaterialTheme.colorScheme.surfaceVariant
    val balanceColor = if (dashboard.netBalance >= 0) AccentPositive else NotionRed
    val balanceSign = if (dashboard.netBalance >= 0) "+" else ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 4.dp, bottom = 4.dp)
    ) {
        // Hard offset shadow — the "Surface" depth layer.
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .background(MangaBlack, RoundedCornerShape(MangaCornerRadius))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(MangaCornerRadius))
                .border(MangaBorderWidth, MangaBlack, RoundedCornerShape(MangaCornerRadius))
                .clip(RoundedCornerShape(MangaCornerRadius))
        ) {
            // ── Cover (hero) section ──────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(coverTint)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MangaIconButton(
                        icon = Icons.Default.Edit,
                        onClick = onEdit,
                        contentDescription = stringResource(R.string.action_edit),
                        size = 36.dp,
                        iconSize = 18.dp
                    )
                    MangaIconButton(
                        icon = Icons.Default.Delete,
                        onClick = onDelete,
                        contentDescription = stringResource(R.string.cta_delete),
                        size = 36.dp,
                        iconSize = 18.dp,
                        tint = NotionRed
                    )
                }

                Text(
                    text = emojiFor(dashboard.dashboardType),
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.align(Alignment.Center)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(balanceColor.copy(alpha = 0.35f), RoundedCornerShape(MangaCornerRadius))
                        .border(MangaBorderWidth, MangaBlack, RoundedCornerShape(MangaCornerRadius))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$balanceSign${dashboard.currencySymbol}${"%,.2f".format(dashboard.netBalance)}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = MangaBlack
                    )
                }
            }

            // 2dp section divider.
            Box(modifier = Modifier.fillMaxWidth().height(MangaBorderWidth).background(MangaBlack))

            // ── Info section ─────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(16.dp)
            ) {
                Text(
                    text = dashboard.title.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${emojiFor(dashboard.dashboardType)} ${dashboard.dashboardType.replaceFirstChar { it.uppercase() }} · ${
                        stringResource(R.string.volume_chapters_short, dashboard.expenseCount)
                    }",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MangaBlack))
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.volume_total_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${dashboard.currencySymbol}${"%,.2f".format(dashboard.totalSpent)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/** Dashed-border "+ NEW VOLUME" empty slot at the bottom of the list. */
@Composable
fun NewVolumeDashedCard(onClick: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "plusPulse")
    val pulseScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "plusScale"
    )

    MangaDashedCard(
        onClick = onClick,
        padding = 24.dp
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .scale(pulseScale)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(MangaCornerRadius))
                    .border(MangaBorderWidth, MangaBlack, RoundedCornerShape(MangaCornerRadius)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MangaBlack)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.cta_new_volume_short).uppercase(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.hub_new_volume_helper),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun emojiFor(type: String) = when (type.lowercase()) {
    "travel"    -> "✈️"
    "household" -> "🏠"
    "event"     -> "🎉"
    else        -> "📁"
}
