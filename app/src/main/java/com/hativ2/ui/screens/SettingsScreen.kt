package com.hativ2.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
