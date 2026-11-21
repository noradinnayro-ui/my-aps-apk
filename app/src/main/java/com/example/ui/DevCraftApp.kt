package com.example.ui

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.model.FocusPhase
import com.example.model.localizedStrings
import com.example.ui.navigation.Destinations
import com.example.ui.screens.ConfigStudioScreen
import com.example.ui.screens.DevToolsScreen
import com.example.ui.screens.FocusTimerScreen
import com.example.ui.screens.PreferencesScreen
import com.example.ui.screens.ThreeDStudioScreen
import com.example.ui.screens.TutorialScreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.viewmodel.DevCraftViewModel
import kotlinx.coroutines.launch

private data class NavItem(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevCraftApp(
  viewModel: DevCraftViewModel,
  isDarkTheme: Boolean,
  userDisplayName: String,
  userEmail: String,
  onSignOut: () -> Unit,
) {
  val prefs by viewModel.prefsState.collectAsStateWithLifecycle()
  val configState by viewModel.configState.collectAsStateWithLifecycle()
  val focusState by viewModel.focusState.collectAsStateWithLifecycle()
  val toolsState by viewModel.toolsState.collectAsStateWithLifecycle()
  val cloudProfileState by viewModel.cloudProfileState.collectAsStateWithLifecycle()
  val cloudConfigsState by viewModel.cloudConfigsState.collectAsStateWithLifecycle()
  val cloudSessionsState by viewModel.cloudSessionsState.collectAsStateWithLifecycle()
  val showTutorial by viewModel.showTutorial.collectAsStateWithLifecycle()

  val strings = remember(prefs.isArabic) { localizedStrings(prefs.isArabic) }
  val layoutDirection = if (prefs.isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination

  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val clipboardManager = LocalClipboardManager.current
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val lifecycleOwner = LocalLifecycleOwner.current

  DisposableEffect(lifecycleOwner, viewModel) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_STOP) {
        viewModel.persistActiveTimerState()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  LaunchedEffect(viewModel, strings.workCompleteBanner) {
    viewModel.phaseCompletedEvent.collect { completedPhase ->
      if (completedPhase == FocusPhase.FOCUS) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        snackbarHostState.showSnackbar(strings.workCompleteBanner)
      }
    }
  }

  val navItems =
    remember(strings) {
      listOf(
        NavItem(
          route = Destinations.ROUTE_CONFIG,
          label = strings.navConfig,
          selectedIcon = Icons.Filled.Tune,
          unselectedIcon = Icons.Outlined.Tune,
        ),
        NavItem(
          route = Destinations.ROUTE_FOCUS,
          label = strings.navFocus,
          selectedIcon = Icons.Filled.Timer,
          unselectedIcon = Icons.Outlined.Timer,
        ),
        NavItem(
          route = Destinations.ROUTE_TOOLS,
          label = strings.navTools,
          selectedIcon = Icons.Filled.Build,
          unselectedIcon = Icons.Outlined.Build,
        ),
        NavItem(
          route = Destinations.ROUTE_STUDIO_3D,
          label = strings.nav3d,
          selectedIcon = Icons.Filled.ViewInAr,
          unselectedIcon = Icons.Outlined.ViewInAr,
        ),
        NavItem(
          route = Destinations.ROUTE_PREFS,
          label = strings.navPrefs,
          selectedIcon = Icons.Filled.Palette,
          unselectedIcon = Icons.Outlined.Palette,
        ),
      )
    }

  val navigateToTab: (String) -> Unit = { route ->
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  val copyTextWithFeedback: (String) -> Unit = { text ->
    clipboardManager.setText(AnnotatedString(text))
    scope.launch { snackbarHostState.showSnackbar(strings.copiedToast) }
  }

  val shareConfigJson: () -> Unit = {
    val sendIntent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "settings_dd06cfbb.json")
        putExtra(Intent.EXTRA_TEXT, configState.jsonText)
      }
    context.startActivity(Intent.createChooser(sendIntent, strings.shareJson))
  }

  CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useNavRail = maxWidth >= 600.dp

      Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
          CenterAlignedTopAppBar(
            navigationIcon = {
              if (focusState.isRunning && currentDestination?.route != Destinations.ROUTE_FOCUS) {
                val mins = focusState.remainingSeconds / 60
                val secs = focusState.remainingSeconds % 60
                TextButton(
                  onClick = { navigateToTab(Destinations.ROUTE_FOCUS) },
                  shape = RoundedCornerShape(999.dp),
                  modifier = Modifier.testTag("topbar_active_pomodoro_pill"),
                ) {
                  Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = strings.navFocus,
                    tint = MaterialTheme.colorScheme.secondary,
                  )
                  Text(
                    text = String.format(" %02d:%02d", mins, secs),
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                  )
                }
              }
            },
            title = {
              Text(
                text = strings.appName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
              )
            },
            actions = {
              IconButton(
                onClick = viewModel::openTutorial,
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .testTag("topbar_tutorial_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                  contentDescription = strings.tutorialAction,
                  tint = MaterialTheme.colorScheme.secondary,
                )
              }
              TextButton(
                onClick = { viewModel.setArabic(!prefs.isArabic) },
                modifier = Modifier.testTag("topbar_language_toggle"),
              ) {
                Icon(
                  imageVector = Icons.Default.Language,
                  contentDescription = strings.languageLabel,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = if (prefs.isArabic) " EN" else " عربي",
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            },
            colors =
              TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
              ),
          )
        },
        bottomBar = {
          if (!useNavRail) {
            NavigationBar(
              containerColor = MaterialTheme.colorScheme.surface,
              tonalElevation = 4.dp,
            ) {
              navItems.forEach { item ->
                val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                NavigationBarItem(
                  selected = selected,
                  onClick = { navigateToTab(item.route) },
                  icon = {
                    Icon(
                      imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                      contentDescription = item.label,
                    )
                  },
                  label = { Text(item.label, maxLines = 1) },
                  modifier = Modifier.testTag("nav_${item.route}"),
                )
              }
            }
          }
        },
      ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          if (useNavRail) {
            NavigationRail(
              containerColor = MaterialTheme.colorScheme.surface,
              modifier = Modifier.fillMaxHeight(),
            ) {
              navItems.forEach { item ->
                val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                NavigationRailItem(
                  selected = selected,
                  onClick = { navigateToTab(item.route) },
                  icon = {
                    Icon(
                      imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                      contentDescription = item.label,
                    )
                  },
                  label = { Text(item.label) },
                  modifier = Modifier.testTag("rail_${item.route}"),
                )
              }
            }
          }

          Box(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentAlignment = Alignment.TopCenter,
          ) {
            NavHost(
              navController = navController,
              startDestination = Destinations.ROUTE_CONFIG,
              modifier = Modifier.fillMaxSize().widthIn(max = 840.dp),
            ) {
              composable(Destinations.ROUTE_CONFIG) {
                ConfigStudioScreen(
                  state = configState,
                  cloudConfigsState = cloudConfigsState,
                  strings = strings,
                  isArabic = prefs.isArabic,
                  onSelectPreset = viewModel::selectPreset,
                  onResetOriginal = {
                    viewModel.resetToAttachedOriginal()
                    scope.launch { snackbarHostState.showSnackbar(strings.resetToast) }
                  },
                  onViewModeChange = viewModel::setConfigViewMode,
                  onSearchChange = viewModel::setConfigSearchQuery,
                  onUpdateNode = viewModel::updateSettingValue,
                  onDeleteNode = viewModel::deleteSettingKey,
                  onAddKey = viewModel::addSettingKey,
                  onRawJsonChange = viewModel::onRawJsonEdited,
                  onFormatJson = viewModel::formatActiveConfig,
                  onMinifyJson = viewModel::minifyActiveConfig,
                  onSortKeys = viewModel::sortActiveConfigKeys,
                  onCopyJson = { copyTextWithFeedback(configState.jsonText) },
                  onShareJson = shareConfigJson,
                  onSaveConfigToCloud = { title ->
                    viewModel.saveActiveConfigToCloud(title) { result ->
                      scope.launch {
                        val msg =
                          if (result.isSuccess) strings.savedToCloudToast
                          else result.exceptionOrNull()?.localizedMessage ?: strings.invalidJson
                        snackbarHostState.showSnackbar(msg)
                      }
                    }
                  },
                  onLoadCloudConfig = viewModel::loadCloudConfig,
                  onDeleteCloudConfig = { configId ->
                    viewModel.deleteCloudConfig(configId) { result ->
                      scope.launch {
                        val msg =
                          if (result.isSuccess) strings.deletedFromCloudToast
                          else result.exceptionOrNull()?.localizedMessage ?: "Delete failed"
                        snackbarHostState.showSnackbar(msg)
                      }
                    }
                  },
                )
              }
              composable(Destinations.ROUTE_FOCUS) {
                FocusTimerScreen(
                  state = focusState,
                  completedSessionsToday = prefs.completedSessionsToday,
                  soundAlertEnabled = prefs.soundAlertEnabled,
                  cloudProfileState = cloudProfileState,
                  cloudSessionsState = cloudSessionsState,
                  strings = strings,
                  onSelectPreset = viewModel::selectFocusPreset,
                  onToggleTimer = viewModel::toggleFocusTimer,
                  onResetTimer = viewModel::resetFocusTimer,
                  onSkipPhase = viewModel::skipFocusPhase,
                  onTaskNameChange = viewModel::updateFocusTaskName,
                  onToggleSoundAlert = viewModel::setSoundAlertEnabled,
                  onTestSoundAlert = viewModel::testSoundAlert,
                )
              }
              composable(Destinations.ROUTE_TOOLS) {
                DevToolsScreen(
                  state = toolsState,
                  strings = strings,
                  onSelectTab = viewModel::selectToolTab,
                  onCopyText = copyTextWithFeedback,
                  onJsonInputChange = viewModel::updateJsonLabInput,
                  onImportActiveConfig = viewModel::importActiveConfigIntoJsonLab,
                  onFormatJson = viewModel::formatJsonLab,
                  onMinifyJson = viewModel::minifyJsonLab,
                  onSortJson = viewModel::sortJsonLab,
                  onCodecModeChange = viewModel::setCodecMode,
                  onCodecInputChange = viewModel::updateCodecInput,
                  onEncodeCodec = viewModel::encodeCodec,
                  onDecodeCodec = viewModel::decodeCodec,
                  onSwapCodec = viewModel::swapCodec,
                  onGenerateUuids = viewModel::generateUuids,
                  onUpdateTokenConfig = viewModel::updateTokenConfig,
                  onUpdateRgb = viewModel::updateColorRgb,
                )
              }
              composable(Destinations.ROUTE_STUDIO_3D) {
                ThreeDStudioScreen(
                  isFocusRunning = focusState.isRunning,
                  isDarkTheme = isDarkTheme,
                  strings = strings,
                )
              }
              composable(Destinations.ROUTE_PREFS) {
                PreferencesScreen(
                  prefs = prefs,
                  strings = strings,
                  userDisplayName = userDisplayName,
                  userEmail = userEmail,
                  onSetArabic = viewModel::setArabic,
                  onSetThemeMode = viewModel::setThemeMode,
                  onSetDynamicColor = viewModel::setDynamicColor,
                  onSetFocusMinutes = viewModel::setCustomFocusMinutes,
                  onSetBreakMinutes = viewModel::setCustomBreakMinutes,
                  onSetSoundAlertEnabled = viewModel::setSoundAlertEnabled,
                  onSetSoundAlertStyle = viewModel::setSoundAlertStyle,
                  onTestSoundAlert = viewModel::testSoundAlert,
                  onOpenTutorial = viewModel::openTutorial,
                  onSignOut = onSignOut,
                )
              }
            }
          }
        }
      }

      if (showTutorial) {
        TutorialScreen(
          isArabic = prefs.isArabic,
          onToggleArabic = viewModel::setArabic,
          onDismiss = viewModel::dismissTutorial,
          onNavigateToTab = { route ->
            viewModel.dismissTutorial()
            navigateToTab(route)
          },
          onPreviewSound = viewModel::testSoundAlert,
        )
      }
    }
  }
}
