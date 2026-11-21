package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

class CloudWorkspaceRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val timerSaveMutex = Mutex()

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  suspend fun ensureUserProfile(user: FirebaseUser): Result<Unit> {
    val uid = user.uid
    val path = "users/$uid"
    return try {
      check(auth.currentUser?.uid == uid) { "Signed-in user mismatch" }
      val docRef = db.collection("users").document(uid)
      val snap = docRef.get().await()
      val safeName =
        (user.displayName?.takeIf { it.isNotBlank() } ?: user.email?.substringBefore("@") ?: "Developer")
          .take(100)
      val safeEmail = (user.email ?: "").take(200)
      val safePhoto = user.photoUrl?.toString()?.take(500)

      if (!snap.exists()) {
        val payload =
          mapOf(
            "userId" to uid,
            "displayName" to safeName,
            "email" to safeEmail,
            "photoUrl" to safePhoto,
            "completedFocusSessions" to 0L,
            "totalFocusMinutes" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }
        docRef.set(payload).await()
      } else {
        val updates =
          mapOf(
            "displayName" to safeName,
            "email" to safeEmail,
            "photoUrl" to safePhoto,
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }
        docRef.update(updates).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Refusing cross-user observation" }
    val path = "users/$uid"
    emitAll(
      db.collection("users")
        .document(uid)
        .snapshots()
        .map { snapshot ->
          snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          val fsErr =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (fsErr == null && error is CancellationException) throw error
          if (auth.currentUser?.uid == uid) {
            handleFirestoreError(fsErr ?: error, OperationType.GET, path)
          }
          throw error
        }
    )
  }

  fun observeSavedConfigs(userId: String): Flow<List<SavedConfig>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Refusing cross-user config list observation" }
    val path = "users/$uid/configs"
    emitAll(
      db.collection("users")
        .document(uid)
        .collection("configs")
        .orderBy("updatedAt", Query.Direction.DESCENDING)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            SavedConfig::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          val fsErr =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (fsErr == null && error is CancellationException) throw error
          if (auth.currentUser?.uid == uid) {
            handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  fun observeFocusSessions(userId: String): Flow<List<FocusSessionLog>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Refusing cross-user session list observation" }
    val path = "users/$uid/focus_sessions"
    emitAll(
      db.collection("users")
        .document(uid)
        .collection("focus_sessions")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(20)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            FocusSessionLog::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          val fsErr =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (fsErr == null && error is CancellationException) throw error
          if (auth.currentUser?.uid == uid) {
            handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  suspend fun saveConfig(
    userId: String,
    title: String,
    jsonContent: String,
    keysCount: Int,
    existingConfigId: String? = null,
  ): Result<Unit> {
    var docPath = "users/$userId/configs"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user mismatch" }
      val cleanTitle = title.trim().take(80)
      require(cleanTitle.isNotEmpty()) { "Config title must not be empty" }
      require(jsonContent.length in 2..50000) { "Config JSON length out of bounds" }
      val safeKeys = keysCount.coerceIn(0, 500).toLong()

      val collectionRef = db.collection("users").document(uid).collection("configs")
      if (!existingConfigId.isNullOrBlank()) {
        val docRef = collectionRef.document(existingConfigId)
        docPath = docRef.path
        docRef
          .update(
            mapOf(
              "title" to cleanTitle,
              "jsonContent" to jsonContent,
              "keysCount" to safeKeys,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        val docRef = collectionRef.document()
        docPath = docRef.path
        docRef
          .set(
            mapOf(
              "userId" to uid,
              "title" to cleanTitle,
              "jsonContent" to jsonContent,
              "keysCount" to safeKeys,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docPath)
      Result.failure(e)
    }
  }

  suspend fun deleteConfig(userId: String, configId: String): Result<Unit> {
    val docPath = "users/$userId/configs/$configId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user mismatch" }
      require(configId.isNotBlank()) { "Config ID must not be blank" }
      db.collection("users").document(uid).collection("configs").document(configId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }

  suspend fun logCompletedFocusSession(
    userId: String,
    presetName: String,
    durationMinutes: Int,
  ): Result<Unit> {
    val safePreset = presetName.trim().ifEmpty { "FOCUS" }.take(40)
    val safeMinutes = durationMinutes.coerceIn(1, 180).toLong()
    val sessionRef = db.collection("users").document(userId).collection("focus_sessions").document()
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user mismatch" }
      sessionRef
        .set(
          mapOf(
            "userId" to uid,
            "presetName" to safePreset,
            "durationMinutes" to safeMinutes,
            "createdAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()

      db.collection("users")
        .document(uid)
        .update(
          mapOf(
            "completedFocusSessions" to FieldValue.increment(1L),
            "totalFocusMinutes" to FieldValue.increment(safeMinutes),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, sessionRef.path)
      Result.failure(e)
    }
  }

  suspend fun getActiveTimerState(userId: String): Result<ActiveTimerState?> {
    val docPath = "users/$userId/active_timer/current"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user mismatch" }
      val snapshot =
        db.collection("users").document(uid).collection("active_timer").document("current").get().await()
      val state =
        snapshot.toObject(
          ActiveTimerState::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      Result.success(state)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docPath)
      Result.failure(e)
    }
  }

  suspend fun saveActiveTimerState(
    userId: String,
    taskName: String,
    presetName: String,
    phaseName: String,
    totalSeconds: Int,
    remainingSeconds: Int,
  ): Result<Unit> {
    val docPath = "users/$userId/active_timer/current"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user mismatch" }
      val safeTask = taskName.trim().take(120)
      val safePreset = presetName.trim().ifEmpty { "CLASSIC" }.take(40)
      val safePhase = if (phaseName == "BREAK") "BREAK" else "FOCUS"
      val safeTotal = totalSeconds.coerceIn(60, 10800).toLong()
      val safeRemaining = remainingSeconds.coerceIn(0, safeTotal.toInt()).toLong()

      timerSaveMutex.withLock {
        val docRef =
          db.collection("users").document(uid).collection("active_timer").document("current")
        val snap = docRef.get().await()
        if (!snap.exists()) {
          docRef
            .set(
              mapOf(
                "userId" to uid,
                "taskName" to safeTask,
                "presetName" to safePreset,
                "phase" to safePhase,
                "totalSeconds" to safeTotal,
                "remainingSeconds" to safeRemaining,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
              )
            )
            .await()
        } else {
          docRef
            .update(
              mapOf(
                "taskName" to safeTask,
                "presetName" to safePreset,
                "phase" to safePhase,
                "totalSeconds" to safeTotal,
                "remainingSeconds" to safeRemaining,
                "updatedAt" to FieldValue.serverTimestamp(),
              )
            )
            .await()
        }
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docPath)
      Result.failure(e)
    }
  }
}
