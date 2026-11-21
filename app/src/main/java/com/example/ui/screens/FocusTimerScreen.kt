package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.FocusSessionLog
import com.example.data.UserProfile
import com.example.model.AppStrings
import com.example.model.CloudUiState
import com.example.model.FocusPhase
import com.example.model.FocusPreset
import com.example.ui.components.HeroHeaderCard
import com.example.ui.components.PomodoroFocusTimer
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.viewmodel.FocusUiState

@Composable
fun FocusTimerScreen(
  state: FocusUiState,
  completedSessionsToday: Int,
  soundAlertEnabled: Boolean,
  cloudProfileState: CloudUiState<UserProfile?>,
  cloudSessionsState: CloudUiState<List<FocusSessionLog>>,
  strings: AppStrings,
  onSelectPreset: (FocusPreset) -> Unit,
  onToggleTimer: () -> Unit,
  onResetTimer: () -> Unit,
  onSkipPhase: () -> Unit,
  onTaskNameChange: (String) -> Unit,
  onToggleSoundAlert: (Boolean) -> Unit,
  onTestSoundAlert: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isFocus = state.phase == FocusPhase.FOCUS
  val accentColor =
    if (isFocus) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      HeroHeaderCard(
        title = strings.focusHeroTitle,
        subtitle = strings.focusTip,
        icon = if (isFocus) Icons.Default.Timer else Icons.Default.Coffee,
        badgeText = if (isFocus) strings.focusPhaseLabel else strings.breakPhaseLabel,
        badgeColor = accentColor,
      )
    }

    // Preset selector chips
    item {
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        val presets =
          listOf(
            FocusPreset.CLASSIC to strings.presetClassic,
            FocusPreset.DEEP_WORK to strings.presetDeep,
            FocusPreset.SPRINT to strings.presetSprint,
            FocusPreset.CUSTOM to strings.presetCustom,
          )
        presets.forEach { (preset, label) ->
          FilterChip(
            selected = state.preset == preset,
            onClick = { onSelectPreset(preset) },
            label = { Text(label) },
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("focus_preset_${preset.name}"),
          )
        }
      }
    }

    if (state.hasResumedFromCloud) {
      item {
        Surface(
          modifier = Modifier.fillMaxWidth().testTag("focus_resumed_banner"),
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Default.CloudDone,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = strings.timerResumedCloudBanner,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
        }
      }
    }

    // Reusable Pomodoro Focus Timer Component
    item {
      val syncStatusText =
        when {
          state.isSyncingToCloud -> strings.timerSyncingCloudBadge
          state.isSyncedToCloud -> strings.timerSyncedCloudBadge
          else -> null
        }
      PomodoroFocusTimer(
        remainingSeconds = state.remainingSeconds,
        totalSeconds = state.totalSeconds,
        isRunning = state.isRunning,
        phase = state.phase,
        phaseLabel = if (isFocus) strings.focusPhaseLabel else strings.breakPhaseLabel,
        startLabel = strings.startAction,
        pauseLabel = strings.pauseAction,
        resetLabel = strings.resetAction,
        skipLabel = strings.skipAction,
        onStart = { if (!state.isRunning) onToggleTimer() },
        onPause = { if (state.isRunning) onToggleTimer() },
        onReset = onResetTimer,
        onSkipPhase = onSkipPhase,
        taskName = state.taskName,
        onTaskNameChange = onTaskNameChange,
        taskNameLabel = strings.focusTaskNameLabel,
        taskNamePlaceholder = strings.focusTaskNamePlaceholder,
        syncStatusText = syncStatusText,
      )
    }

    // Quick Sound Alert Controls Row
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        FilterChip(
          selected = soundAlertEnabled,
          onClick = { onToggleSoundAlert(!soundAlertEnabled) },
          leadingIcon = {
            Icon(
              imageVector =
                if (soundAlertEnabled) Icons.AutoMirrored.Filled.VolumeUp
                else Icons.AutoMirrored.Filled.VolumeOff,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
          },
          label = {
            Text(if (soundAlertEnabled) strings.soundEnabledChip else strings.soundMutedChip)
          },
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .testTag("focus_sound_toggle_chip"),
        )

        OutlinedButton(
          onClick = onTestSoundAlert,
          modifier = Modifier.heightIn(min = 48.dp).testTag("focus_test_sound_button"),
        ) {
          Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(6.dp))
          Text(strings.testSoundAction)
        }
      }
    }

    // Completed Sessions Today + Cloud Totals Stats Card
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(30.dp),
              )
              Column {
                Text(
                  text = strings.completedSessions,
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "${state.preset.focusMin}m / ${state.preset.breakMin}m",
                  fontFamily = JetBrainsMonoFontFamily,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            Text(
              text = completedSessionsToday.toString(),
              fontFamily = JetBrainsMonoFontFamily,
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.testTag("completed_sessions_count"),
            )
          }

          when (cloudProfileState) {
            is CloudUiState.Loading -> {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                Text(
                  text = strings.cloudLoading,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            is CloudUiState.Error -> {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  Icons.Default.ErrorOutline,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = cloudProfileState.message,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.error,
                )
              }
            }
            is CloudUiState.Success -> {
              val profile = cloudProfileState.data
              if (profile != null) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Icon(
                      Icons.Default.CloudDone,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(18.dp),
                    )
                    Text(
                      text = strings.cloudTotalStatsLabel,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Text(
                    text = "${profile.completedFocusSessions} / ${profile.totalFocusMinutes}m",
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("cloud_focus_totals"),
                  )
                }
              }
            }
          }
        }
      }
    }

    // Recent Cloud Focus Session Logs
    if (cloudSessionsState is CloudUiState.Success && cloudSessionsState.data.isNotEmpty()) {
      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
              Text(
                text = strings.cloudRecentSessionsTitle,
                style = MaterialTheme.typography.titleSmall,
              )
            }

            cloudSessionsState.data.take(5).forEach { log ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = log.presetName,
                  fontFamily = JetBrainsMonoFontFamily,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "${log.durationMinutes} ${strings.minutesUnit}",
                  fontFamily = JetBrainsMonoFontFamily,
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.tertiary,
                )
              }
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(12.dp)) }
  }
}
