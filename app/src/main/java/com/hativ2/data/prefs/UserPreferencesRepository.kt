package com.hativ2.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hativ2.ui.theme.DensityPreset
import com.hativ2.ui.theme.TextSizePreset
import com.hativ2.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class UserPrefs(
    val themeMode: ThemeMode = ThemeMode.System,
    val density: DensityPreset = DensityPreset.Standard,
    val textSize: TextSizePreset = TextSizePreset.Medium
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val ds: DataStore<Preferences>
) {
    val flow: Flow<UserPrefs> = ds.data.map { prefs ->
        UserPrefs(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.System,
            density   = prefs[Keys.DENSITY]?.let { runCatching { DensityPreset.valueOf(it) }.getOrNull() } ?: DensityPreset.Standard,
            textSize  = prefs[Keys.TEXT_SIZE]?.let { runCatching { TextSizePreset.valueOf(it) }.getOrNull() } ?: TextSizePreset.Medium
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        ds.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDensity(density: DensityPreset) {
        ds.edit { it[Keys.DENSITY] = density.name }
    }

    suspend fun setTextSize(size: TextSizePreset) {
        ds.edit { it[Keys.TEXT_SIZE] = size.name }
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DENSITY    = stringPreferencesKey("density_scale")
        val TEXT_SIZE  = stringPreferencesKey("text_size")
    }
}
