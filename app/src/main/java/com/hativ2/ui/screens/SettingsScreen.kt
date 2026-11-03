package com.hativ2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import com.hativ2.ui.components.MangaBorderWidth
import com.hativ2.ui.components.MangaCornerRadius
import com.hativ2.ui.theme.HatiPalette
import com.hativ2.ui.theme.HatiPalettes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hativ2.R
import com.hativ2.ui.MainViewModel
import com.hativ2.ui.components.MangaCard
import com.hativ2.ui.components.MangaIconButton
import com.hativ2.ui.components.MangaSegmentedControl
import com.hativ2.ui.components.MangaTopBar
import com.hativ2.ui.theme.DensityPreset
import com.hativ2.ui.theme.TextSizePreset
import com.hativ2.ui.theme.ThemeMode
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val prefs by viewModel.prefs.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MangaTopBar(
                title = stringResource(R.string.settings_title),
                left = {
                    MangaIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onBack,
                        contentDescription = stringResource(R.string.cta_back),
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .align(Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                SettingsSection(title = stringResource(R.string.settings_appearance)) {
                    MangaSegmentedControl(
                        options = listOf(
                            ThemeMode.System to stringResource(R.string.settings_theme_system),
                            ThemeMode.Light  to stringResource(R.string.settings_theme_light),
                            ThemeMode.Dark   to stringResource(R.string.settings_theme_dark),
                        ),
                        selected = prefs.themeMode,
                        onSelect = viewModel::setThemeMode
                    )
                }

                SettingsSection(
                    title = stringResource(R.string.settings_palette),
                    helper = stringResource(R.string.settings_palette_helper)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        HatiPalettes.all.forEach { palette ->
                            PaletteRow(
                                palette = palette,
                                selected = palette.id == prefs.paletteId,
                                onClick = { viewModel.setPalette(palette.id) }
                            )
                        }
                    }
                }

                SettingsSection(
                    title = stringResource(R.string.settings_density),
                    helper = stringResource(R.string.settings_density_helper)
                ) {
                    MangaSegmentedControl(
                        options = listOf(
                            DensityPreset.Compact     to stringResource(R.string.settings_density_compact),
                            DensityPreset.Standard    to stringResource(R.string.settings_density_standard),
                            DensityPreset.Comfortable to stringResource(R.string.settings_density_comfortable),
                        ),
                        selected = prefs.density,
                        onSelect = viewModel::setDensity
                    )
                }

                SettingsSection(
                    title = stringResource(R.string.settings_text_size),
                    helper = stringResource(R.string.settings_text_size_helper)
                ) {
                    MangaSegmentedControl(
                        options = listOf(
                            TextSizePreset.Small  to stringResource(R.string.settings_text_size_small),
                            TextSizePreset.Medium to stringResource(R.string.settings_text_size_medium),
                            TextSizePreset.Large  to stringResource(R.string.settings_text_size_large),
                        ),
                        selected = prefs.textSize,
                        onSelect = viewModel::setTextSize
                    )
                }

                SettingsSection(title = stringResource(R.string.settings_about)) {
                    Text(
                        text = stringResource(R.string.settings_about_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    helper: String? = null,
    content: @Composable () -> Unit,
) {
    MangaCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (helper != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = helper,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun PaletteRow(
    palette: HatiPalette,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(MangaCornerRadius)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.paper, shape)
            .border(if (selected) 3.dp else MangaBorderWidth, palette.ink, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .height(28.dp)
                .width(72.dp)
                .border(1.dp, palette.ink)
        ) {
            palette.swatches.forEach { swatch ->
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(swatch))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = palette.name.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = palette.ink
            )
            Text(
                text = palette.wadaNumber?.let { stringResource(R.string.settings_palette_plate, it) }
                    ?: stringResource(R.string.settings_palette_original),
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted
            )
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(palette.accent, shape)
                .border(MangaBorderWidth, palette.ink, shape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = palette.ink,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
