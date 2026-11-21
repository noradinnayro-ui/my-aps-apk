package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.UserPreferences
import com.example.data.UserPreferencesRepository
import com.example.model.ThemeMode
import com.example.ui.DevCraftApp
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.DevCraftTheme
import com.example.viewmodel.DevCraftViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val rootPrefsRepo = remember { UserPreferencesRepository(applicationContext) }
      val rootPrefs by rootPrefsRepo.preferencesFlow.collectAsStateWithLifecycle(UserPreferences())
      val scope = rememberCoroutineScope()
      val credentialManager = remember { CredentialManager.create(this@MainActivity) }

      val currentUser by
        remember { Firebase.auth.authStateFlow() }
          .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

      LaunchedEffect(Unit) {
        attemptAutoSignIn(
          context = this@MainActivity,
          credentialManager = credentialManager,
          onAuthSuccess = {},
          onUnauthenticated = {},
          scope = scope,
        )
      }

      val useDark =
        when (rootPrefs.themeMode) {
          ThemeMode.SYSTEM -> isSystemInDarkTheme()
          ThemeMode.DARK -> true
          ThemeMode.LIGHT -> false
        }

      DevCraftTheme(darkTheme = useDark, dynamicColor = rootPrefs.dynamicColor) {
        val user = currentUser
        if (user == null) {
          AuthScreen(
            isArabic = rootPrefs.isArabic,
            onToggleArabic = { arabic -> scope.launch { rootPrefsRepo.setArabic(arabic) } },
            onAuthSuccess = {},
          )
        } else {
          key(user.uid) {
            val vm: DevCraftViewModel =
              viewModel(
                key = user.uid,
                factory = DevCraftViewModel.Factory(application, user.uid),
              )
            val displayName =
              user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore("@")
                ?: "Developer"
            DevCraftApp(
              viewModel = vm,
              isDarkTheme = useDark,
              userDisplayName = displayName,
              userEmail = user.email.orEmpty(),
              onSignOut = {
                signOutUser(credentialManager = credentialManager, scope = scope)
              },
            )
          }
        }
      }
    }
  }
}
