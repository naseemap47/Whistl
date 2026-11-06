package com.whistl.app.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WhistleStateMachineTest {

    @Test
    fun testRejectsShortTransientNoise() {
        val sm = WhistleStateMachine(threshold = 0.75f, minDurationMs = 1000L, cooldownMs = 4000L)
        // Spike for 400ms then back to silence
        assertNull(sm.process(confidence = 0.90f, currentTimeMs = 0L))
        assertEquals(WhistleState.POSSIBLE, sm.currentState)
        assertNull(sm.process(confidence = 0.90f, currentTimeMs = 400L))
        assertNull(sm.process(confidence = 0.20f, currentTimeMs = 600L))
        assertEquals(WhistleState.IDLE, sm.currentState)
        assertEquals(0, sm.whistleCount)
    }

    @Test
    fun testConfirmsSustainedWhistleAndEnforcesCooldown() {
        val sm = WhistleStateMachine(threshold = 0.75f, minDurationMs = 1000L, cooldownMs = 4000L)
        // Sustained whistle for 1.2s
        assertNull(sm.process(0.85f, currentTimeMs = 1000L))
        assertNull(sm.process(0.88f, currentTimeMs = 1500L))
        val event = sm.process(0.92f, currentTimeMs = 2100L)

        assertNotNull(event)
        assertEquals(1, sm.whistleCount)
        assertEquals(WhistleState.COOLDOWN, sm.currentState)

        // Whistle continues blowing for 2 more seconds - should NOT count again
        assertNull(sm.process(0.90f, currentTimeMs = 3000L))
        assertEquals(1, sm.whistleCount)

        // Cooker stops at 4500ms
        assertNull(sm.process(0.15f, currentTimeMs = 4500L))
        // Still in cooldown (< 4000ms after count)
        assertNull(sm.process(0.15f, currentTimeMs = 6000L))
        assertEquals(WhistleState.COOLDOWN, sm.currentState)

        // Cooldown expires: 2100ms (count) + 4000ms cooldown = 6100ms
        assertNull(sm.process(0.15f, currentTimeMs = 6200L))
        assertEquals(WhistleState.IDLE, sm.currentState)

        // Next whistle at 7000ms
        assertNull(sm.process(0.85f, currentTimeMs = 7000L))
        val event2 = sm.process(0.88f, currentTimeMs = 8200L)
        assertNotNull(event2)
        assertEquals(2, sm.whistleCount)
    }

    @Test
    fun testResetClearsState() {
        val sm = WhistleStateMachine()
        sm.process(0.90f, currentTimeMs = 0L)
        sm.process(0.90f, currentTimeMs = 1200L) // confirms
        assertEquals(1, sm.whistleCount)
        sm.reset()
        assertEquals(0, sm.whistleCount)
        assertEquals(WhistleState.IDLE, sm.currentState)
    }

    @Test
    fun testEventContainsCorrectData() {
        val sm = WhistleStateMachine(threshold = 0.75f, minDurationMs = 1000L, cooldownMs = 4000L)
        sm.process(0.80f, currentTimeMs = 0L)
        val event = sm.process(0.95f, currentTimeMs = 1100L)
        assertNotNull(event)
        assertEquals(1, event!!.whistleNumber)
        assertEquals(1100L, event.timestampMs)
        assertEquals(0.95f, event.peakConfidence, 0.001f)
    }
}
