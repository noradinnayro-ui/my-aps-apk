package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.RingtoneManager
import android.media.ToneGenerator
import com.example.model.SoundAlertStyle
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface PomodoroSoundNotifier {
  fun playWorkCompletedAlert(style: SoundAlertStyle)
}

class AndroidPomodoroSoundNotifier(private val context: Context) : PomodoroSoundNotifier {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  override fun playWorkCompletedAlert(style: SoundAlertStyle) {
    scope.launch {
      when (style) {
        SoundAlertStyle.HARMONIC_CHIME -> {
          val played = playHarmonicChime()
          if (!played) playFallbackTone()
        }
        SoundAlertStyle.SYSTEM_NOTIFICATION -> {
          val played = playSystemNotificationSound()
          if (!played) {
            if (!playHarmonicChime()) playFallbackTone()
          }
        }
      }
    }
  }

  private fun playSystemNotificationSound(): Boolean =
    runCatching {
        val uri =
          RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: return@runCatching false
        val ringtone = RingtoneManager.getRingtone(context, uri) ?: return@runCatching false
        ringtone.audioAttributes =
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        ringtone.play()
        true
      }
      .getOrDefault(false)

  private suspend fun playHarmonicChime(): Boolean {
    val sampleRate = 44100
    val pcmData = generateChimeSamples(sampleRate)
    val bufferBytes = pcmData.size * 2

    val track =
      runCatching {
          AudioTrack.Builder()
            .setAudioAttributes(
              AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            )
            .setAudioFormat(
              AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            )
            .setBufferSizeInBytes(bufferBytes)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        }
        .getOrNull() ?: return false

    return try {
      val written = track.write(pcmData, 0, pcmData.size)
      if (written <= 0) return false
      track.play()
      val durationMs = (pcmData.size * 1000L) / sampleRate
      delay(durationMs + 100L)
      true
    } catch (_: Throwable) {
      false
    } finally {
      runCatching {
        track.stop()
        track.release()
      }
    }
  }

  private fun playFallbackTone() {
    runCatching {
      val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
      toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 450)
      Thread.sleep(500)
      toneGen.release()
    }
  }

  companion object {
    /**
     * Synthesizes a smooth 3-note ascending C-major chime (C5 -> E5 -> G5)
     * with soft attack and exponential decay to prevent clicks.
     */
    fun generateChimeSamples(sampleRate: Int = 44100): ShortArray {
      val noteFrequencies = doubleArrayOf(523.25, 659.25, 783.99)
      val noteStepSeconds = 0.16
      val totalDurationSeconds = 0.95
      val totalSamples = (sampleRate * totalDurationSeconds).toInt()
      val samples = ShortArray(totalSamples)

      for (i in 0 until totalSamples) {
        val t = i.toDouble() / sampleRate
        var mix = 0.0
        for (n in noteFrequencies.indices) {
          val noteStart = n * noteStepSeconds
          if (t >= noteStart) {
            val dt = t - noteStart
            val attack = (dt / 0.012).coerceAtMost(1.0)
            val decay = exp(-5.2 * dt)
            val freq = noteFrequencies[n]
            // Fundamental + soft octave overtone for bell-like clarity
            val wave =
              0.78 * sin(2.0 * PI * freq * dt) + 0.22 * sin(2.0 * PI * (freq * 2.0) * dt)
            mix += wave * attack * decay
          }
        }
        val normalized = (mix * 0.45).coerceIn(-1.0, 1.0)
        samples[i] = (normalized * Short.MAX_VALUE).toInt().toShort()
      }
      return samples
    }
  }
}
