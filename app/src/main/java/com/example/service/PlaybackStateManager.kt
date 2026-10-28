package com.example.service

import com.example.models.RadioItem
import kotlinx.coroutines.flow.MutableStateFlow

object PlaybackStateManager {
    val activeRadio = MutableStateFlow<RadioItem?>(null)
    val radioList = MutableStateFlow<List<RadioItem>>(emptyList())
    val isPlaying = MutableStateFlow(false)
    val isRadioBuffering = MutableStateFlow(false)
    val isRadioActuallyPlaying = MutableStateFlow(false)
    val notificationClickTrigger = MutableStateFlow(false)

    // Callbacks from Service to ViewModel/UI
    var onPlayPauseToggle: (() -> Unit)? = null
    var onNext: (() -> Unit)? = null
    var onPrevious: (() -> Unit)? = null
    var onStop: (() -> Unit)? = null
}
