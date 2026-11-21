package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.AndroidPomodoroSoundNotifier
import com.example.audio.PomodoroSoundNotifier
import com.example.data.CloudWorkspaceRepository
import com.example.data.ConfigRepository
import com.example.data.DevUtilitiesEngine
import com.example.data.FocusSessionLog
import com.example.data.SavedConfig
import com.example.data.UserPreferences
import com.example.data.UserPreferencesRepository
import com.example.data.UserProfile
import com.example.model.CloudUiState
import com.example.model.ConfigPreset
import com.example.model.ConfigViewMode
import com.example.model.DevToolTab
import com.example.model.FocusPhase
import com.example.model.FocusPreset
import com.example.model.SettingNode
import com.example.model.SoundAlertStyle
import com.example.model.ThemeMode
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConfigUiState(
  val jsonText: String = "",
  val nodes: List<SettingNode> = emptyList(),
  val isValidJson: Boolean = true,
  val jsonError: String? = null,
  val searchQuery: String = "",
  val viewMode: ConfigViewMode = ConfigViewMode.VISUAL,
  val activePresetId: String = "attached",
  val presets: List<ConfigPreset> = emptyList(),
)

data class FocusUiState(
  val phase: FocusPhase = FocusPhase.FOCUS,
  val preset: FocusPreset = FocusPreset.CLASSIC,
  val totalSeconds: Int = 25 * 60,
  val remainingSeconds: Int = 25 * 60,
  val isRunning: Boolean = false,
  val taskName: String = "",
  val isSyncingToCloud: Boolean = false,
  val isSyncedToCloud: Boolean = false,
  val hasResumedFromCloud: Boolean = false,
)

data class ToolsUiState(
  val activeTab: DevToolTab = DevToolTab.JSON_LAB,
  // JSON Lab
  val jsonInput: String = "",
  val jsonOutput: String = "",
  val jsonIndent: Int = 4,
  val jsonLabError: String? = null,
  // Codec
  val isBase64Mode: Boolean = true,
  val codecInput: String = "DevCraft Studio • تطوير ذكي",
  val codecOutput: String = "",
  val codecError: String? = null,
  // Generator
  val generatedUuids: List<String> = emptyList(),
  val generatedToken: String = "",
  val tokenLength: Int = 24,
  val includeUpper: Boolean = true,
  val includeLower: Boolean = true,
  val includeDigits: Boolean = true,
  val includeSymbols: Boolean = true,
  // Color
  val red: Int = 56,
  val green: Int = 189,
  val blue: Int = 248,
)

class DevCraftViewModel(
  private val prefsRepo: UserPreferencesRepository,
  private val configRepo: ConfigRepository,
  private val soundNotifier: PomodoroSoundNotifier,
  private val currentUserId: String = "",
  private val cloudRepo: CloudWorkspaceRepository? = null,
) : ViewModel() {

  private val _prefsState = MutableStateFlow(UserPreferences())
  val prefsState: StateFlow<UserPreferences> = _prefsState.asStateFlow()

  private val _configState = MutableStateFlow(ConfigUiState())
  val configState: StateFlow<ConfigUiState> = _configState.asStateFlow()

  private val _focusState = MutableStateFlow(FocusUiState())
  val focusState: StateFlow<FocusUiState> = _focusState.asStateFlow()

  private val _toolsState = MutableStateFlow(ToolsUiState())
  val toolsState: StateFlow<ToolsUiState> = _toolsState.asStateFlow()

  private val _phaseCompletedEvent = MutableSharedFlow<FocusPhase>(extraBufferCapacity = 4)
  val phaseCompletedEvent: SharedFlow<FocusPhase> = _phaseCompletedEvent.asSharedFlow()

  private val _showTutorial = MutableStateFlow(false)
  val showTutorial: StateFlow<Boolean> = _showTutorial.asStateFlow()

  val cloudProfileState: StateFlow<CloudUiState<UserProfile?>> =
    if (cloudRepo != null && currentUserId.isNotBlank()) {
      cloudRepo
        .observeUserProfile(currentUserId)
        .map<UserProfile?, CloudUiState<UserProfile?>> { CloudUiState.Success(it) }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val rootError = firestoreError ?: error
          emit(CloudUiState.Error(rootError.message ?: "Failed to sync user profile"))
        }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000L),
          initialValue = CloudUiState.Loading,
        )
    } else {
      flowOf<CloudUiState<UserProfile?>>(CloudUiState.Success(null))
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000L),
          initialValue = CloudUiState.Success(null),
        )
    }

  val cloudConfigsState: StateFlow<CloudUiState<List<SavedConfig>>> =
    if (cloudRepo != null && currentUserId.isNotBlank()) {
      cloudRepo
        .observeSavedConfigs(currentUserId)
        .map<List<SavedConfig>, CloudUiState<List<SavedConfig>>> { CloudUiState.Success(it) }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val rootError = firestoreError ?: error
          emit(CloudUiState.Error(rootError.message ?: "Failed to load saved configs"))
        }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000L),
          initialValue = CloudUiState.Loading,
        )
    } else {
      flowOf<CloudUiState<List<SavedConfig>>>(CloudUiState.Success(emptyList()))
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000L),
          initialValue = CloudUiState.Success(emptyList()),
        )
    }

  val cloudSessionsState: StateFlow<CloudUiState<List<FocusSessionLog>>> =
    if (cloudRepo != null && currentUserId.isNotBlank()) {
      cloudRepo
        .observeFocusSessions(currentUserId)
        .map<List<FocusSessionLog>, CloudUiState<List<FocusSessionLog>>> {
          CloudUiState.Success(it)
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val rootError = firestoreError ?: error
          emit(CloudUiState.Error(rootError.message ?: "Failed to load focus history"))
        }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000L),
          initialValue = CloudUiState.Loading,
        )
    } else {
      flowOf<CloudUiState<List<FocusSessionLog>>>(CloudUiState.Success(emptyList()))
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000L),
          initialValue = CloudUiState.Success(emptyList()),
        )
    }

  private var timerJob: Job? = null
  private var taskSyncDebounceJob: Job? = null
  private var hasUserModifiedFocus = false

  init {
    val presets = configRepo.getPresets()
    val originalJson = configRepo.loadAttachedSettingsJson()

    if (cloudRepo != null && currentUserId.isNotBlank()) {
      viewModelScope.launch {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null && currentUser.uid == currentUserId) {
          cloudRepo.ensureUserProfile(currentUser)
        }
        cloudRepo.getActiveTimerState(currentUserId).onSuccess { saved ->
          if (saved != null && !hasUserModifiedFocus) {
            val restoredPreset =
              FocusPreset.entries.firstOrNull { it.name == saved.presetName } ?: FocusPreset.CLASSIC
            val restoredPhase =
              FocusPhase.entries.firstOrNull { it.name == saved.phase } ?: FocusPhase.FOCUS
            val restoredTotal = saved.totalSeconds.toInt().coerceIn(60, 10800)
            val restoredRemaining = saved.remainingSeconds.toInt().coerceIn(0, restoredTotal)
            val resumed =
              restoredRemaining < restoredTotal ||
                saved.taskName.isNotBlank() ||
                restoredPhase == FocusPhase.BREAK
            _focusState.value =
              FocusUiState(
                phase = restoredPhase,
                preset = restoredPreset,
                totalSeconds = restoredTotal,
                remainingSeconds = restoredRemaining,
                isRunning = false,
                taskName = saved.taskName,
                isSyncingToCloud = false,
                isSyncedToCloud = true,
                hasResumedFromCloud = resumed,
              )
          }
        }
      }
    }

    viewModelScope.launch {
      val initialPrefs = prefsRepo.preferencesFlow.first()
      _prefsState.value = initialPrefs
      if (!initialPrefs.hasSeenTutorial) {
        _showTutorial.value = true
      }

      val startJson = initialPrefs.savedConfigJson ?: originalJson
      val parsed = configRepo.parseConfigNodes(startJson)
      _configState.value =
        ConfigUiState(
          jsonText = startJson,
          nodes = parsed.getOrDefault(emptyList()),
          isValidJson = parsed.isSuccess,
          jsonError = parsed.exceptionOrNull()?.localizedMessage,
          presets = presets,
          activePresetId = if (startJson == originalJson) "attached" else "custom",
        )

      _toolsState.update {
        it.copy(
          jsonInput = startJson,
          jsonOutput = DevUtilitiesEngine.formatJson(startJson, 4).getOrDefault(startJson),
          codecOutput = DevUtilitiesEngine.encodeBase64(it.codecInput),
          generatedUuids = listOf(DevUtilitiesEngine.generateUuid()),
          generatedToken =
            DevUtilitiesEngine.generateSecureToken(
              length = it.tokenLength,
              includeUpper = it.includeUpper,
              includeLower = it.includeLower,
              includeDigits = it.includeDigits,
              includeSymbols = it.includeSymbols,
            ),
        )
      }

      prefsRepo.preferencesFlow.collect { updated ->
        _prefsState.value = updated
      }
    }
  }

  // --- Config Studio Actions ---

  fun setConfigViewMode(mode: ConfigViewMode) {
    _configState.update { it.copy(viewMode = mode) }
  }

  fun setConfigSearchQuery(query: String) {
    _configState.update { it.copy(searchQuery = query) }
  }

  fun selectPreset(preset: ConfigPreset) {
    applyNewConfigJson(preset.jsonContent, presetId = preset.id)
  }

  fun resetToAttachedOriginal() {
    val original = configRepo.loadAttachedSettingsJson()
    applyNewConfigJson(original, presetId = "attached")
    viewModelScope.launch { prefsRepo.saveConfigJson(null) }
  }

  fun onRawJsonEdited(newText: String) {
    applyNewConfigJson(newText, presetId = "custom")
  }

  fun updateSettingValue(path: String, newValue: Any) {
    val currentJson = _configState.value.jsonText
    configRepo.updateNodeValue(currentJson, path, newValue).onSuccess { updated ->
      applyNewConfigJson(updated, presetId = "custom")
    }
  }

  fun addSettingKey(key: String, value: Any) {
    if (key.isBlank()) return
    val currentJson = _configState.value.jsonText
    configRepo.addTopLevelKey(currentJson, key, value).onSuccess { updated ->
      applyNewConfigJson(updated, presetId = "custom")
    }
  }

  fun deleteSettingKey(path: String) {
    val currentJson = _configState.value.jsonText
    configRepo.deleteNodeAtPath(currentJson, path).onSuccess { updated ->
      applyNewConfigJson(updated, presetId = "custom")
    }
  }

  fun formatActiveConfig() {
    DevUtilitiesEngine.formatJson(_configState.value.jsonText, 4).onSuccess { formatted ->
      applyNewConfigJson(formatted, presetId = _configState.value.activePresetId)
    }
  }

  fun minifyActiveConfig() {
    DevUtilitiesEngine.minifyJson(_configState.value.jsonText).onSuccess { minified ->
      applyNewConfigJson(minified, presetId = _configState.value.activePresetId)
    }
  }

  fun sortActiveConfigKeys() {
    DevUtilitiesEngine.sortJsonKeys(_configState.value.jsonText, 4).onSuccess { sorted ->
      applyNewConfigJson(sorted, presetId = _configState.value.activePresetId)
    }
  }

  private fun applyNewConfigJson(jsonText: String, presetId: String) {
    val parsed = configRepo.parseConfigNodes(jsonText)
    _configState.update {
      it.copy(
        jsonText = jsonText,
        nodes = if (parsed.isSuccess) parsed.getOrDefault(emptyList()) else it.nodes,
        isValidJson = parsed.isSuccess,
        jsonError = parsed.exceptionOrNull()?.localizedMessage,
        activePresetId = presetId,
      )
    }
    if (parsed.isSuccess) {
      viewModelScope.launch { prefsRepo.saveConfigJson(jsonText) }
    }
  }

  // --- Focus Timer Actions ---

  fun updateFocusTaskName(taskName: String) {
    hasUserModifiedFocus = true
    val sanitized = taskName.take(120)
    _focusState.update {
      it.copy(taskName = sanitized, isSyncedToCloud = false, hasResumedFromCloud = false)
    }
    taskSyncDebounceJob?.cancel()
    if (cloudRepo != null && currentUserId.isNotBlank()) {
      taskSyncDebounceJob =
        viewModelScope.launch {
          delay(450L)
          persistActiveTimerState()
        }
    }
  }

  fun selectFocusPreset(preset: FocusPreset) {
    hasUserModifiedFocus = true
    timerJob?.cancel()
    val focusMin =
      if (preset == FocusPreset.CUSTOM) _prefsState.value.customFocusMinutes else preset.focusMin
    val totalSec = focusMin * 60
    _focusState.update {
      it.copy(
        phase = FocusPhase.FOCUS,
        preset = preset,
        totalSeconds = totalSec,
        remainingSeconds = totalSec,
        isRunning = false,
        hasResumedFromCloud = false,
      )
    }
    persistActiveTimerState()
  }

  fun toggleFocusTimer() {
    hasUserModifiedFocus = true
    val current = _focusState.value
    if (current.isRunning) {
      timerJob?.cancel()
      _focusState.update { it.copy(isRunning = false, hasResumedFromCloud = false) }
      persistActiveTimerState()
    } else {
      _focusState.update { it.copy(isRunning = true, hasResumedFromCloud = false) }
      persistActiveTimerState()
      timerJob?.cancel()
      timerJob =
        viewModelScope.launch {
          var elapsedTicks = 0
          while (_focusState.value.isRunning) {
            delay(1000L)
            val state = _focusState.value
            if (!state.isRunning) break
            if (state.remainingSeconds > 1) {
              _focusState.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
              elapsedTicks += 1
              if (elapsedTicks % 5 == 0) {
                persistActiveTimerState()
              }
            } else {
              completeCurrentPhase()
            }
          }
        }
    }
  }

  fun resetFocusTimer() {
    hasUserModifiedFocus = true
    timerJob?.cancel()
    val state = _focusState.value
    val minutes = phaseMinutes(state.preset, state.phase)
    val totalSec = minutes * 60
    _focusState.update {
      it.copy(
        totalSeconds = totalSec,
        remainingSeconds = totalSec,
        isRunning = false,
        hasResumedFromCloud = false,
      )
    }
    persistActiveTimerState()
  }

  fun skipFocusPhase() {
    hasUserModifiedFocus = true
    timerJob?.cancel()
    val state = _focusState.value
    val nextPhase = if (state.phase == FocusPhase.FOCUS) FocusPhase.BREAK else FocusPhase.FOCUS
    val totalSec = phaseMinutes(state.preset, nextPhase) * 60
    _focusState.update {
      it.copy(
        phase = nextPhase,
        totalSeconds = totalSec,
        remainingSeconds = totalSec,
        isRunning = false,
        hasResumedFromCloud = false,
      )
    }
    persistActiveTimerState()
  }

  fun persistActiveTimerState() {
    val repo = cloudRepo ?: return
    if (currentUserId.isBlank()) return
    val state = _focusState.value
    viewModelScope.launch {
      _focusState.update { it.copy(isSyncingToCloud = true) }
      val result =
        repo.saveActiveTimerState(
          userId = currentUserId,
          taskName = state.taskName,
          presetName = state.preset.name,
          phaseName = state.phase.name,
          totalSeconds = state.totalSeconds,
          remainingSeconds = state.remainingSeconds,
        )
      _focusState.update {
        it.copy(
          isSyncingToCloud = false,
          isSyncedToCloud = result.isSuccess || it.isSyncedToCloud,
        )
      }
    }
  }

  private fun completeCurrentPhase() {
    timerJob?.cancel()
    val state = _focusState.value
    if (state.phase == FocusPhase.FOCUS) {
      val durationMin = phaseMinutes(state.preset, FocusPhase.FOCUS)
      viewModelScope.launch {
        prefsRepo.incrementCompletedSessions()
        if (cloudRepo != null && currentUserId.isNotBlank()) {
          cloudRepo.logCompletedFocusSession(
            userId = currentUserId,
            presetName = state.preset.name,
            durationMinutes = durationMin,
          )
        }
      }
      val currentPrefs = _prefsState.value
      if (currentPrefs.soundAlertEnabled) {
        soundNotifier.playWorkCompletedAlert(currentPrefs.soundAlertStyle)
      }
      _phaseCompletedEvent.tryEmit(FocusPhase.FOCUS)
    }
    val nextPhase = if (state.phase == FocusPhase.FOCUS) FocusPhase.BREAK else FocusPhase.FOCUS
    val totalSec = phaseMinutes(state.preset, nextPhase) * 60
    _focusState.update {
      it.copy(
        phase = nextPhase,
        totalSeconds = totalSec,
        remainingSeconds = totalSec,
        isRunning = false,
        hasResumedFromCloud = false,
      )
    }
    persistActiveTimerState()
  }

  // --- Cloud Config Actions ---

  fun saveActiveConfigToCloud(
    title: String,
    existingConfigId: String? = null,
    onResult: (Result<Unit>) -> Unit = {},
  ) {
    val repo = cloudRepo ?: return
    if (currentUserId.isBlank()) return
    val current = _configState.value
    if (!current.isValidJson) {
      onResult(Result.failure(IllegalArgumentException("Cannot save invalid JSON")))
      return
    }
    viewModelScope.launch {
      val result =
        repo.saveConfig(
          userId = currentUserId,
          title = title,
          jsonContent = current.jsonText,
          keysCount = current.nodes.size,
          existingConfigId = existingConfigId,
        )
      onResult(result)
    }
  }

  fun loadCloudConfig(config: SavedConfig) {
    applyNewConfigJson(config.jsonContent, presetId = "cloud_${config.id}")
  }

  fun deleteCloudConfig(configId: String, onResult: (Result<Unit>) -> Unit = {}) {
    val repo = cloudRepo ?: return
    if (currentUserId.isBlank()) return
    viewModelScope.launch {
      val result = repo.deleteConfig(userId = currentUserId, configId = configId)
      onResult(result)
    }
  }

  private fun phaseMinutes(preset: FocusPreset, phase: FocusPhase): Int {
    val prefs = _prefsState.value
    return when (phase) {
      FocusPhase.FOCUS -> if (preset == FocusPreset.CUSTOM) prefs.customFocusMinutes else preset.focusMin
      FocusPhase.BREAK -> if (preset == FocusPreset.CUSTOM) prefs.customBreakMinutes else preset.breakMin
    }
  }

  // --- Dev Tools Actions ---

  fun selectToolTab(tab: DevToolTab) {
    _toolsState.update { it.copy(activeTab = tab) }
  }

  fun updateJsonLabInput(input: String) {
    _toolsState.update { it.copy(jsonInput = input) }
    formatJsonLab(_toolsState.value.jsonIndent)
  }

  fun importActiveConfigIntoJsonLab() {
    updateJsonLabInput(_configState.value.jsonText)
  }

  fun formatJsonLab(indent: Int) {
    val input = _toolsState.value.jsonInput
    DevUtilitiesEngine.formatJson(input, indent)
      .onSuccess { formatted ->
        _toolsState.update {
          it.copy(jsonIndent = indent, jsonOutput = formatted, jsonLabError = null)
        }
      }
      .onFailure { err ->
        _toolsState.update {
          it.copy(jsonIndent = indent, jsonLabError = err.localizedMessage)
        }
      }
  }

  fun minifyJsonLab() {
    DevUtilitiesEngine.minifyJson(_toolsState.value.jsonInput)
      .onSuccess { minified ->
        _toolsState.update { it.copy(jsonOutput = minified, jsonLabError = null) }
      }
      .onFailure { err ->
        _toolsState.update { it.copy(jsonLabError = err.localizedMessage) }
      }
  }

  fun sortJsonLab() {
    val indent = _toolsState.value.jsonIndent
    DevUtilitiesEngine.sortJsonKeys(_toolsState.value.jsonInput, indent)
      .onSuccess { sorted ->
        _toolsState.update { it.copy(jsonOutput = sorted, jsonLabError = null) }
      }
      .onFailure { err ->
        _toolsState.update { it.copy(jsonLabError = err.localizedMessage) }
      }
  }

  fun setCodecMode(isBase64: Boolean) {
    _toolsState.update { it.copy(isBase64Mode = isBase64, codecError = null) }
    encodeCodec()
  }

  fun updateCodecInput(input: String) {
    _toolsState.update { it.copy(codecInput = input, codecError = null) }
  }

  fun encodeCodec() {
    val state = _toolsState.value
    val encoded =
      if (state.isBase64Mode) DevUtilitiesEngine.encodeBase64(state.codecInput)
      else DevUtilitiesEngine.encodeUrl(state.codecInput)
    _toolsState.update { it.copy(codecOutput = encoded, codecError = null) }
  }

  fun decodeCodec() {
    val state = _toolsState.value
    val result =
      if (state.isBase64Mode) DevUtilitiesEngine.decodeBase64(state.codecInput)
      else DevUtilitiesEngine.decodeUrl(state.codecInput)
    result
      .onSuccess { decoded ->
        _toolsState.update { it.copy(codecOutput = decoded, codecError = null) }
      }
      .onFailure { err ->
        _toolsState.update { it.copy(codecError = err.localizedMessage) }
      }
  }

  fun swapCodec() {
    _toolsState.update {
      it.copy(codecInput = it.codecOutput, codecOutput = it.codecInput, codecError = null)
    }
  }

  fun generateUuids(count: Int = 1) {
    val list = List(count) { DevUtilitiesEngine.generateUuid() }
    _toolsState.update { it.copy(generatedUuids = list) }
  }

  fun updateTokenConfig(
    length: Int = _toolsState.value.tokenLength,
    upper: Boolean = _toolsState.value.includeUpper,
    lower: Boolean = _toolsState.value.includeLower,
    digits: Boolean = _toolsState.value.includeDigits,
    symbols: Boolean = _toolsState.value.includeSymbols,
  ) {
    val safeUpper = if (!upper && !lower && !digits && !symbols) true else upper
    val token =
      DevUtilitiesEngine.generateSecureToken(
        length = length,
        includeUpper = safeUpper,
        includeLower = lower,
        includeDigits = digits,
        includeSymbols = symbols,
      )
    _toolsState.update {
      it.copy(
        tokenLength = length.coerceIn(8, 64),
        includeUpper = safeUpper,
        includeLower = lower,
        includeDigits = digits,
        includeSymbols = symbols,
        generatedToken = token,
      )
    }
  }

  fun updateColorRgb(r: Int, g: Int, b: Int) {
    _toolsState.update {
      it.copy(
        red = r.coerceIn(0, 255),
        green = g.coerceIn(0, 255),
        blue = b.coerceIn(0, 255),
      )
    }
  }

  // --- Preferences Actions ---

  fun setArabic(isArabic: Boolean) {
    viewModelScope.launch { prefsRepo.setArabic(isArabic) }
  }

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch { prefsRepo.setThemeMode(mode) }
  }

  fun setDynamicColor(enabled: Boolean) {
    viewModelScope.launch { prefsRepo.setDynamicColor(enabled) }
  }

  fun setCustomFocusMinutes(minutes: Int) {
    viewModelScope.launch {
      prefsRepo.setCustomFocusMinutes(minutes)
      val focus = _focusState.value
      if (focus.preset == FocusPreset.CUSTOM && focus.phase == FocusPhase.FOCUS && !focus.isRunning) {
        val total = minutes.coerceIn(5, 90) * 60
        _focusState.update { it.copy(totalSeconds = total, remainingSeconds = total) }
      }
    }
  }

  fun setCustomBreakMinutes(minutes: Int) {
    viewModelScope.launch {
      prefsRepo.setCustomBreakMinutes(minutes)
      val focus = _focusState.value
      if (focus.preset == FocusPreset.CUSTOM && focus.phase == FocusPhase.BREAK && !focus.isRunning) {
        val total = minutes.coerceIn(1, 30) * 60
        _focusState.update { it.copy(totalSeconds = total, remainingSeconds = total) }
      }
    }
  }

  fun setSoundAlertEnabled(enabled: Boolean) {
    viewModelScope.launch { prefsRepo.setSoundAlertEnabled(enabled) }
  }

  fun setSoundAlertStyle(style: SoundAlertStyle) {
    viewModelScope.launch { prefsRepo.setSoundAlertStyle(style) }
  }

  fun testSoundAlert() {
    soundNotifier.playWorkCompletedAlert(_prefsState.value.soundAlertStyle)
  }

  fun openTutorial() {
    _showTutorial.value = true
  }

  fun dismissTutorial() {
    _showTutorial.value = false
    viewModelScope.launch { prefsRepo.setHasSeenTutorial(true) }
  }

  class Factory(
    private val application: Application,
    private val currentUserId: String,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return DevCraftViewModel(
        prefsRepo = UserPreferencesRepository(application),
        configRepo = ConfigRepository(application),
        soundNotifier = AndroidPomodoroSoundNotifier(application),
        currentUserId = currentUserId,
        cloudRepo = CloudWorkspaceRepository(application),
      )
        as T
    }
  }
}
