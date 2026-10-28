package com.example

import android.os.Bundle
import android.app.PictureInPictureParams
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModelProvider
import com.example.ui.MediaApp
import com.example.ui.MediaViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val isInPipMode = mutableStateOf(false)
  private lateinit var viewModel: MediaViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handleIntent(intent)
    enableEdgeToEdge()
    viewModel = ViewModelProvider(this)[MediaViewModel::class.java]

    setContent {
      MyApplicationTheme {
        MediaApp(
          viewModel = viewModel,
          onExitApp = { finish() },
          isInPipMode = isInPipMode.value,
          onEnterPip = { enterPip() }
        )
      }
    }
  }

  fun enterPip() {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      val params = PictureInPictureParams.Builder()
        .setAspectRatio(Rational(16, 9))
        .build()
      enterPictureInPictureMode(params)
    } else {
      @Suppress("DEPRECATION")
      enterPictureInPictureMode()
    }
  }

  override fun onUserLeaveHint() {
    super.onUserLeaveHint()
    // PIP mode is only triggered explicitly when the user clicks the PIP button
  }

  override fun onPictureInPictureModeChanged(
    isInPictureInPictureMode: Boolean,
    newConfig: android.content.res.Configuration
  ) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    isInPipMode.value = isInPictureInPictureMode
  }

  override fun onStop() {
    super.onStop()
    if (!isInPipMode.value) {
      if (::viewModel.isInitialized) {
        viewModel.stopIptvAndMoviePlayback()
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    if (::viewModel.isInitialized) {
      viewModel.stopIptvAndMoviePlayback()
    }
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: android.content.Intent?) {
    if (intent?.getStringExtra("navigation_target") == "radio" || intent?.getBooleanExtra("focus_active_radio", false) == true) {
      com.example.service.PlaybackStateManager.notificationClickTrigger.value = true
    }
  }
}
