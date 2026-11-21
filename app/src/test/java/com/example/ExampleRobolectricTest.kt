package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ConfigRepository
import com.example.data.DevUtilitiesEngine
import com.example.model.SettingNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("DevCraft", appName)
  }

  @Test
  fun `bundled settings json loads and parses cleanly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = ConfigRepository(context)
    val raw = repo.loadAttachedSettingsJson()
    val parsed = repo.parseConfigNodes(raw)
    assertTrue(parsed.isSuccess)
    val nodes = parsed.getOrThrow()
    assertTrue(nodes.any { it.key == "files.autoSave" })
    assertTrue(nodes.any { it is SettingNode.GroupNode && it.key == "editor.quickSuggestions" })
  }

  @Test
  fun `utilities engine formats json and encodes base64 accurately`() {
    val minified = DevUtilitiesEngine.minifyJson("""{"b": 2, "a": 1}""").getOrThrow()
    assertEquals("""{"b":2,"a":1}""", minified)

    val sorted = DevUtilitiesEngine.sortJsonKeys("""{"b": 2, "a": 1}""", 2).getOrThrow()
    assertTrue(sorted.indexOf("\"a\"") < sorted.indexOf("\"b\""))

    val encoded = DevUtilitiesEngine.encodeBase64("DevCraft")
    assertEquals("DevCraft", DevUtilitiesEngine.decodeBase64(encoded).getOrThrow())

    val contrast = DevUtilitiesEngine.contrastRatio(255, 255, 255, 15, 23, 42)
    assertTrue(contrast > 15.0)
  }
}
