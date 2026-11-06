package com.whistl.app.ml

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

data class DetectionResult(
    val backgroundProb: Float,
    val whistleProb: Float
)

class WhistleDetector(
    private val modelBuffer: ByteBuffer
) : AutoCloseable {

    private val interpreter: Interpreter

    init {
        val options = Interpreter.Options().apply {
            setNumThreads(2)
        }
        interpreter = Interpreter(modelBuffer, options)
        interpreter.allocateTensors()
    }

    constructor(context: Context, modelAssetPath: String = "whistle_detector_end_to_end.tflite") : this(
        loadModelFile(context, modelAssetPath)
    )

    fun detect(waveform: FloatArray): DetectionResult {
        require(waveform.size == 15600) { "Input waveform must be exactly 15600 samples, got ${waveform.size}" }

        val output = Array(1) { FloatArray(2) }
        interpreter.run(waveform, output)

        val probs = output[0]
        return DetectionResult(
            backgroundProb = probs[0],
            whistleProb = probs[1]
        )
    }

    override fun close() {
        interpreter.close()
    }

    companion object {
        fun loadModelFile(context: Context, assetPath: String): ByteBuffer {
            val fileDescriptor = context.assets.openFd(assetPath)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength).apply {
                order(ByteOrder.nativeOrder())
            }
        }
    }
}
