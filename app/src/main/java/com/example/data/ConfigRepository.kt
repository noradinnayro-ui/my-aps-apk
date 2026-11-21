package com.example.data

import android.content.Context
import com.example.model.ConfigPreset
import com.example.model.SettingNode
import org.json.JSONArray
import org.json.JSONObject

class ConfigRepository(private val context: Context? = null) {

  companion object {
    const val ASSET_FILE_NAME = "settings_dd06cfbb.json"

    val FALLBACK_SETTINGS_JSON =
      """
      {
          "editor.gotoLocation.alternativeDeclarationCommand": "editor.action.revealDefinition",
          "editor.gotoLocation.alternativeDefinitionCommand": "editor.action.revealDefinition",
          "editor.gotoLocation.alternativeTypeDefinitionCommand": "editor.action.revealDefinition",
          "editor.selectionHighlight": false,
          "files.autoSave": "onFocusChange",
          "editor.suggest.snippetsPreventQuickSuggestions": false,
          "editor.quickSuggestions": {
              "other": "on",
              "comments": "off",
              "strings": "on"
          }
      }
      """
        .trimIndent()
  }

  fun loadAttachedSettingsJson(): String {
    val ctx = context ?: return FALLBACK_SETTINGS_JSON
    return runCatching {
        ctx.assets.open(ASSET_FILE_NAME).bufferedReader().use { it.readText() }.trim()
      }
      .getOrDefault(FALLBACK_SETTINGS_JSON)
  }

  fun getPresets(): List<ConfigPreset> {
    val attached = loadAttachedSettingsJson()
    return listOf(
      ConfigPreset(
        id = "attached",
        titleAr = "ملفك المرفق",
        titleEn = "Attached Config",
        subtitleAr = "settings_dd06cfbb.json الأصلي",
        subtitleEn = "Original settings_dd06cfbb.json",
        jsonContent = attached,
      ),
      ConfigPreset(
        id = "deep_focus",
        titleAr = "وضع التركيز الهادئ",
        titleEn = "Zen Deep Focus",
        subtitleAr = "تقليل المشتتات وحفظ تلقائي فوري",
        subtitleEn = "Minimal distractions & instant auto-save",
        jsonContent =
          """
          {
              "editor.gotoLocation.alternativeDeclarationCommand": "editor.action.revealDefinition",
              "editor.gotoLocation.alternativeDefinitionCommand": "editor.action.revealDefinition",
              "editor.selectionHighlight": false,
              "editor.minimap.enabled": false,
              "editor.lineNumbers": "relative",
              "files.autoSave": "afterDelay",
              "files.autoSaveDelay": 800,
              "editor.suggest.snippetsPreventQuickSuggestions": true,
              "editor.quickSuggestions": {
                  "other": "inline",
                  "comments": "off",
                  "strings": "off"
              }
          }
          """
            .trimIndent(),
      ),
      ConfigPreset(
        id = "fast_dev",
        titleAr = "سرعة الإنتاجية",
        titleEn = "Rapid Coding",
        subtitleAr = "إكمال تلقائي شامل وتنسيق عند الحفظ",
        subtitleEn = "Full autocomplete & format on save",
        jsonContent =
          """
          {
              "editor.gotoLocation.alternativeDeclarationCommand": "editor.action.revealDefinition",
              "editor.gotoLocation.alternativeDefinitionCommand": "editor.action.revealDefinition",
              "editor.gotoLocation.alternativeTypeDefinitionCommand": "editor.action.revealDefinition",
              "editor.selectionHighlight": true,
              "editor.formatOnSave": true,
              "editor.tabSize": 2,
              "files.autoSave": "onFocusChange",
              "editor.suggest.snippetsPreventQuickSuggestions": false,
              "editor.quickSuggestions": {
                  "other": "on",
                  "comments": "on",
                  "strings": "on"
              }
          }
          """
            .trimIndent(),
      ),
    )
  }

  fun parseConfigNodes(jsonText: String): Result<List<SettingNode>> =
    runCatching {
      val root = JSONObject(jsonText)
      parseObject(root, prefix = "")
    }

  private fun parseObject(obj: JSONObject, prefix: String): List<SettingNode> {
    val result = mutableListOf<SettingNode>()
    val keys = obj.keys()
    while (keys.hasNext()) {
      val key = keys.next()
      val path = if (prefix.isEmpty()) key else "$prefix/$key"
      val value = obj.get(key)
      when (value) {
        is Boolean -> result.add(SettingNode.BooleanNode(path = path, key = key, value = value))
        is Number ->
          result.add(SettingNode.NumberNode(path = path, key = key, value = value.toString()))
        is String ->
          result.add(
            SettingNode.StringNode(
              path = path,
              key = key,
              value = value,
              suggestions = knownSuggestionsFor(key, path),
            )
          )
        is JSONObject ->
          result.add(
            SettingNode.GroupNode(
              path = path,
              key = key,
              children = parseObject(value, prefix = path),
            )
          )
        is JSONArray ->
          result.add(
            SettingNode.StringNode(
              path = path,
              key = key,
              value = value.toString(),
              suggestions = emptyList(),
            )
          )
      }
    }
    return result
  }

  fun updateNodeValue(jsonText: String, path: String, newValue: Any): Result<String> =
    runCatching {
      val root = JSONObject(jsonText)
      val segments = path.split("/")
      var current = root
      for (i in 0 until segments.lastIndex) {
        val seg = segments[i]
        current = current.getJSONObject(seg)
      }
      current.put(segments.last(), newValue)
      root.toString(4)
    }

  fun addTopLevelKey(jsonText: String, key: String, value: Any): Result<String> =
    runCatching {
      val root = JSONObject(jsonText)
      root.put(key.trim(), value)
      root.toString(4)
    }

  fun deleteNodeAtPath(jsonText: String, path: String): Result<String> =
    runCatching {
      val root = JSONObject(jsonText)
      val segments = path.split("/")
      var current = root
      for (i in 0 until segments.lastIndex) {
        current = current.getJSONObject(segments[i])
      }
      current.remove(segments.last())
      root.toString(4)
    }

  private fun knownSuggestionsFor(key: String, path: String): List<String> {
    return when {
      key == "files.autoSave" -> listOf("off", "afterDelay", "onFocusChange", "onWindowChange")
      key.startsWith("editor.gotoLocation") ->
        listOf("peek", "gotoAndPeek", "goto", "editor.action.revealDefinition")
      path.startsWith("editor.quickSuggestions/") -> listOf("on", "off", "inline")
      key == "editor.lineNumbers" -> listOf("on", "off", "relative", "interval")
      else -> emptyList()
    }
  }
}
