package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SavedConfig
import com.example.model.AppStrings
import com.example.model.CloudUiState
import com.example.model.ConfigPreset
import com.example.model.ConfigViewMode
import com.example.model.SettingNode
import com.example.ui.components.HeroHeaderCard
import com.example.ui.components.SyntaxHighlightedJsonBlock
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.viewmodel.ConfigUiState

@Composable
fun ConfigStudioScreen(
  state: ConfigUiState,
  cloudConfigsState: CloudUiState<List<SavedConfig>>,
  strings: AppStrings,
  isArabic: Boolean,
  onSelectPreset: (ConfigPreset) -> Unit,
  onResetOriginal: () -> Unit,
  onViewModeChange: (ConfigViewMode) -> Unit,
  onSearchChange: (String) -> Unit,
  onUpdateNode: (String, Any) -> Unit,
  onDeleteNode: (String) -> Unit,
  onAddKey: (String, Any) -> Unit,
  onRawJsonChange: (String) -> Unit,
  onFormatJson: () -> Unit,
  onMinifyJson: () -> Unit,
  onSortKeys: () -> Unit,
  onCopyJson: () -> Unit,
  onShareJson: () -> Unit,
  onSaveConfigToCloud: (String) -> Unit,
  onLoadCloudConfig: (SavedConfig) -> Unit,
  onDeleteCloudConfig: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showAddDialog by remember { mutableStateOf(false) }
  var showSaveCloudDialog by remember { mutableStateOf(false) }

  val filteredNodes =
    remember(state.nodes, state.searchQuery) {
      val q = state.searchQuery.trim().lowercase()
      if (q.isEmpty()) state.nodes
      else state.nodes.filter { nodeMatchesQuery(it, q) }
    }

  val statusBadge =
    if (state.isValidJson) {
      "${strings.validJson} • ${state.nodes.size} ${strings.keysLabel}"
    } else {
      strings.invalidJson
    }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      HeroHeaderCard(
        title = strings.configHeroTitle,
        subtitle = strings.configHeroSubtitle,
        icon = Icons.Default.Tune,
        badgeText = statusBadge,
        badgeColor =
          if (state.isValidJson) MaterialTheme.colorScheme.tertiary
          else MaterialTheme.colorScheme.error,
      )
    }

    // Presets & Primary Actions Row
    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = strings.presetsTitle,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          state.presets.forEach { preset ->
            FilterChip(
              selected = state.activePresetId == preset.id,
              onClick = { onSelectPreset(preset) },
              label = { Text(if (isArabic) preset.titleAr else preset.titleEn) },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("preset_${preset.id}"),
            )
          }
        }
      }
    }

    // Mode Switcher & Search
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          SegmentedButton(
            selected = state.viewMode == ConfigViewMode.VISUAL,
            onClick = { onViewModeChange(ConfigViewMode.VISUAL) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            icon = {
              Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            modifier = Modifier.testTag("mode_visual_button"),
          ) {
            Text(strings.visualMode)
          }
          SegmentedButton(
            selected = state.viewMode == ConfigViewMode.CODE,
            onClick = { onViewModeChange(ConfigViewMode.CODE) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            icon = {
              Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            modifier = Modifier.testTag("mode_code_button"),
          ) {
            Text(strings.codeMode)
          }
        }

        // Quick Toolbar: Save Cloud, Add, Copy, Share, Reset
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = { showSaveCloudDialog = true },
            enabled = state.isValidJson,
            modifier = Modifier.heightIn(min = 48.dp).testTag("save_cloud_config_button"),
          ) {
            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(strings.saveToCloudAction)
          }
          FilledTonalButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.heightIn(min = 48.dp).testTag("add_setting_button"),
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(strings.addKey)
          }
          OutlinedButton(
            onClick = onCopyJson,
            modifier = Modifier.heightIn(min = 48.dp).testTag("copy_config_button"),
          ) {
            Icon(
              Icons.Default.ContentCopy,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(strings.copyJson)
          }
          OutlinedButton(
            onClick = onShareJson,
            modifier = Modifier.heightIn(min = 48.dp).testTag("share_config_button"),
          ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(strings.shareJson)
          }
          OutlinedButton(
            onClick = onResetOriginal,
            modifier = Modifier.heightIn(min = 48.dp).testTag("reset_config_button"),
          ) {
            Icon(
              Icons.Default.RestartAlt,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(strings.resetOriginal)
          }
        }
      }
    }

    // Cloud Synced Configurations Section
    item {
      CloudConfigsSection(
        cloudConfigsState = cloudConfigsState,
        activePresetId = state.activePresetId,
        strings = strings,
        onLoadConfig = onLoadCloudConfig,
        onDeleteConfig = onDeleteCloudConfig,
      )
    }

    if (state.viewMode == ConfigViewMode.VISUAL) {
      item {
        OutlinedTextField(
          value = state.searchQuery,
          onValueChange = onSearchChange,
          placeholder = { Text(strings.searchHint) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth().testTag("config_search_input"),
        )
      }

      if (filteredNodes.isEmpty()) {
        item {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
          ) {
            Column(
              modifier = Modifier.padding(28.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp),
              )
              Text(
                text = strings.emptySearch,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        items(items = filteredNodes, key = { it.path }) { node ->
          SettingNodeCard(
            node = node,
            onUpdate = onUpdateNode,
            onDelete = onDeleteNode,
          )
        }
      }
    } else {
      // Code Mode: Formatting actions + Editable JSON + Syntax Highlight Preview
      item {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilledTonalButton(
            onClick = onFormatJson,
            modifier = Modifier.heightIn(min = 48.dp).testTag("format_config_button"),
          ) {
            Icon(
              Icons.Default.AutoFixHigh,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(strings.formatJson)
          }
          OutlinedButton(
            onClick = onMinifyJson,
            modifier = Modifier.heightIn(min = 48.dp).testTag("minify_config_button"),
          ) {
            Icon(
              Icons.Default.Compress,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(strings.minifyJson)
          }
          OutlinedButton(
            onClick = onSortKeys,
            modifier = Modifier.heightIn(min = 48.dp).testTag("sort_config_button"),
          ) {
            Icon(
              Icons.Default.SortByAlpha,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(strings.sortKeys)
          }
        }
      }

      item {
        AnimatedVisibility(visible = !state.isValidJson && state.jsonError != null) {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.errorContainer,
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
              )
              Text(
                text = state.jsonError ?: strings.invalidJson,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
              )
            }
          }
        }
      }

      item {
        SyntaxHighlightedJsonBlock(jsonText = state.jsonText)
      }

      item {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = strings.rawJsonLabel,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            OutlinedTextField(
              value = state.jsonText,
              onValueChange = onRawJsonChange,
              textStyle =
                TextStyle(
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 13.sp,
                  lineHeight = 20.sp,
                ),
              minLines = 8,
              maxLines = 16,
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth().testTag("raw_json_editor"),
            )
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(12.dp)) }
  }

  if (showAddDialog) {
    AddSettingDialog(
      strings = strings,
      onDismiss = { showAddDialog = false },
      onConfirm = { key, value ->
        onAddKey(key, value)
        showAddDialog = false
      },
    )
  }

  if (showSaveCloudDialog) {
    SaveCloudConfigDialog(
      strings = strings,
      onDismiss = { showSaveCloudDialog = false },
      onConfirm = { title ->
        onSaveConfigToCloud(title)
        showSaveCloudDialog = false
      },
    )
  }
}

@Composable
private fun CloudConfigsSection(
  cloudConfigsState: CloudUiState<List<SavedConfig>>,
  activePresetId: String,
  strings: AppStrings,
  onLoadConfig: (SavedConfig) -> Unit,
  onDeleteConfig: (String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = Icons.Default.CloudDone,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.size(18.dp),
      )
      Text(
        text = strings.cloudConfigsTitle,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    when (cloudConfigsState) {
      is CloudUiState.Loading -> {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Text(strings.cloudLoading, style = MaterialTheme.typography.bodySmall)
          }
        }
      }
      is CloudUiState.Error -> {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.errorContainer,
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              Icons.Default.ErrorOutline,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
              text = cloudConfigsState.message,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
            )
          }
        }
      }
      is CloudUiState.Success -> {
        if (cloudConfigsState.data.isEmpty()) {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
          ) {
            Text(
              text = strings.cloudConfigsEmpty,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(14.dp),
            )
          }
        } else {
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            cloudConfigsState.data.forEach { cfg ->
              val isSelected = activePresetId == "cloud_${cfg.id}"
              Surface(
                onClick = { onLoadConfig(cfg) },
                shape = RoundedCornerShape(16.dp),
                color =
                  if (isSelected) MaterialTheme.colorScheme.primaryContainer
                  else MaterialTheme.colorScheme.surface,
                border =
                  BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                  ),
                modifier = Modifier.testTag("cloud_config_card_${cfg.id}"),
              ) {
                Row(
                  modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Column {
                    Text(
                      text = cfg.title,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                      text = "${cfg.keysCount} ${strings.keysLabel}",
                      fontFamily = JetBrainsMonoFontFamily,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  IconButton(
                    onClick = { onDeleteConfig(cfg.id) },
                    modifier =
                      Modifier.minimumInteractiveComponentSize()
                        .testTag("delete_cloud_config_${cfg.id}"),
                  ) {
                    Icon(
                      imageVector = Icons.Default.DeleteOutline,
                      contentDescription = "Delete ${cfg.title}",
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SaveCloudConfigDialog(
  strings: AppStrings,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var title by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(strings.saveConfigDialogTitle) },
    text = {
      OutlinedTextField(
        value = title,
        onValueChange = { title = it.take(80) },
        label = { Text(strings.configTitleLabel) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("cloud_config_title_input"),
      )
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(title.trim()) },
        enabled = title.trim().isNotEmpty(),
        modifier = Modifier.testTag("confirm_save_cloud_button"),
      ) {
        Text(strings.saveAction)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(strings.cancelAction) }
    },
  )
}

@Composable
private fun SettingNodeCard(
  node: SettingNode,
  onUpdate: (String, Any) -> Unit,
  onDelete: (String) -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth().testTag("setting_card_${node.path}"),
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      when (node) {
        is SettingNode.BooleanNode -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
              CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                  text = node.key,
                  fontFamily = JetBrainsMonoFontFamily,
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
              Text(
                text = if (node.value) "true" else "false",
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
              )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Switch(
                checked = node.value,
                onCheckedChange = { onUpdate(node.path, it) },
                modifier = Modifier.testTag("switch_${node.path}"),
              )
              IconButton(
                onClick = { onDelete(node.path) },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("delete_${node.path}"),
              ) {
                Icon(
                  Icons.Default.DeleteOutline,
                  contentDescription = "Delete ${node.key}",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
        is SettingNode.StringNode -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
              Text(
                text = node.key,
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
              )
            }
            IconButton(
              onClick = { onDelete(node.path) },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("delete_${node.path}"),
            ) {
              Icon(
                Icons.Default.DeleteOutline,
                contentDescription = "Delete ${node.key}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          if (node.suggestions.isNotEmpty()) {
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              node.suggestions.forEach { option ->
                FilterChip(
                  selected = node.value == option,
                  onClick = { onUpdate(node.path, option) },
                  label = {
                    Text(
                      text = option,
                      fontFamily = JetBrainsMonoFontFamily,
                      fontSize = 12.sp,
                    )
                  },
                  leadingIcon =
                    if (node.value == option) {
                      {
                        Icon(
                          Icons.Default.CheckCircle,
                          contentDescription = null,
                          modifier = Modifier.size(16.dp),
                        )
                      }
                    } else null,
                  modifier =
                    Modifier.minimumInteractiveComponentSize().testTag("chip_${node.path}_$option"),
                )
              }
            }
          } else {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
              OutlinedTextField(
                value = node.value,
                onValueChange = { onUpdate(node.path, it) },
                singleLine = true,
                textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 13.sp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("input_${node.path}"),
              )
            }
          }
        }
        is SettingNode.NumberNode -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
              Text(
                text = node.key,
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
              )
            }
            IconButton(
              onClick = { onDelete(node.path) },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("delete_${node.path}"),
            ) {
              Icon(
                Icons.Default.DeleteOutline,
                contentDescription = "Delete ${node.key}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            OutlinedTextField(
              value = node.value,
              onValueChange = { raw ->
                val num: Any = raw.toIntOrNull() ?: raw.toDoubleOrNull() ?: raw
                onUpdate(node.path, num)
              },
              singleLine = true,
              textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 13.sp),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().testTag("input_${node.path}"),
            )
          }
        }
        is SettingNode.GroupNode -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
              Text(
                text = "${node.key} { ${node.children.size} }",
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
              )
            }
            IconButton(
              onClick = { onDelete(node.path) },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("delete_${node.path}"),
            ) {
              Icon(
                Icons.Default.DeleteOutline,
                contentDescription = "Delete ${node.key}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          Column(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            node.children.forEach { child ->
              SettingNodeCard(node = child, onUpdate = onUpdate, onDelete = onDelete)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AddSettingDialog(
  strings: AppStrings,
  onDismiss: () -> Unit,
  onConfirm: (String, Any) -> Unit,
) {
  var keyName by remember { mutableStateOf("") }
  var selectedType by remember { mutableStateOf(0) } // 0 = String, 1 = Boolean, 2 = Number
  var textValue by remember { mutableStateOf("") }
  var boolValue by remember { mutableStateOf(true) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(strings.addKeyTitle) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
          OutlinedTextField(
            value = keyName,
            onValueChange = { keyName = it },
            label = { Text(strings.keyPathLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("new_key_name_input"),
          )
        }

        Text(text = strings.valueTypeLabel, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(strings.textType, strings.booleanType, strings.numberType).forEachIndexed {
            idx,
            label ->
            FilterChip(
              selected = selectedType == idx,
              onClick = { selectedType = idx },
              label = { Text(label) },
              modifier = Modifier.minimumInteractiveComponentSize(),
            )
          }
        }

        if (selectedType == 1) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(if (boolValue) "true" else "false", fontFamily = JetBrainsMonoFontFamily)
            Switch(checked = boolValue, onCheckedChange = { boolValue = it })
          }
        } else {
          CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            OutlinedTextField(
              value = textValue,
              onValueChange = { textValue = it },
              label = { Text(strings.valueLabel) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("new_key_value_input"),
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val parsedValue: Any =
            when (selectedType) {
              1 -> boolValue
              2 -> textValue.toIntOrNull() ?: textValue.toDoubleOrNull() ?: 0
              else -> textValue
            }
          onConfirm(keyName, parsedValue)
        },
        enabled = keyName.isNotBlank(),
        modifier = Modifier.testTag("confirm_add_key_button"),
      ) {
        Text(strings.saveAction)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(strings.cancelAction) }
    },
  )
}

private fun nodeMatchesQuery(node: SettingNode, query: String): Boolean {
  if (node.key.lowercase().contains(query) || node.path.lowercase().contains(query)) return true
  return when (node) {
    is SettingNode.BooleanNode -> node.value.toString().contains(query)
    is SettingNode.NumberNode -> node.value.lowercase().contains(query)
    is SettingNode.StringNode -> node.value.lowercase().contains(query)
    is SettingNode.GroupNode -> node.children.any { nodeMatchesQuery(it, query) }
  }
}
