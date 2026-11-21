package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.UserPreferences
import com.example.model.AppStrings
import com.example.model.SoundAlertStyle
import com.example.model.ThemeMode
import com.example.ui.components.HeroHeaderCard
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlin.math.roundToInt

@Composable
fun PreferencesScreen(
  prefs: UserPreferences,
  strings: AppStrings,
  userDisplayName: String,
  userEmail: String,
  onSetArabic: (Boolean) -> Unit,
  onSetThemeMode: (ThemeMode) -> Unit,
  onSetDynamicColor: (Boolean) -> Unit,
  onSetFocusMinutes: (Int) -> Unit,
  onSetBreakMinutes: (Int) -> Unit,
  onSetSoundAlertEnabled: (Boolean) -> Unit,
  onSetSoundAlertStyle: (SoundAlertStyle) -> Unit,
  onTestSoundAlert: () -> Unit,
  onOpenTutorial: () -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      HeroHeaderCard(
        title = strings.prefsHeroTitle,
        subtitle = strings.languageLabel,
        icon = Icons.Default.Palette,
      )
    }

    // Account & Cloud Sync Card
    item {
      PrefsCard {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = strings.cloudSyncTitle,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp),
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = strings.signedInAs,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = userDisplayName,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
            if (userEmail.isNotBlank()) {
              Text(
                text = userEmail,
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
        OutlinedButton(
          onClick = onSignOut,
          modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("sign_out_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = strings.signOutAction,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(8.dp))
          Text(strings.signOutAction)
        }
      }
    }

    // Interactive App Guide Card
    item {
      PrefsCard {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = Icons.Default.School,
            contentDescription = strings.tutorialAction,
            tint = MaterialTheme.colorScheme.secondary,
          )
          Text(strings.tutorialAction, style = MaterialTheme.typography.titleSmall)
        }
        Text(
          text = strings.tutorialOpenFromPrefsDesc,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
          onClick = onOpenTutorial,
          modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("open_tutorial_button"),
        ) {
          Icon(
            imageVector = Icons.Default.School,
            contentDescription = strings.tutorialAction,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(8.dp))
          Text(strings.tutorialAction)
        }
      }
    }

    // Language & RTL/LTR Toggle
    item {
      PrefsCard {
        Text(strings.languageLabel, style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          SegmentedButton(
            selected = prefs.isArabic,
            onClick = { onSetArabic(true) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            modifier = Modifier.testTag("lang_arabic_button"),
          ) {
            Text(strings.languageAr)
          }
          SegmentedButton(
            selected = !prefs.isArabic,
            onClick = { onSetArabic(false) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            modifier = Modifier.testTag("lang_english_button"),
          ) {
            Text(strings.languageEn)
          }
        }
      }
    }

    // Theme Mode + Dynamic Color
    item {
      PrefsCard {
        Text(strings.themeLabel, style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          val modes =
            listOf(
              ThemeMode.SYSTEM to strings.themeSystem,
              ThemeMode.DARK to strings.themeDark,
              ThemeMode.LIGHT to strings.themeLight,
            )
          modes.forEachIndexed { idx, (mode, label) ->
            SegmentedButton(
              selected = prefs.themeMode == mode,
              onClick = { onSetThemeMode(mode) },
              shape = SegmentedButtonDefaults.itemShape(index = idx, count = modes.size),
              modifier = Modifier.testTag("theme_mode_${mode.name}"),
            ) {
              Text(label)
            }
          }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
              Text(strings.dynamicColorLabel, style = MaterialTheme.typography.titleSmall)
              Text(
                strings.dynamicColorDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = prefs.dynamicColor,
              onCheckedChange = onSetDynamicColor,
              modifier = Modifier.testTag("dynamic_color_switch"),
            )
          }
        }
      }
    }

    // Custom Focus & Break Durations
    item {
      PrefsCard {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(strings.focusDurationLabel, style = MaterialTheme.typography.titleSmall)
          Text(
            text = "${prefs.customFocusMinutes} ${strings.minutesUnit}",
            fontFamily = JetBrainsMonoFontFamily,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Slider(
          value = prefs.customFocusMinutes.toFloat(),
          onValueChange = { onSetFocusMinutes(it.roundToInt()) },
          valueRange = 5f..90f,
          modifier = Modifier.testTag("custom_focus_slider"),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(strings.breakDurationLabel, style = MaterialTheme.typography.titleSmall)
          Text(
            text = "${prefs.customBreakMinutes} ${strings.minutesUnit}",
            fontFamily = JetBrainsMonoFontFamily,
            color = MaterialTheme.colorScheme.tertiary,
          )
        }
        Slider(
          value = prefs.customBreakMinutes.toFloat(),
          onValueChange = { onSetBreakMinutes(it.roundToInt()) },
          valueRange = 1f..30f,
          modifier = Modifier.testTag("custom_break_slider"),
        )
      }
    }

    // Sound Notification Preferences
    item {
      PrefsCard {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(strings.soundAlertLabel, style = MaterialTheme.typography.titleSmall)
            Text(
              strings.soundAlertDesc,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = prefs.soundAlertEnabled,
            onCheckedChange = onSetSoundAlertEnabled,
            modifier = Modifier.testTag("sound_alert_switch"),
          )
        }

        Text(strings.soundStyleLabel, style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          val styles =
            listOf(
              SoundAlertStyle.HARMONIC_CHIME to strings.soundStyleChime,
              SoundAlertStyle.SYSTEM_NOTIFICATION to strings.soundStyleSystem,
            )
          styles.forEachIndexed { idx, (style, label) ->
            SegmentedButton(
              selected = prefs.soundAlertStyle == style,
              onClick = { onSetSoundAlertStyle(style) },
              shape = SegmentedButtonDefaults.itemShape(index = idx, count = styles.size),
              modifier = Modifier.testTag("sound_style_${style.name}"),
            ) {
              Text(label)
            }
          }
        }

        OutlinedButton(
          onClick = onTestSoundAlert,
          modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("prefs_test_sound_button"),
        ) {
          Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(8.dp))
          Text(strings.testSoundAction)
        }
      }
    }

    // Bundled Config Asset Info
    item {
      PrefsCard {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            Icons.Default.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
          )
          Text(strings.attachedAssetTitle, style = MaterialTheme.typography.titleSmall)
        }
        Text(
          text = strings.attachedAssetDesc,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    item { Spacer(modifier = Modifier.height(12.dp)) }
  }
}

@Composable
private fun PrefsCard(content: @Composable () -> Unit) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      content()
    }
  }
}
