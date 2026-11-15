package app.shiguang.ccd.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.shiguang.imaging.LookJson
import app.shiguang.imaging.LookParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "shiguang_settings")

class SettingsStore(private val context: Context) {

    private val lastPresetKey = stringPreferencesKey("last_preset")
    private val customPresetsKey = stringPreferencesKey("custom_presets")
    private val keepOriginalKey = booleanPreferencesKey("keep_original")

    val lastPresetId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[lastPresetKey] ?: ""
    }

    val customPresets: Flow<List<LookParams>> = context.dataStore.data.map { prefs ->
        prefs[customPresetsKey]?.let { LookJson.decodeList(it) } ?: emptyList()
    }

    val keepOriginal: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[keepOriginalKey] ?: false
    }

    suspend fun setLastPreset(id: String) {
        context.dataStore.edit { it[lastPresetKey] = id }
    }

    suspend fun setCustomPresets(presets: List<LookParams>) {
        context.dataStore.edit { it[customPresetsKey] = LookJson.encodeList(presets) }
    }

    suspend fun setKeepOriginal(value: Boolean) {
        context.dataStore.edit { it[keepOriginalKey] = value }
    }
}
