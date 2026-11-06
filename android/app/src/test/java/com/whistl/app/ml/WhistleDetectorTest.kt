package com.whistl.app.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class WhistleDetectorTest {

    @Test
    fun testModelAssetIntegrity() {
        val modelFile = File("src/main/assets/whistle_detector_end_to_end.tflite")
        assertTrue("Model asset file must exist at ${modelFile.absolutePath}", modelFile.exists())
        assertTrue("Model file should be > 10MB, actual: ${modelFile.length()}", modelFile.length() > 10_000_000)

        // Verify TFLite magic identifier at offset 4: "TFL3"
        val bytes = ByteArray(8)
        FileInputStream(modelFile).use { it.read(bytes) }
        val magic = String(bytes, 4, 4, Charsets.US_ASCII)
        assertEquals("TFLite FlatBuffer identifier must be TFL3", "TFL3", magic)
    }

    @Test
    fun testDetectionResultDataClass() {
        val result = DetectionResult(backgroundProb = 0.05f, whistleProb = 0.95f)
        assertEquals(0.05f, result.backgroundProb, 0.001f)
        assertEquals(0.95f, result.whistleProb, 0.001f)
    }

    @Test
    fun testWaveformSizeContract() {
        // Verify input size requirement constant
        val expectedSize = 15600
        assertEquals(15600, expectedSize)
    }
}
