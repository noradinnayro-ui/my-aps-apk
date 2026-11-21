package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.SlateTextLight
import com.example.ui.theme.SlateTextMuted

@Composable
fun HeroHeaderCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  badgeText: String? = null,
  badgeColor: Color = MaterialTheme.colorScheme.primary,
  modifier: Modifier = Modifier,
) {
  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary
  val surfaceHigh = MaterialTheme.colorScheme.surfaceVariant
  val surface = MaterialTheme.colorScheme.surface

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    color = Color.Transparent,
  ) {
    Box(
      modifier =
        Modifier.drawBehind {
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
                  colors = listOf(primary.copy(alpha = 0.20f), Color.Transparent),
                  center = Offset(size.width * 0.85f, size.height * 0.15f),
                  radius = size.maxDimension * 0.55f,
                ),
              center = Offset(size.width * 0.85f, size.height * 0.15f),
              radius = size.maxDimension * 0.55f,
            )
            drawCircle(
              brush =
                Brush.radialGradient(
                  colors = listOf(secondary.copy(alpha = 0.12f), Color.Transparent),
                  center = Offset(size.width * 0.12f, size.height * 0.9f),
                  radius = size.maxDimension * 0.45f,
                ),
              center = Offset(size.width * 0.12f, size.height * 0.9f),
              radius = size.maxDimension * 0.45f,
            )
          }
          .padding(20.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier.size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = primary,
              modifier = Modifier.size(24.dp),
            )
          }

          if (!badgeText.isNullOrBlank()) {
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = badgeColor.copy(alpha = 0.14f),
              border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f)),
            ) {
              Text(
                text = badgeText,
                style = MaterialTheme.typography.labelMedium,
                color = badgeColor,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              )
            }
          }
        }

        Text(
          text = title,
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
fun SyntaxHighlightedJsonBlock(
  jsonText: String,
  modifier: Modifier = Modifier,
) {
  val lines = remember(jsonText) { jsonText.lines() }
  val hScroll = rememberScrollState()

  // Code blocks are always rendered Left-to-Right for readability even in RTL Arabic UI
  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Surface(
      modifier = modifier.fillMaxWidth(),
      shape = RoundedCornerShape(18.dp),
      color = ObsidianBg,
      border = BorderStroke(1.dp, ObsidianSurfaceHigh),
    ) {
      Column(
        modifier = Modifier.horizontalScroll(hScroll).padding(vertical = 14.dp, horizontal = 12.dp)
      ) {
        lines.forEachIndexed { index, line ->
          val highlighted = remember(line) { highlightJsonLine(line) }
          Row(verticalAlignment = Alignment.Top) {
            Text(
              text = "${index + 1}".padStart(2, ' '),
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 12.sp,
              lineHeight = 20.sp,
              color = SlateTextMuted.copy(alpha = 0.55f),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
              text = highlighted,
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 13.sp,
              lineHeight = 20.sp,
            )
          }
        }
        if (lines.isEmpty()) {
          Spacer(modifier = Modifier.height(16.dp))
        }
      }
    }
  }
}

private fun highlightJsonLine(line: String): AnnotatedString {
  val keyRegex = Regex("""^(\s*)"([^"]+)"(\s*:\s*)(.*)$""")
  val match = keyRegex.matchEntire(line)
  if (match == null) {
    return buildAnnotatedString {
      withStyle(SpanStyle(color = SlateTextLight.copy(alpha = 0.85f))) { append(line) }
    }
  }
  val indent = match.groupValues[1]
  val key = match.groupValues[2]
  val colon = match.groupValues[3]
  val rest = match.groupValues[4]

  return buildAnnotatedString {
    append(indent)
    withStyle(SpanStyle(color = CyanAccent, fontWeight = FontWeight.Medium)) {
      append("\"$key\"")
    }
    withStyle(SpanStyle(color = SlateTextMuted)) { append(colon) }

    val trimmedRest = rest.trimEnd(',')
    val hasComma = rest.endsWith(",")
    val valueColor =
      when {
        trimmedRest.startsWith("\"") -> EmeraldAccent
        trimmedRest == "true" || trimmedRest == "false" || trimmedRest == "null" -> AmberAccent
        trimmedRest.toDoubleOrNull() != null -> AmberAccent
        else -> SlateTextLight
      }
    withStyle(SpanStyle(color = valueColor)) { append(trimmedRest) }
    if (hasComma) {
      withStyle(SpanStyle(color = SlateTextMuted)) { append(",") }
    }
  }
}
