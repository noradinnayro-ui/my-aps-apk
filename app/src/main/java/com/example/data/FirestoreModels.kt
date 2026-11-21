package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val email: String = "",
  val photoUrl: String? = null,
  val completedFocusSessions: Long = 0L,
  val totalFocusMinutes: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class SavedConfig(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val jsonContent: String = "",
  val keysCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class FocusSessionLog(
  @DocumentId val id: String = "",
  val userId: String = "",
  val presetName: String = "",
  val durationMinutes: Long = 25L,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ActiveTimerState(
  @DocumentId val id: String = "",
  val userId: String = "",
  val taskName: String = "",
  val presetName: String = "CLASSIC",
  val phase: String = "FOCUS",
  val totalSeconds: Long = 1500L,
  val remainingSeconds: Long = 1500L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)
