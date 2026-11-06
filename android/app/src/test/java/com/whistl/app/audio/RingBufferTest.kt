package com.whistl.app.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class RingBufferTest {
    @Test
    fun testRingBufferWriteAndSnapshot() {
        val buffer = RingBuffer(capacity = 5)
        val input1 = shortArrayOf(0, 16384, 32767)
        buffer.write(input1, 3)

        val snapshot1 = FloatArray(5)
        buffer.readNormalizedFloatSnapshot(snapshot1)

        assertEquals(0f, snapshot1[0], 0.001f)
        assertEquals(0f, snapshot1[1], 0.001f)
        assertEquals(0f, snapshot1[2], 0.001f)
        assertEquals(0.5f, snapshot1[3], 0.01f)
        assertEquals(1.0f, snapshot1[4], 0.01f)

        // Write more to trigger wrap-around
        val input2 = shortArrayOf(-32768, 0, 16384)
        buffer.write(input2, 3)

        val snapshot2 = FloatArray(5)
        buffer.readNormalizedFloatSnapshot(snapshot2)
        assertEquals(0.5f, snapshot2[0], 0.01f)
        assertEquals(1.0f, snapshot2[1], 0.01f)
        assertEquals(-1.0f, snapshot2[2], 0.01f)
        assertEquals(0.0f, snapshot2[3], 0.01f)
        assertEquals(0.5f, snapshot2[4], 0.01f)
    }

    @Test
    fun testRingBufferClear() {
        val buffer = RingBuffer(capacity = 5)
        buffer.write(shortArrayOf(1000, 2000, 3000), 3)
        buffer.clear()

        val snapshot = FloatArray(5)
        buffer.readNormalizedFloatSnapshot(snapshot)
        for (i in 0 until 5) {
            assertEquals(0f, snapshot[i], 0.001f)
        }
    }

    @Test
    fun testRingBufferOverflow() {
        val buffer = RingBuffer(capacity = 3)
        buffer.write(shortArrayOf(10, 20, 30, 40, 50), 5)
        val snapshot = FloatArray(3)
        buffer.readNormalizedFloatSnapshot(snapshot)
        assertEquals(30f / 32768.0f, snapshot[0], 0.0001f)
        assertEquals(40f / 32768.0f, snapshot[1], 0.0001f)
        assertEquals(50f / 32768.0f, snapshot[2], 0.0001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testSnapshotBufferMismatch() {
        val buffer = RingBuffer(capacity = 5)
        buffer.readNormalizedFloatSnapshot(FloatArray(3))
    }
}
