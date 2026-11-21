package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.model.AppStrings
import com.example.model.localizedStrings
import com.example.ui.components.SyntaxHighlightedJsonBlock
import com.example.ui.navigation.Destinations
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.launch

private data class TutorialSlide(
  val title: String,
  val description: String,
  val bullets: List<String>,
  val icon: ImageVector,
  val accentIndex: Int,
  val targetRoute: String? = null,
)

@Composable
fun TutorialScreen(
  isArabic: Boolean,
  onToggleArabic: ((Boolean) -> Unit)? = null,
  onDismiss: () -> Unit,
  onNavigateToTab: ((String) -> Unit)? = null,
  onPreviewSound: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onDismiss)

  val strings = remember(isArabic) { localizedStrings(isArabic) }
  val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

  val slides =
    remember(strings) {
      listOf(
        TutorialSlide(
          title = strings.tutorialSlide1Title,
          description = strings.tutorialSlide1Desc,
          bullets =
            listOf(
              strings.tutorialSlide1Bullet1,
              strings.tutorialSlide1Bullet2,
              strings.tutorialSlide1Bullet3,
            ),
          icon = Icons.Default.AutoAwesome,
          accentIndex = 0,
        ),
        TutorialSlide(
          title = strings.tutorialSlide2Title,
          description = strings.tutorialSlide2Desc,
          bullets = listOf(strings.tutorialSlide2Bullet1, strings.tutorialSlide2Bullet2),
          icon = Icons.Default.Tune,
          accentIndex = 1,
          targetRoute = Destinations.ROUTE_CONFIG,
        ),
        TutorialSlide(
          title = strings.tutorialSlide3Title,
          description = strings.tutorialSlide3Desc,
          bullets =
            listOf(
              strings.tutorialSlide3Bullet1,
              strings.tutorialSlide3Bullet2,
              strings.tutorialSlide3Bullet3,
            ),
          icon = Icons.Default.Timer,
          accentIndex = 2,
          targetRoute = Destinations.ROUTE_FOCUS,
        ),
        TutorialSlide(
          title = strings.tutorialSlide4Title,
          description = strings.tutorialSlide4Desc,
          bullets =
            listOf(
              strings.tutorialSlide4Bullet1,
              strings.tutorialSlide4Bullet2,
              strings.tutorialSlide4Bullet3,
            ),
          icon = Icons.Default.Build,
          accentIndex = 0,
          targetRoute = Destinations.ROUTE_TOOLS,
        ),
        TutorialSlide(
          title = strings.tutorialSlide5Title,
          description = strings.tutorialSlide5Desc,
          bullets =
            listOf(
              strings.tutorialSlide5Bullet1,
              strings.tutorialSlide5Bullet2,
              strings.tutorialSlide5Bullet3,
            ),
          icon = Icons.Default.ViewInAr,
          accentIndex = 1,
          targetRoute = Destinations.ROUTE_STUDIO_3D,
        ),
      )
    }

  val pagerState = rememberPagerState(pageCount = { slides.size })
  val scope = rememberCoroutineScope()

  CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
    Scaffold(
      modifier = modifier.fillMaxSize(),
      contentWindowInsets = WindowInsets.safeDrawing,
      containerColor = MaterialTheme.colorScheme.background,
      topBar = {
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Default.School,
                contentDescription = strings.tutorialHeaderBadge,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text =
                  "${strings.tutorialHeaderBadge} • ${pagerState.currentPage + 1}/${slides.size}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            if (onToggleArabic != null) {
              IconButton(
                onClick = { onToggleArabic(!isArabic) },
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .testTag("tutorial_language_toggle"),
              ) {
                Icon(
                  imageVector = Icons.Default.Language,
                  contentDescription = strings.languageLabel,
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            }
            TextButton(
              onClick = onDismiss,
              modifier = Modifier.heightIn(min = 48.dp).testTag("tutorial_skip_button"),
            ) {
              Text(strings.tutorialSkip)
              Spacer(Modifier.width(4.dp))
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = strings.tutorialSkip,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      },
      bottomBar = {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 4.dp,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        ) {
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            // Animated step dots
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              repeat(slides.size) { index ->
                val isSelected = pagerState.currentPage == index
                val width by
                  animateDpAsState(
                    targetValue = if (isSelected) 28.dp else 8.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "dotWidth",
                  )
                val dotColor by
                  animateColorAsState(
                    targetValue =
                      if (isSelected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    label = "dotColor",
                  )
                Box(
                  modifier =
                    Modifier.padding(horizontal = 4.dp)
                      .height(8.dp)
                      .width(width)
                      .clip(CircleShape)
                      .background(dotColor)
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              if (pagerState.currentPage > 0) {
                OutlinedButton(
                  onClick = {
                    scope.launch {
                      pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                  },
                  modifier =
                    Modifier.weight(1f)
                      .heightIn(min = 48.dp)
                      .testTag("tutorial_prev_button"),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = strings.tutorialPrevious,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(Modifier.width(6.dp))
                  Text(strings.tutorialPrevious)
                }
              }

              val isLastPage = pagerState.currentPage == slides.lastIndex
              Button(
                onClick = {
                  if (isLastPage) {
                    onDismiss()
                  } else {
                    scope.launch {
                      pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                  }
                },
                modifier =
                  Modifier.weight(1.4f)
                    .heightIn(min = 48.dp)
                    .testTag("tutorial_next_button"),
              ) {
                Text(
                  text = if (isLastPage) strings.tutorialStartNow else strings.tutorialNext,
                  fontWeight = FontWeight.Bold,
                )
                if (!isLastPage) {
                  Spacer(Modifier.width(6.dp))
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
            }
          }
        }
      },
    ) { innerPadding ->
      Box(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentAlignment = Alignment.TopCenter,
      ) {
        HorizontalPager(
          state = pagerState,
          modifier = Modifier.fillMaxSize().widthIn(max = 720.dp).testTag("tutorial_pager"),
        ) { pageIndex ->
          val slide = slides[pageIndex]
          val accent =
            when (slide.accentIndex) {
              0 -> MaterialTheme.colorScheme.primary
              1 -> MaterialTheme.colorScheme.secondary
              else -> MaterialTheme.colorScheme.tertiary
            }

          Column(
            modifier =
              Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            // Visual Showcase Card with radial/linear glow
            TutorialHeroVisualCard(
              stepNumber = pageIndex + 1,
              stepLabel = strings.tutorialStepLabel,
              icon = slide.icon,
              accent = accent,
            )

            Text(
              text = slide.title,
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
              text = slide.description,
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Key takeaways card
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(20.dp),
              color = MaterialTheme.colorScheme.surface,
              tonalElevation = 2.dp,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            ) {
              Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                slide.bullets.forEach { bullet ->
                  Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Default.CheckCircle,
                      contentDescription = null,
                      tint = accent,
                      modifier = Modifier.size(20.dp).padding(top = 2.dp),
                    )
                    Text(
                      text = bullet,
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                  }
                }
              }
            }

            // Interactive mini-demo on Config slide (page 1)
            if (pageIndex == 1) {
              InteractiveConfigMiniDemo(strings = strings)
            }

            // Interactive sound test demo on Focus slide (page 2)
            if (pageIndex == 2 && onPreviewSound != null) {
              InteractiveSoundMiniDemo(strings = strings, onPreviewSound = onPreviewSound)
            }

            if (slide.targetRoute != null && onNavigateToTab != null) {
              FilledTonalButton(
                onClick = { onNavigateToTab(slide.targetRoute) },
                modifier =
                  Modifier.fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("tutorial_try_tab_${slide.targetRoute}"),
              ) {
                Icon(
                  imageVector = slide.icon,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(strings.tutorialTrySection)
              }
            }

            Spacer(Modifier.height(12.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun TutorialHeroVisualCard(
  stepNumber: Int,
  stepLabel: String,
  icon: ImageVector,
  accent: Color,
) {
  val surfaceHigh = MaterialTheme.colorScheme.surfaceVariant
  val surface = MaterialTheme.colorScheme.surface

  Surface(
    modifier = Modifier.fillMaxWidth().height(156.dp),
    shape = RoundedCornerShape(24.dp),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
    color = Color.Transparent,
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize().drawBehind {
          drawRect(
            brush =
              Brush.linearGradient(
                colors = listOf(surfaceHigh, surface),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
              )
          )
          drawCircle(
            brush =
              Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.28f), Color.Transparent),
                center = Offset(size.width * 0.5f, size.height * 0.5f),
                radius = size.maxDimension * 0.45f,
              ),
            center = Offset(size.width * 0.5f, size.height * 0.5f),
            radius = size.maxDimension * 0.45f,
          )
        },
      contentAlignment = Alignment.Center,
    ) {
      Surface(
        modifier = Modifier.align(Alignment.TopStart).padding(14.dp),
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.16f),
      ) {
        Text(
          text = "$stepLabel $stepNumber",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = accent,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }

      Box(
        modifier =
          Modifier.size(74.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(accent.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accent,
          modifier = Modifier.size(40.dp),
        )
      }
    }
  }
}

@Composable
private fun InteractiveConfigMiniDemo(strings: AppStrings) {
  var formatOnSave by remember { mutableStateOf(true) }
  var tabSize by remember { mutableStateOf(2) }

  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = strings.tutorialSlide2InteractiveHint,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "editor.formatOnSave",
          fontFamily = JetBrainsMonoFontFamily,
          style = MaterialTheme.typography.bodyMedium,
        )
        Switch(
          checked = formatOnSave,
          onCheckedChange = { formatOnSave = it },
          modifier = Modifier.testTag("tutorial_demo_switch"),
        )
      }
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "editor.tabSize:",
          fontFamily = JetBrainsMonoFontFamily,
          style = MaterialTheme.typography.bodySmall,
        )
        listOf(2, 4).forEach { size ->
          OutlinedButton(
            onClick = { tabSize = size },
            modifier = Modifier.heightIn(min = 48.dp),
          ) {
            Text(
              text = size.toString(),
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = if (tabSize == size) FontWeight.Bold else FontWeight.Normal,
            )
          }
        }
      }
      SyntaxHighlightedJsonBlock(
        jsonText =
          """
          {
            "editor.formatOnSave": $formatOnSave,
            "editor.tabSize": $tabSize
          }
          """
            .trimIndent()
      )
    }
  }
}

@Composable
private fun InteractiveSoundMiniDemo(
  strings: AppStrings,
  onPreviewSound: () -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.weight(1f),
      ) {
        Icon(
          imageVector = Icons.Default.PlayArrow,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.tertiary,
        )
        Text(
          text = strings.soundAlertLabel,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
        )
      }
      OutlinedButton(
        onClick = onPreviewSound,
        modifier = Modifier.heightIn(min = 48.dp).testTag("tutorial_test_sound_button"),
      ) {
        Icon(
          imageVector = Icons.Default.NotificationsActive,
          contentDescription = strings.testSoundAction,
          modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(strings.testSoundAction)
      }
    }
  }
}
