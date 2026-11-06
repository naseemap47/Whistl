package com.whistl.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.util.concurrent.atomic.AtomicBoolean

class AudioRecorderSource(
    private val sampleRate: Int = 16000,
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
) {
    private val isRecording = AtomicBoolean(false)
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null

    @SuppressLint("MissingPermission")
    fun start(onSamplesRead: (ShortArray, Int) -> Unit) {
        if (isRecording.getAndSet(true)) return

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = maxOf(minBufferSize, sampleRate / 10 * 2) // ~100ms chunks
        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )
        audioRecord = record

        record.startRecording()
        recordingThread = Thread {
            val chunk = ShortArray(1600) // 100ms at 16kHz
            while (isRecording.get()) {
                val read = record.read(chunk, 0, chunk.size)
                if (read > 0) {
                    onSamplesRead(chunk, read)
                }
            }
        }.apply { start() }
    }

    fun stop() {
        if (!isRecording.getAndSet(false)) return
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        recordingThread?.join(500)
        recordingThread = null
    }
}
