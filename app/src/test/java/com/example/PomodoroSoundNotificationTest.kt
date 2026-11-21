package com.example

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AndroidPomodoroSoundNotifier
import com.example.audio.PomodoroSoundNotifier
import com.example.data.ConfigRepository
import com.example.data.UserPreferencesRepository
import com.example.model.FocusPhase
import com.example.model.FocusPreset
import com.example.model.SoundAlertStyle
import com.example.viewmodel.DevCraftViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PomodoroSoundNotificationTest {

  private val testDispatcher = StandardTestDispatcher()

  private class FakeSoundNotifier : PomodoroSoundNotifier {
    val playedStyles = mutableListOf<SoundAlertStyle>()

    override fun playWorkCompletedAlert(style: SoundAlertStyle) {
      playedStyles.add(style)
    }
  }

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `synthesized harmonic chime generates audible non-zero pcm waveform`() {
    val samples = AndroidPomodoroSoundNotifier.generateChimeSamples(44100)
    assertTrue(samples.isNotEmpty())
    val maxAmplitude = samples.maxOf { kotlin.math.abs(it.toInt()) }
    assertTrue("Expected audible chime amplitude, got $maxAmplitude", maxAmplitude > 3000)
  }

  @Test
  fun `sound alert triggers automatically when work interval completes`() = runTest(testDispatcher) {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefsRepo = UserPreferencesRepository(context)
    prefsRepo.setSoundAlertEnabled(true)
    prefsRepo.setSoundAlertStyle(SoundAlertStyle.HARMONIC_CHIME)

    val fakeNotifier = FakeSoundNotifier()
    val vm = DevCraftViewModel(prefsRepo, ConfigRepository(context), fakeNotifier)
    try {
      runCurrent()

      vm.selectFocusPreset(FocusPreset.SPRINT) // 15 minutes = 900 seconds
      vm.toggleFocusTimer()
      runCurrent()

      // Advance full 15-minute work interval
      advanceTimeBy(900_000L)
      runCurrent()

      assertEquals(1, fakeNotifier.playedStyles.size)
      assertEquals(SoundAlertStyle.HARMONIC_CHIME, fakeNotifier.playedStyles.first())
      assertEquals(FocusPhase.BREAK, vm.focusState.value.phase)
    } finally {
      vm.viewModelScope.cancel()
    }
  }

  @Test
  fun `sound alert does not trigger when muted or skipped`() = runTest(testDispatcher) {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefsRepo = UserPreferencesRepository(context)
    prefsRepo.setSoundAlertEnabled(false)

    val fakeNotifier = FakeSoundNotifier()
    val vm = DevCraftViewModel(prefsRepo, ConfigRepository(context), fakeNotifier)
    try {
      runCurrent()

      vm.selectFocusPreset(FocusPreset.SPRINT)
      vm.toggleFocusTimer()
      runCurrent()
      advanceTimeBy(900_000L)
      runCurrent()

      assertEquals(0, fakeNotifier.playedStyles.size)
      assertEquals(FocusPhase.BREAK, vm.focusState.value.phase)

      // Skipping break back to focus also should not trigger work completion alert
      vm.skipFocusPhase()
      runCurrent()
      assertEquals(0, fakeNotifier.playedStyles.size)
    } finally {
      vm.viewModelScope.cancel()
    }
  }
}
