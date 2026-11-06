package com.whistl.app.audio

class RingBuffer(val capacity: Int) {
    private val buffer = ShortArray(capacity)
    private var writeIndex = 0
    private var count = 0
    private val lock = Any()

    fun write(samples: ShortArray, length: Int) {
        synchronized(lock) {
            for (i in 0 until length) {
                buffer[writeIndex] = samples[i]
                writeIndex = (writeIndex + 1) % capacity
                if (count < capacity) count++
            }
        }
    }

    fun readNormalizedFloatSnapshot(outBuffer: FloatArray) {
        require(outBuffer.size == capacity) { "Output buffer must match capacity $capacity" }
        synchronized(lock) {
            val start = if (count < capacity) 0 else writeIndex
            val offset = capacity - count
            for (i in 0 until offset) {
                outBuffer[i] = 0f
            }
            for (i in 0 until count) {
                val idx = (start + i) % capacity
                outBuffer[offset + i] = buffer[idx] / 32768.0f
            }
        }
    }

    fun clear() {
        synchronized(lock) {
            buffer.fill(0)
            writeIndex = 0
            count = 0
        }
    }
}
