package com.whistl.app.service

import com.whistl.app.state.WhistleEvent
import com.whistl.app.state.WhistleState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSessionState(
    val isListening: Boolean = false,
    val currentCount: Int = 0,
    val targetCount: Int = 3,
    val whistleState: WhistleState = WhistleState.IDLE,
    val currentConfidence: Float = 0f,
    val isTargetReached: Boolean = false,
    val whistleHistory: List<WhistleEvent> = emptyList()
)

object AppSessionRepository {
    private val _sessionState = MutableStateFlow(AppSessionState())
    val sessionState: StateFlow<AppSessionState> = _sessionState.asStateFlow()

    fun updateState(transform: (AppSessionState) -> AppSessionState) {
        _sessionState.value = transform(_sessionState.value)
    }

    fun resetSession() {
        _sessionState.value = _sessionState.value.copy(
            currentCount = 0,
            whistleState = WhistleState.IDLE,
            currentConfidence = 0f,
            isTargetReached = false,
            whistleHistory = emptyList()
        )
    }
}
