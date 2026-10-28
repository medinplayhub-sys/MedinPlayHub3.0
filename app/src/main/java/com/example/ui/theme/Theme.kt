package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFFD1C4B9),       // Sophisticated Sand
    secondary = Color(0xFFA09994),     // Stone Grey
    tertiary = Color(0xFFC67C65),      // Terracotta Accent
    background = Color(0xFF0F0D0B),    // Deep Rich Dark Charcoal
    surface = Color(0xFF24211E),       // Sophisticated Dark Card Background
    onPrimary = Color(0xFF0F0D0B),
    onSecondary = Color(0xFF0F0D0B),
    onBackground = Color(0xFFE6E1DE),  // Warm Oyster White
    onSurface = Color(0xFFE6E1DE),
    outline = Color(0xFF3D3834)        // Warm Border/Outline
  )

private val LightColorScheme = DarkColorScheme // Always maintain the sophisticated dark aesthetic

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force Dark theme
  dynamicColor: Boolean = false, // Disable dynamic colors to keep brand fidelity
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val view = androidx.compose.ui.platform.LocalView.current
  if (!view.isInEditMode) {
    androidx.compose.runtime.SideEffect {
      val window = (view.context as? android.app.Activity)?.window
      window?.statusBarColor = android.graphics.Color.parseColor("#888888")
    }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
