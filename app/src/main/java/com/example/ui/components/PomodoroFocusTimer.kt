package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FocusPhase
import com.example.ui.theme.DevCraftTheme
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay

/**
 * Reusable Pomodoro Focus Timer component with circular progress ring and
 * explicit Start, Pause, Reset, and optional Skip interval controls.
 */
@Composable
fun PomodoroFocusTimer(
  remainingSeconds: Int,
  totalSeconds: Int,
  isRunning: Boolean,
  phase: FocusPhase,
  phaseLabel: String,
  startLabel: String,
  pauseLabel: String,
  resetLabel: String,
  onStart: () -> Unit,
  onPause: () -> Unit,
  onReset: () -> Unit,
  modifier: Modifier = Modifier,
  skipLabel: String? = null,
  onSkipPhase: (() -> Unit)? = null,
  taskName: String = "",
  onTaskNameChange: ((String) -> Unit)? = null,
  taskNameLabel: String = "",
  taskNamePlaceholder: String = "",
  syncStatusText: String? = null,
) {
  val isFocus = phase == FocusPhase.FOCUS
  val accentColor =
    if (isFocus) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
  val secondaryAccent =
    if (isFocus) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary

  val rawProgress =
    if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds.toFloat() else 0f
  val animatedProgress by
    animateFloatAsState(
      targetValue = rawProgress.coerceIn(0f, 1f),
      animationSpec = tween(durationMillis = 450),
      label = "pomodoro_ring_progress",
    )

  val infiniteTransition = rememberInfiniteTransition(label = "pomodoro_pulse")
  val pulseAlpha by
    infiniteTransition.animateFloat(
      initialValue = 0.14f,
      targetValue = 0.34f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "pomodoro_glow_alpha",
    )

  val mins = remainingSeconds.coerceAtLeast(0) / 60
  val secs = remainingSeconds.coerceAtLeast(0) % 60
  val formattedTime = String.format("%02d:%02d", mins, secs)
  val percentRemaining = (rawProgress.coerceIn(0f, 1f) * 100f).roundToInt()

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(28.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 3.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp, horizontal = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      if (onTaskNameChange != null) {
        OutlinedTextField(
          value = taskName,
          onValueChange = { onTaskNameChange(it.take(120)) },
          label =
            if (taskNameLabel.isNotBlank()) {
              { Text(taskNameLabel) }
            } else null,
          placeholder =
            if (taskNamePlaceholder.isNotBlank()) {
              { Text(taskNamePlaceholder) }
            } else null,
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.EditNote,
              contentDescription = taskNameLabel.ifBlank { "Task Name" },
              tint = accentColor,
            )
          },
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth().testTag("pomodoro_task_name_input"),
        )
      } else if (taskName.isNotBlank()) {
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = accentColor.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        ) {
          Text(
            text = taskName,
            style = MaterialTheme.typography.labelLarge,
            color = accentColor,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
          )
        }
      }
      val trackColor = MaterialTheme.colorScheme.surfaceVariant
      val tickColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)

      Box(modifier = Modifier.size(240.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val strokeWidth = 14.dp.toPx()
          val diameter = size.minDimension - strokeWidth * 2
          val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
          val arcSize = Size(diameter, diameter)
          val center = Offset(size.width / 2f, size.height / 2f)

          if (isRunning) {
            drawCircle(
              brush =
                Brush.radialGradient(
                  colors = listOf(accentColor.copy(alpha = pulseAlpha), Color.Transparent),
                  center = center,
                  radius = size.minDimension / 2f,
                ),
              radius = size.minDimension / 2f,
              center = center,
            )
          }

          // Subtle 24-tick outer dial
          val tickOuter = size.minDimension / 2f - 2.dp.toPx()
          val tickInner = tickOuter - 5.dp.toPx()
          for (i in 0 until 24) {
            val angleRad = (i * 15.0) * PI / 180.0
            val p1 =
              Offset(
                center.x + (tickInner * cos(angleRad)).toFloat(),
                center.y + (tickInner * sin(angleRad)).toFloat(),
              )
            val p2 =
              Offset(
                center.x + (tickOuter * cos(angleRad)).toFloat(),
                center.y + (tickOuter * sin(angleRad)).toFloat(),
              )
            drawLine(color = tickColor, start = p1, end = p2, strokeWidth = 1.5.dp.toPx())
          }

          // Background Track
          drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
          )

          // Active Gradient Progress Arc
          val sweep = 360f * animatedProgress
          drawArc(
            brush = Brush.sweepGradient(listOf(accentColor, secondaryAccent, accentColor)),
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
          )

          // Glowing tip marker
          if (sweep > 2f) {
            val tipAngleRad = (-90f + sweep) * PI / 180.0
            val radius = diameter / 2f
            val tipCenter =
              Offset(
                center.x + (radius * cos(tipAngleRad)).toFloat(),
                center.y + (radius * sin(tipAngleRad)).toFloat(),
              )
            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = tipCenter)
          }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(
              text = formattedTime,
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 46.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.testTag("timer_display_text"),
            )
          }
          Spacer(Modifier.height(4.dp))
          Text(
            text = phaseLabel,
            style = MaterialTheme.typography.labelLarge,
            color = accentColor,
          )
          Spacer(Modifier.height(2.dp))
          Text(
            text = "$percentRemaining%",
            fontFamily = JetBrainsMonoFontFamily,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Controls: Reset, Start/Pause, and optional Skip
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedButton(
          onClick = onReset,
          shape = CircleShape,
          modifier = Modifier.size(56.dp).testTag("focus_reset_button"),
          contentPadding = PaddingValues(0.dp),
        ) {
          Icon(imageVector = Icons.Default.Refresh, contentDescription = resetLabel)
        }

        if (isRunning) {
          FilledTonalButton(
            onClick = onPause,
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier.heightIn(min = 56.dp).width(156.dp).testTag("focus_play_pause"),
          ) {
            Icon(imageVector = Icons.Default.Pause, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(text = pauseLabel, style = MaterialTheme.typography.titleMedium)
          }
        } else {
          Button(
            onClick = onStart,
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            modifier = Modifier.heightIn(min = 56.dp).width(156.dp).testTag("focus_play_pause"),
          ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(text = startLabel, style = MaterialTheme.typography.titleMedium)
          }
        }

        if (onSkipPhase != null && skipLabel != null) {
          OutlinedButton(
            onClick = onSkipPhase,
            shape = CircleShape,
            modifier = Modifier.size(56.dp).testTag("focus_skip_button"),
            contentPadding = PaddingValues(0.dp),
          ) {
            Icon(imageVector = Icons.Default.SkipNext, contentDescription = skipLabel)
          }
        }
      }

      if (!syncStatusText.isNullOrBlank()) {
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
          modifier = Modifier.testTag("pomodoro_cloud_sync_badge"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Default.CloudDone,
              contentDescription = syncStatusText,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = syncStatusText,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

/**
 * Self-contained stateful wrapper for embedding a standalone Pomodoro timer
 * anywhere without needing an external ViewModel.
 */
@Composable
fun StatefulPomodoroFocusTimer(
  workMinutes: Int = 25,
  breakMinutes: Int = 5,
  workLabel: String = "جلسة عمل مركّز",
  breakLabel: String = "استراحة تنشيطية",
  startLabel: String = "ابدأ",
  pauseLabel: String = "إيقاف مؤقت",
  resetLabel: String = "إعادة",
  skipLabel: String = "تخطي",
  modifier: Modifier = Modifier,
) {
  var phase by remember { mutableStateOf(FocusPhase.FOCUS) }
  var isRunning by remember { mutableStateOf(false) }
  val totalSeconds =
    remember(phase, workMinutes, breakMinutes) {
      (if (phase == FocusPhase.FOCUS) workMinutes else breakMinutes).coerceAtLeast(1) * 60
    }
  var remainingSeconds by remember(phase, totalSeconds) { mutableIntStateOf(totalSeconds) }

  LaunchedEffect(isRunning, phase, totalSeconds) {
    if (!isRunning) return@LaunchedEffect
    while (isRunning && remainingSeconds > 0) {
      delay(1000L)
      remainingSeconds -= 1
    }
    if (remainingSeconds <= 0) {
      isRunning = false
      phase = if (phase == FocusPhase.FOCUS) FocusPhase.BREAK else FocusPhase.FOCUS
    }
  }

  PomodoroFocusTimer(
    remainingSeconds = remainingSeconds,
    totalSeconds = totalSeconds,
    isRunning = isRunning,
    phase = phase,
    phaseLabel = if (phase == FocusPhase.FOCUS) workLabel else breakLabel,
    startLabel = startLabel,
    pauseLabel = pauseLabel,
    resetLabel = resetLabel,
    skipLabel = skipLabel,
    onStart = { isRunning = true },
    onPause = { isRunning = false },
    onReset = {
      isRunning = false
      remainingSeconds = totalSeconds
    },
    onSkipPhase = {
      isRunning = false
      phase = if (phase == FocusPhase.FOCUS) FocusPhase.BREAK else FocusPhase.FOCUS
    },
    modifier = modifier,
  )
}

@Preview(showBackground = true)
@Composable
private fun PomodoroFocusTimerDarkPreview() {
  DevCraftTheme(darkTheme = true) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
      PomodoroFocusTimer(
        remainingSeconds = 18 * 60 + 42,
        totalSeconds = 25 * 60,
        isRunning = true,
        phase = FocusPhase.FOCUS,
        phaseLabel = "جلسة عمل مركّز",
        startLabel = "ابدأ",
        pauseLabel = "إيقاف مؤقت",
        resetLabel = "إعادة",
        skipLabel = "تخطي",
        onStart = {},
        onPause = {},
        onReset = {},
        onSkipPhase = {},
        modifier = Modifier.padding(16.dp),
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun PomodoroFocusTimerLightPreview() {
  DevCraftTheme(darkTheme = false) {
    PomodoroFocusTimer(
      remainingSeconds = 25 * 60,
      totalSeconds = 25 * 60,
      isRunning = false,
      phase = FocusPhase.FOCUS,
      phaseLabel = "Focus Session",
      startLabel = "Start",
      pauseLabel = "Pause",
      resetLabel = "Reset",
      skipLabel = "Skip",
      onStart = {},
      onPause = {},
      onReset = {},
      onSkipPhase = {},
      modifier = Modifier.padding(16.dp),
    )
  }
}
