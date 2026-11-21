package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DevUtilitiesEngine
import com.example.model.AppStrings
import com.example.model.DevToolTab
import com.example.ui.components.HeroHeaderCard
import com.example.ui.components.SyntaxHighlightedJsonBlock
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.viewmodel.ToolsUiState
import kotlin.math.roundToInt

@Composable
fun DevToolsScreen(
  state: ToolsUiState,
  strings: AppStrings,
  onSelectTab: (DevToolTab) -> Unit,
  onCopyText: (String) -> Unit,
  // JSON Lab
  onJsonInputChange: (String) -> Unit,
  onImportActiveConfig: () -> Unit,
  onFormatJson: (Int) -> Unit,
  onMinifyJson: () -> Unit,
  onSortJson: () -> Unit,
  // Codec
  onCodecModeChange: (Boolean) -> Unit,
  onCodecInputChange: (String) -> Unit,
  onEncodeCodec: () -> Unit,
  onDecodeCodec: () -> Unit,
  onSwapCodec: () -> Unit,
  // Generator
  onGenerateUuids: (Int) -> Unit,
  onUpdateTokenConfig: (Int, Boolean, Boolean, Boolean, Boolean) -> Unit,
  // Color
  onUpdateRgb: (Int, Int, Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      HeroHeaderCard(
        title = strings.toolsHeroTitle,
        subtitle = "JSON • Base64/URL • UUID v4 • WCAG 2.1",
        icon = Icons.Default.Build,
      )
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        val tabs =
          listOf(
            Triple(DevToolTab.JSON_LAB, strings.tabJsonLab, Icons.Default.DataObject),
            Triple(DevToolTab.CODEC, strings.tabCodec, Icons.Default.Lock),
            Triple(DevToolTab.GENERATOR, strings.tabGenerator, Icons.Default.Key),
            Triple(DevToolTab.COLOR, strings.tabColor, Icons.Default.ColorLens),
          )
        tabs.forEach { (tab, label, icon) ->
          FilterChip(
            selected = state.activeTab == tab,
            onClick = { onSelectTab(tab) },
            leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
            label = { Text(label) },
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("tool_tab_${tab.name}"),
          )
        }
      }
    }

    when (state.activeTab) {
      DevToolTab.JSON_LAB -> {
        item {
          JsonLabSection(
            state = state,
            strings = strings,
            onInputChange = onJsonInputChange,
            onImportActiveConfig = onImportActiveConfig,
            onFormat = onFormatJson,
            onMinify = onMinifyJson,
            onSort = onSortJson,
            onCopyOutput = { onCopyText(state.jsonOutput) },
          )
        }
      }
      DevToolTab.CODEC -> {
        item {
          CodecSection(
            state = state,
            strings = strings,
            onModeChange = onCodecModeChange,
            onInputChange = onCodecInputChange,
            onEncode = onEncodeCodec,
            onDecode = onDecodeCodec,
            onSwap = onSwapCodec,
            onCopyOutput = { onCopyText(state.codecOutput) },
          )
        }
      }
      DevToolTab.GENERATOR -> {
        item {
          GeneratorSection(
            state = state,
            strings = strings,
            onGenerateUuids = onGenerateUuids,
            onUpdateTokenConfig = onUpdateTokenConfig,
            onCopyText = onCopyText,
          )
        }
      }
      DevToolTab.COLOR -> {
        item {
          ColorWcagSection(
            state = state,
            strings = strings,
            onUpdateRgb = onUpdateRgb,
            onCopyHex = onCopyText,
          )
        }
      }
    }

    item { Spacer(modifier = Modifier.height(12.dp)) }
  }
}

@Composable
private fun JsonLabSection(
  state: ToolsUiState,
  strings: AppStrings,
  onInputChange: (String) -> Unit,
  onImportActiveConfig: () -> Unit,
  onFormat: (Int) -> Unit,
  onMinify: () -> Unit,
  onSort: () -> Unit,
  onCopyOutput: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      FilledTonalButton(
        onClick = onImportActiveConfig,
        modifier = Modifier.heightIn(min = 48.dp).testTag("json_lab_import_button"),
      ) {
        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(strings.loadActiveConfig)
      }
      OutlinedButton(
        onClick = { onFormat(2) },
        modifier = Modifier.heightIn(min = 48.dp).testTag("json_lab_indent2_button"),
      ) {
        Text(strings.indent2)
      }
      OutlinedButton(
        onClick = { onFormat(4) },
        modifier = Modifier.heightIn(min = 48.dp).testTag("json_lab_indent4_button"),
      ) {
        Text(strings.indent4)
      }
      OutlinedButton(
        onClick = onMinify,
        modifier = Modifier.heightIn(min = 48.dp).testTag("json_lab_minify_button"),
      ) {
        Text(strings.minifyJson)
      }
      OutlinedButton(
        onClick = onSort,
        modifier = Modifier.heightIn(min = 48.dp).testTag("json_lab_sort_button"),
      ) {
        Text(strings.sortKeys)
      }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
      OutlinedTextField(
        value = state.jsonInput,
        onValueChange = onInputChange,
        label = { Text(strings.jsonInputLabel) },
        textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 13.sp),
        minLines = 5,
        maxLines = 10,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("json_lab_input"),
      )
    }

    if (state.jsonLabError != null) {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
      ) {
        Text(
          text = state.jsonLabError,
          color = MaterialTheme.colorScheme.onErrorContainer,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(12.dp),
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(strings.jsonOutputLabel, style = MaterialTheme.typography.titleSmall)
      OutlinedButton(
        onClick = onCopyOutput,
        modifier = Modifier.heightIn(min = 48.dp).testTag("json_lab_copy_output"),
      ) {
        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(strings.copyJson)
      }
    }

    SyntaxHighlightedJsonBlock(jsonText = state.jsonOutput)
  }
}

@Composable
private fun CodecSection(
  state: ToolsUiState,
  strings: AppStrings,
  onModeChange: (Boolean) -> Unit,
  onInputChange: (String) -> Unit,
  onEncode: () -> Unit,
  onDecode: () -> Unit,
  onSwap: () -> Unit,
  onCopyOutput: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChip(
        selected = state.isBase64Mode,
        onClick = { onModeChange(true) },
        label = { Text(strings.base64Mode) },
        modifier = Modifier.minimumInteractiveComponentSize().testTag("codec_base64_chip"),
      )
      FilterChip(
        selected = !state.isBase64Mode,
        onClick = { onModeChange(false) },
        label = { Text(strings.urlMode) },
        modifier = Modifier.minimumInteractiveComponentSize().testTag("codec_url_chip"),
      )
    }

    OutlinedTextField(
      value = state.codecInput,
      onValueChange = onInputChange,
      label = { Text(strings.codecInputLabel) },
      minLines = 3,
      maxLines = 6,
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth().testTag("codec_input_field"),
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Button(
        onClick = onEncode,
        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("codec_encode_button"),
      ) {
        Text(strings.encodeAction)
      }
      FilledTonalButton(
        onClick = onDecode,
        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("codec_decode_button"),
      ) {
        Text(strings.decodeAction)
      }
      OutlinedButton(
        onClick = onSwap,
        modifier = Modifier.heightIn(min = 48.dp).testTag("codec_swap_button"),
      ) {
        Icon(Icons.Default.SwapVert, contentDescription = strings.swapAction)
      }
    }

    if (state.codecError != null) {
      Text(
        text = state.codecError,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
      )
    }

    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(strings.codecOutputLabel, style = MaterialTheme.typography.titleSmall)
          IconButton(
            onClick = onCopyOutput,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("codec_copy_button"),
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = strings.copyJson)
          }
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
          Text(
            text = state.codecOutput,
            fontFamily = JetBrainsMonoFontFamily,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }
  }
}

@Composable
private fun GeneratorSection(
  state: ToolsUiState,
  strings: AppStrings,
  onGenerateUuids: (Int) -> Unit,
  onUpdateTokenConfig: (Int, Boolean, Boolean, Boolean, Boolean) -> Unit,
  onCopyText: (String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    // UUID Generator Card
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = { onGenerateUuids(1) },
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("gen_uuid_1_button"),
          ) {
            Text(strings.generateUuid)
          }
          OutlinedButton(
            onClick = { onGenerateUuids(5) },
            modifier = Modifier.heightIn(min = 48.dp).testTag("gen_uuid_5_button"),
          ) {
            Text("x5")
          }
        }

        state.generatedUuids.forEachIndexed { idx, uuid ->
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                  text = uuid,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 12.sp,
                  modifier = Modifier.weight(1f),
                )
              }
              IconButton(
                onClick = { onCopyText(uuid) },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("copy_uuid_$idx"),
              ) {
                Icon(
                  Icons.Default.ContentCopy,
                  contentDescription = strings.copyJson,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }
        }
      }
    }

    // Secure Token Generator Card
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("${strings.tokenLength}: ${state.tokenLength}", style = MaterialTheme.typography.titleSmall)
          Button(
            onClick = {
              onUpdateTokenConfig(
                state.tokenLength,
                state.includeUpper,
                state.includeLower,
                state.includeDigits,
                state.includeSymbols,
              )
            },
            modifier = Modifier.heightIn(min = 48.dp).testTag("gen_token_button"),
          ) {
            Text(strings.generateToken)
          }
        }

        Slider(
          value = state.tokenLength.toFloat(),
          onValueChange = {
            onUpdateTokenConfig(
              it.roundToInt(),
              state.includeUpper,
              state.includeLower,
              state.includeDigits,
              state.includeSymbols,
            )
          },
          valueRange = 8f..64f,
          modifier = Modifier.testTag("token_length_slider"),
        )

        ToggleOptionRow(strings.includeUpper, state.includeUpper) {
          onUpdateTokenConfig(
            state.tokenLength,
            it,
            state.includeLower,
            state.includeDigits,
            state.includeSymbols,
          )
        }
        ToggleOptionRow(strings.includeLower, state.includeLower) {
          onUpdateTokenConfig(
            state.tokenLength,
            state.includeUpper,
            it,
            state.includeDigits,
            state.includeSymbols,
          )
        }
        ToggleOptionRow(strings.includeDigits, state.includeDigits) {
          onUpdateTokenConfig(
            state.tokenLength,
            state.includeUpper,
            state.includeLower,
            it,
            state.includeSymbols,
          )
        }
        ToggleOptionRow(strings.includeSymbols, state.includeSymbols) {
          onUpdateTokenConfig(
            state.tokenLength,
            state.includeUpper,
            state.includeLower,
            state.includeDigits,
            it,
          )
        }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
              Text(
                text = state.generatedToken,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
              )
            }
            IconButton(
              onClick = { onCopyText(state.generatedToken) },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("copy_token_button"),
            ) {
              Icon(Icons.Default.ContentCopy, contentDescription = strings.copyJson)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ToggleOptionRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(label, style = MaterialTheme.typography.bodyMedium)
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

@Composable
private fun ColorWcagSection(
  state: ToolsUiState,
  strings: AppStrings,
  onUpdateRgb: (Int, Int, Int) -> Unit,
  onCopyHex: (String) -> Unit,
) {
  val hex = DevUtilitiesEngine.rgbToHex(state.red, state.green, state.blue)
  val swatchColor = Color(state.red, state.green, state.blue)
  val ratioWhite = DevUtilitiesEngine.contrastRatio(state.red, state.green, state.blue, 255, 255, 255)
  val ratioDark = DevUtilitiesEngine.contrastRatio(state.red, state.green, state.blue, 15, 23, 42)

  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
  ) {
    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(swatchColor),
        contentAlignment = Alignment.Center,
      ) {
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = Color.Black.copy(alpha = 0.45f),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = hex,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              color = Color.White,
            )
            IconButton(
              onClick = { onCopyHex(hex) },
              modifier = Modifier.size(48.dp).testTag("copy_hex_button"),
            ) {
              Icon(
                Icons.Default.ContentCopy,
                contentDescription = strings.copyJson,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }

      RgbSliderRow("R", state.red) { onUpdateRgb(it, state.green, state.blue) }
      RgbSliderRow("G", state.green) { onUpdateRgb(state.red, it, state.blue) }
      RgbSliderRow("B", state.blue) { onUpdateRgb(state.red, state.green, it) }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ContrastBadgeCard(
          label = strings.contrastWhite,
          ratio = ratioWhite,
          strings = strings,
          modifier = Modifier.weight(1f),
        )
        ContrastBadgeCard(
          label = strings.contrastDark,
          ratio = ratioDark,
          strings = strings,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun RgbSliderRow(channel: String, value: Int, onValueChange: (Int) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Text(
      text = channel,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.width(20.dp),
    )
    Slider(
      value = value.toFloat(),
      onValueChange = { onValueChange(it.roundToInt()) },
      valueRange = 0f..255f,
      modifier = Modifier.weight(1f),
    )
    Text(
      text = value.toString().padStart(3, ' '),
      fontFamily = JetBrainsMonoFontFamily,
      modifier = Modifier.width(36.dp),
    )
  }
}

@Composable
private fun ContrastBadgeCard(
  label: String,
  ratio: Double,
  strings: AppStrings,
  modifier: Modifier = Modifier,
) {
  val statusText =
    when {
      ratio >= 7.0 -> strings.wcagPassAaa
      ratio >= 4.5 -> strings.wcagPassAa
      else -> strings.wcagFail
    }
  val statusColor = if (ratio >= 4.5) EmeraldAccent else ErrorRed

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(label, style = MaterialTheme.typography.labelSmall)
      Text(
        text = String.format("%.2f:1", ratio),
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleMedium,
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Icon(
          imageVector = if (ratio >= 4.5) Icons.Default.CheckCircle else Icons.Default.Warning,
          contentDescription = null,
          tint = statusColor,
          modifier = Modifier.size(16.dp),
        )
        Text(statusText, style = MaterialTheme.typography.labelMedium, color = statusColor)
      }
    }
  }
}
