package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.example.model.FocusPhase
import com.example.ui.components.PomodoroFocusTimer
import com.example.ui.components.StatefulPomodoroFocusTimer
import com.example.ui.theme.DevCraftTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PomodoroFocusTimerTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `pomodoro timer invokes start pause and reset callbacks`() {
    var started = 0
    var paused = 0
    var reset = 0

    composeTestRule.setContent {
      DevCraftTheme(darkTheme = true) {
        PomodoroFocusTimer(
          remainingSeconds = 25 * 60,
          totalSeconds = 25 * 60,
          isRunning = false,
          phase = FocusPhase.FOCUS,
          phaseLabel = "جلسة عمل مركّز",
          startLabel = "ابدأ",
          pauseLabel = "إيقاف مؤقت",
          resetLabel = "إعادة",
          onStart = { started++ },
          onPause = { paused++ },
          onReset = { reset++ },
        )
      }
    }

    composeTestRule.onNodeWithTag("timer_display_text").assertTextEquals("25:00")
    composeTestRule.onNodeWithText("ابدأ").assertIsDisplayed()

    composeTestRule.onNodeWithTag("focus_play_pause").performClick()
    composeTestRule.onNodeWithTag("focus_reset_button").performClick()

    assertEquals(1, started)
    assertEquals(0, paused)
    assertEquals(1, reset)
  }

  @Test
  fun `pomodoro timer displays and edits task name and shows cloud sync badge`() {
    var latestTaskName = "Refactor Firestore Sync"

    composeTestRule.setContent {
      DevCraftTheme(darkTheme = true) {
        var taskName by remember { mutableStateOf(latestTaskName) }
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          PomodoroFocusTimer(
            remainingSeconds = 12 * 60 + 34,
            totalSeconds = 25 * 60,
            isRunning = false,
            phase = FocusPhase.FOCUS,
            phaseLabel = "Focus Session",
            startLabel = "Start",
            pauseLabel = "Pause",
            resetLabel = "Reset",
            onStart = {},
            onPause = {},
            onReset = {},
            taskName = taskName,
            onTaskNameChange = { updated ->
              taskName = updated
              latestTaskName = updated
            },
            taskNameLabel = "Current Task",
            taskNamePlaceholder = "e.g. Build Auth Flow",
            syncStatusText = "Saved to cloud",
          )
        }
      }
    }

    composeTestRule.onNodeWithTag("timer_display_text").assertTextEquals("12:34")
    composeTestRule.onNodeWithTag("pomodoro_task_name_input")
      .assertIsDisplayed()
      .assertTextContains("Refactor Firestore Sync")
    composeTestRule.onNodeWithTag("pomodoro_cloud_sync_badge")
      .performScrollTo()
      .assertIsDisplayed()
    composeTestRule.onNodeWithText("Saved to cloud").assertIsDisplayed()

    composeTestRule.onNodeWithTag("pomodoro_task_name_input")
      .performScrollTo()
      .performTextReplacement("Write Unit Tests")

    assertEquals("Write Unit Tests", latestTaskName)
  }

  @Test
  fun `stateful pomodoro timer toggles between start pause and reset`() {
    composeTestRule.setContent {
      DevCraftTheme(darkTheme = false) {
        StatefulPomodoroFocusTimer(
          workMinutes = 25,
          breakMinutes = 5,
          startLabel = "Start",
          pauseLabel = "Pause",
          resetLabel = "Reset",
        )
      }
    }

    composeTestRule.onNodeWithTag("timer_display_text").assertTextEquals("25:00")
    composeTestRule.onNodeWithText("Start").assertIsDisplayed()

    composeTestRule.onNodeWithTag("focus_play_pause").performClick()
    composeTestRule.onNodeWithText("Pause").assertIsDisplayed()

    composeTestRule.onNodeWithTag("focus_reset_button").performClick()
    composeTestRule.onNodeWithText("Start").assertIsDisplayed()
    composeTestRule.onNodeWithTag("timer_display_text").assertTextEquals("25:00")
  }
}
