package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.screens.TutorialScreen
import com.example.ui.theme.DevCraftTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TutorialScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `tutorial screen navigates across slides and triggers interactive demo`() {
    var dismissed = 0

    composeTestRule.setContent {
      DevCraftTheme(darkTheme = true) {
        TutorialScreen(
          isArabic = true,
          onDismiss = { dismissed++ },
        )
      }
    }

    composeTestRule.onNodeWithText("مرحباً بك في مُطوّر برو").assertIsDisplayed()
    composeTestRule.onNodeWithTag("tutorial_next_button").performClick()

    composeTestRule.onNodeWithText("1. استوديو إعدادات المحرر").assertIsDisplayed()
    composeTestRule.onNodeWithTag("tutorial_demo_switch").performClick()

    composeTestRule.onNodeWithTag("tutorial_skip_button").performClick()
    assertEquals(1, dismissed)
  }
}
