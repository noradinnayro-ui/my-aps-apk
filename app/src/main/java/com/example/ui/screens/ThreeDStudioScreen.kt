package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.AppStrings
import java.io.ByteArrayInputStream

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ThreeDStudioScreen(
  isFocusRunning: Boolean,
  isDarkTheme: Boolean,
  strings: AppStrings,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var isPageLoaded by remember { mutableStateOf(false) }

  val webView = remember {
    WebView(context).apply {
      setBackgroundColor(AndroidColor.TRANSPARENT)
      settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        allowFileAccess = false
        allowContentAccess = false
        builtInZoomControls = false
        displayZoomControls = false
      }
      webViewClient =
        object : WebViewClient() {
          override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?,
          ): WebResourceResponse? {
            val url = request?.url ?: return null
            if (url.host != "appassets.local") {
              return WebResourceResponse(
                "text/plain",
                "UTF-8",
                403,
                "Forbidden",
                emptyMap(),
                ByteArrayInputStream(ByteArray(0)),
              )
            }
            val path = url.path ?: return null
            if (!path.startsWith("/assets/")) return null
            val assetPath = path.removePrefix("/assets/")
            if (assetPath.contains("..")) return null

            val mimeType =
              when {
                assetPath.endsWith(".html") -> "text/html"
                assetPath.endsWith(".js") -> "application/javascript"
                assetPath.endsWith(".css") -> "text/css"
                assetPath.endsWith(".json") -> "application/json"
                else -> "application/octet-stream"
              }
            return try {
              val stream = context.assets.open(assetPath)
              WebResourceResponse(
                mimeType,
                "UTF-8",
                200,
                "OK",
                mapOf("Access-Control-Allow-Origin" to "*"),
                stream,
              )
            } catch (_: Exception) {
              null
            }
          }

          override fun onPageFinished(view: WebView?, url: String?) {
            isPageLoaded = true
          }
        }
      loadUrl("https://appassets.local/assets/threejs/index.html")
    }
  }

  DisposableEffect(webView) {
    onDispose {
      webView.stopLoading()
      webView.destroy()
    }
  }

  LaunchedEffect(isFocusRunning, isDarkTheme, isPageLoaded) {
    if (isPageLoaded) {
      webView.evaluateJavascript(
        "window.setAppState && window.setAppState({isFocusRunning:$isFocusRunning, darkTheme:$isDarkTheme});",
        null,
      )
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    AndroidView(
      factory = { webView },
      modifier = Modifier.fillMaxSize().testTag("studio_3d_webview"),
    )

    // Minimal unobtrusive status chip in top corner
    val badgeColor =
      if (isFocusRunning) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    Surface(
      modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
      shape = RoundedCornerShape(999.dp),
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
      border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f)),
      tonalElevation = 2.dp,
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Default.ViewInAr,
          contentDescription = strings.nav3d,
          tint = badgeColor,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = if (isFocusRunning) strings.focusActiveBadge else strings.focusIdleBadge,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}
