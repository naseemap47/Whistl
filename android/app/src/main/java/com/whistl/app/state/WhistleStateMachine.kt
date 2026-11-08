package com.whistl.app.state

enum class WhistleState {
    IDLE,
    POSSIBLE,
    CONFIRMED,
    COOLDOWN
}

data class WhistleEvent(
    val whistleNumber: Int,
    val timestampMs: Long,
    val peakConfidence: Float
)

class WhistleStateMachine(
    private val threshold: Float = 0.75f,
    private val minDurationMs: Long = 1000L,
    private val cooldownMs: Long = 4000L,
    private val cooldownExitThreshold: Float = 0.35f
) {
    var currentState: WhistleState = WhistleState.IDLE
        private set

    var whistleCount: Int = 0
        private set

    private var whistleStartTimeMs: Long = 0L
    private var cooldownStartTimeMs: Long = 0L
    private var peakConfidence: Float = 0f

    fun process(confidence: Float, currentTimeMs: Long): WhistleEvent? {
        when (currentState) {
            WhistleState.IDLE -> {
                if (confidence >= threshold) {
                    currentState = WhistleState.POSSIBLE
                    whistleStartTimeMs = currentTimeMs
                    peakConfidence = confidence
                }
            }
            WhistleState.POSSIBLE -> {
                if (confidence < threshold - 0.10f) {
                    // Fell below threshold prematurely — transient noise, discard
                    currentState = WhistleState.IDLE
                    peakConfidence = 0f
                } else {
                    peakConfidence = maxOf(peakConfidence, confidence)
                    if (currentTimeMs - whistleStartTimeMs >= minDurationMs) {
                        whistleCount++
                        cooldownStartTimeMs = currentTimeMs
                        currentState = WhistleState.COOLDOWN
                        return WhistleEvent(
                            whistleNumber = whistleCount,
                            timestampMs = currentTimeMs,
                            peakConfidence = peakConfidence
                        )
                    }
                }
            }
            WhistleState.CONFIRMED -> {
                // Direct transition fallback (unused in current flow but kept for safety)
                currentState = WhistleState.COOLDOWN
                cooldownStartTimeMs = currentTimeMs
            }
            WhistleState.COOLDOWN -> {
                // Exit cooldown only after both time and confidence conditions are met
                if (currentTimeMs - cooldownStartTimeMs >= cooldownMs && confidence <= cooldownExitThreshold) {
                    currentState = WhistleState.IDLE
                    peakConfidence = 0f
                }
            }
        }
        return null
    }

    fun reset(initialCount: Int = 0) {
        currentState = WhistleState.IDLE
        whistleCount = initialCount
        whistleStartTimeMs = 0L
        cooldownStartTimeMs = 0L
        peakConfidence = 0f
    }
}
