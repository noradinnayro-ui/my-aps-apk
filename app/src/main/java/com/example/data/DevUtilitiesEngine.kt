package com.example.data

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

object DevUtilitiesEngine {

  private val secureRandom = SecureRandom()

  fun formatJson(input: String, indentSpaces: Int): Result<String> =
    runCatching {
      val trimmed = input.trim()
      require(trimmed.isNotEmpty()) { "JSON input is empty" }
      when (val parsed = JSONTokener(trimmed).nextValue()) {
        is JSONObject -> parsed.toString(indentSpaces)
        is JSONArray -> parsed.toString(indentSpaces)
        else -> throw IllegalArgumentException("Root element must be a JSON object or array")
      }
    }

  fun minifyJson(input: String): Result<String> =
    runCatching {
      val trimmed = input.trim()
      require(trimmed.isNotEmpty()) { "JSON input is empty" }
      when (val parsed = JSONTokener(trimmed).nextValue()) {
        is JSONObject -> parsed.toString()
        is JSONArray -> parsed.toString()
        else -> throw IllegalArgumentException("Root element must be a JSON object or array")
      }
    }

  fun sortJsonKeys(input: String, indentSpaces: Int = 4): Result<String> =
    runCatching {
      val trimmed = input.trim()
      require(trimmed.isNotEmpty()) { "JSON input is empty" }
      when (val parsed = JSONTokener(trimmed).nextValue()) {
        is JSONObject -> sortJsonObject(parsed).toString(indentSpaces)
        is JSONArray -> sortJsonArray(parsed).toString(indentSpaces)
        else -> throw IllegalArgumentException("Root element must be a JSON object or array")
      }
    }

  private fun sortJsonObject(obj: JSONObject): JSONObject {
    val sorted = JSONObject()
    val keys = mutableListOf<String>()
    val iter = obj.keys()
    while (iter.hasNext()) keys.add(iter.next())
    keys.sort()
    for (key in keys) {
      val value = obj.get(key)
      sorted.put(
        key,
        when (value) {
          is JSONObject -> sortJsonObject(value)
          is JSONArray -> sortJsonArray(value)
          else -> value
        },
      )
    }
    return sorted
  }

  private fun sortJsonArray(arr: JSONArray): JSONArray {
    val result = JSONArray()
    for (i in 0 until arr.length()) {
      val item = arr.get(i)
      result.put(
        when (item) {
          is JSONObject -> sortJsonObject(item)
          is JSONArray -> sortJsonArray(item)
          else -> item
        }
      )
    }
    return result
  }

  fun encodeBase64(input: String): String =
    Base64.getEncoder().encodeToString(input.toByteArray(StandardCharsets.UTF_8))

  fun decodeBase64(input: String): Result<String> =
    runCatching {
      val bytes = Base64.getDecoder().decode(input.trim())
      String(bytes, StandardCharsets.UTF_8)
    }

  fun encodeUrl(input: String): String =
    URLEncoder.encode(input, StandardCharsets.UTF_8.name())

  fun decodeUrl(input: String): Result<String> =
    runCatching { URLDecoder.decode(input, StandardCharsets.UTF_8.name()) }

  fun generateUuid(): String = UUID.randomUUID().toString()

  fun generateSecureToken(
    length: Int,
    includeUpper: Boolean,
    includeLower: Boolean,
    includeDigits: Boolean,
    includeSymbols: Boolean,
  ): String {
    val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val lower = "abcdefghijklmnopqrstuvwxyz"
    val digits = "0123456789"
    val symbols = "!@#\$%^&*_-+=?"
    val pool = buildString {
      if (includeUpper) append(upper)
      if (includeLower) append(lower)
      if (includeDigits) append(digits)
      if (includeSymbols) append(symbols)
    }.ifEmpty { lower + digits }

    val safeLen = length.coerceIn(8, 64)
    return buildString(safeLen) {
      repeat(safeLen) {
        append(pool[secureRandom.nextInt(pool.length)])
      }
    }
  }

  fun rgbToHex(r: Int, g: Int, b: Int): String =
    String.format("#%02X%02X%02X", r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))

  fun relativeLuminance(r: Int, g: Int, b: Int): Double {
    fun channel(c: Int): Double {
      val s = c.coerceIn(0, 255) / 255.0
      return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
  }

  fun contrastRatio(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Double {
    val l1 = relativeLuminance(r1, g1, b1)
    val l2 = relativeLuminance(r2, g2, b2)
    val lighter = max(l1, l2)
    val darker = min(l1, l2)
    return (lighter + 0.05) / (darker + 0.05)
  }
}
