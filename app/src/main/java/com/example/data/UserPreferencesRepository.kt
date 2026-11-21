package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.SoundAlertStyle
import com.example.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "devcraft_prefs")

data class UserPreferences(
  val isArabic: Boolean = true,
  val themeMode: ThemeMode = ThemeMode.DARK,
  val dynamicColor: Boolean = false,
  val customFocusMinutes: Int = 30,
  val customBreakMinutes: Int = 5,
  val completedSessionsToday: Int = 0,
  val savedConfigJson: String? = null,
  val soundAlertEnabled: Boolean = true,
  val soundAlertStyle: SoundAlertStyle = SoundAlertStyle.HARMONIC_CHIME,
  val hasSeenTutorial: Boolean = false,
)

class UserPreferencesRepository(private val context: Context) {

  private object Keys {
    val IS_ARABIC = booleanPreferencesKey("is_arabic")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val FOCUS_MINUTES = intPreferencesKey("custom_focus_minutes")
    val BREAK_MINUTES = intPreferencesKey("custom_break_minutes")
    val COMPLETED_SESSIONS = intPreferencesKey("completed_sessions_today")
    val SAVED_CONFIG_JSON = stringPreferencesKey("saved_config_json")
    val SOUND_ALERT_ENABLED = booleanPreferencesKey("sound_alert_enabled")
    val SOUND_ALERT_STYLE = stringPreferencesKey("sound_alert_style")
    val HAS_SEEN_TUTORIAL = booleanPreferencesKey("has_seen_tutorial")
  }

  val preferencesFlow: Flow<UserPreferences> =
    context.dataStore.data.map { prefs ->
      val themeName = prefs[Keys.THEME_MODE] ?: ThemeMode.DARK.name
      val themeMode = runCatching { ThemeMode.valueOf(themeName) }.getOrDefault(ThemeMode.DARK)
      val styleName = prefs[Keys.SOUND_ALERT_STYLE] ?: SoundAlertStyle.HARMONIC_CHIME.name
      val soundStyle =
        runCatching { SoundAlertStyle.valueOf(styleName) }
          .getOrDefault(SoundAlertStyle.HARMONIC_CHIME)
      UserPreferences(
        isArabic = prefs[Keys.IS_ARABIC] ?: true,
        themeMode = themeMode,
        dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
        customFocusMinutes = (prefs[Keys.FOCUS_MINUTES] ?: 30).coerceIn(5, 90),
        customBreakMinutes = (prefs[Keys.BREAK_MINUTES] ?: 5).coerceIn(1, 30),
        completedSessionsToday = prefs[Keys.COMPLETED_SESSIONS] ?: 0,
        savedConfigJson = prefs[Keys.SAVED_CONFIG_JSON],
        soundAlertEnabled = prefs[Keys.SOUND_ALERT_ENABLED] ?: true,
        soundAlertStyle = soundStyle,
        hasSeenTutorial = prefs[Keys.HAS_SEEN_TUTORIAL] ?: false,
      )
    }

  suspend fun setArabic(isArabic: Boolean) {
    context.dataStore.edit { it[Keys.IS_ARABIC] = isArabic }
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
  }

  suspend fun setDynamicColor(enabled: Boolean) {
    context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
  }

  suspend fun setCustomFocusMinutes(minutes: Int) {
    context.dataStore.edit { it[Keys.FOCUS_MINUTES] = minutes.coerceIn(5, 90) }
  }

  suspend fun setCustomBreakMinutes(minutes: Int) {
    context.dataStore.edit { it[Keys.BREAK_MINUTES] = minutes.coerceIn(1, 30) }
  }

  suspend fun incrementCompletedSessions() {
    context.dataStore.edit { prefs ->
      val current = prefs[Keys.COMPLETED_SESSIONS] ?: 0
      prefs[Keys.COMPLETED_SESSIONS] = current + 1
    }
  }

  suspend fun saveConfigJson(json: String?) {
    context.dataStore.edit { prefs ->
      if (json == null) {
        prefs.remove(Keys.SAVED_CONFIG_JSON)
      } else {
        prefs[Keys.SAVED_CONFIG_JSON] = json
      }
    }
  }

  suspend fun setSoundAlertEnabled(enabled: Boolean) {
    context.dataStore.edit { it[Keys.SOUND_ALERT_ENABLED] = enabled }
  }

  suspend fun setSoundAlertStyle(style: SoundAlertStyle) {
    context.dataStore.edit { it[Keys.SOUND_ALERT_STYLE] = style.name }
  }

  suspend fun setHasSeenTutorial(seen: Boolean) {
    context.dataStore.edit { it[Keys.HAS_SEEN_TUTORIAL] = seen }
  }
}
