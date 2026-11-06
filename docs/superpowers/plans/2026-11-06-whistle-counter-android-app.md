# Whistle Counter Android App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a native Kotlin Android application that uses an on-device TensorFlow Lite model to reliably detect, debounce, and count pressure cooker whistles in the background, alerting the user with an alarm when the target whistle count is reached.

**Architecture:** Android Foreground Service with microphone wake-lock capturing 16kHz audio into a ring buffer. A unified YAMNet+Classifier TFLite model runs inference every 400ms. A debouncing state machine validates sustained sound (>= 1.0s) and enforces a 4.0s cooldown before incrementing count. Modern Jetpack Compose Material 3 UI observes state flows.

**Tech Stack:** Kotlin 2.0, Android SDK API 34+, Jetpack Compose, Material 3, Coroutines/Flow, TensorFlow Lite (`org.tensorflow:tensorflow-lite:2.16.1`).

**Spec:** `docs/superpowers/specs/2026-11-06-whistle-counter-android-app-design.md`

## Global Constraints
- Android SDK directory: `/home/naseem/Android/Sdk`
- Android App directory: `/home/naseem/Projects/Whistl/android`
- Package: `com.whistl.app`
- Min SDK: 26 (Android 8.0), Target SDK: 34
- No placeholder code or unverified dependencies.

---

### Task 1: End-to-End TFLite Model Export

**Files:**
- Create: `export_end_to_end_tflite.py`
- Test: `test_exported_tflite.py`
- Output: `whistle_detector_end_to_end.tflite`

**Interfaces:**
- Produces: `whistle_detector_end_to_end.tflite`
  - Input: `waveform` `float32[15600]`
  - Output: `probabilities` `float32[2]` (`[prob_background, prob_whistle]`)

- [ ] **Step 1: Write model export script**

```python
# export_end_to_end_tflite.py
import tensorflow as tf
import tensorflow_hub as hub

def export_model():
    print("Loading YAMNet feature extractor...")
    yamnet = hub.load('https://tfhub.dev/google/yamnet/1')
    print("Loading custom whistle classifier head...")
    classifier = tf.keras.models.load_model('whistle_classifier.h5')

    class EndToEndWhistleModel(tf.Module):
        def __init__(self, yamnet_model, classifier_model):
            super().__init__()
            self.yamnet = yamnet_model
            self.classifier = classifier_model

        @tf.function(input_signature=[tf.TensorSpec(shape=[15600], dtype=tf.float32, name='waveform')])
        def __call__(self, waveform):
            _, embeddings, _ = self.yamnet(waveform)
            mean_embedding = tf.reduce_mean(embeddings, axis=0, keepdims=True)
            probs = self.classifier(mean_embedding)
            return {'probabilities': probs[0]}

    e2e = EndToEndWhistleModel(yamnet, classifier)
    concrete_fn = e2e.__call__.get_concrete_function()
    converter = tf.lite.TFLiteConverter.from_concrete_functions([concrete_fn])
    tflite_model = converter.convert()

    output_path = 'whistle_detector_end_to_end.tflite'
    with open(output_path, 'wb') as f:
        f.write(tflite_model)
    print(f"Exported end-to-end model to {output_path} ({len(tflite_model)} bytes)")

if __name__ == '__main__':
    export_model()
```

- [ ] **Step 2: Write test script to verify model predictions**

```python
# test_exported_tflite.py
import tensorflow as tf
import numpy as np
import librosa
import glob

def test_exported_model():
    interpreter = tf.lite.Interpreter(model_path='whistle_detector_end_to_end.tflite')
    interpreter.allocate_tensors()
    inputs = interpreter.get_input_details()
    outputs = interpreter.get_output_details()

    whistle_files = glob.glob('dataset/whistle/*.wav') + glob.glob('dataset_norm/**/whistle/*.wav', recursive=True)
    assert len(whistle_files) > 0, "No whistle files found for testing"
    
    waveform, sr = librosa.load(whistle_files[0], sr=16000, mono=True)
    waveform = np.pad(waveform, (0, max(0, 15600 - len(waveform))))[:15600].astype(np.float32)

    interpreter.set_tensor(inputs[0]['index'], waveform)
    interpreter.invoke()
    preds = interpreter.get_tensor(outputs[0]['index'])
    
    whistle_prob = float(preds[1])
    print(f"Tested {whistle_files[0]} -> whistle confidence: {whistle_prob:.4f}")
    assert whistle_prob > 0.85, f"Expected whistle prob > 0.85, got {whistle_prob}"
    print("Verification PASSED!")

if __name__ == '__main__':
    test_exported_model()
```

- [ ] **Step 3: Run export and verification**

Run: `/home/naseem/Projects/Whistl/.venv/bin/python export_end_to_end_tflite.py && /home/naseem/Projects/Whistl/.venv/bin/python test_exported_tflite.py`  
Expected: `Verification PASSED!`

- [ ] **Step 4: Commit**

```bash
git add export_end_to_end_tflite.py test_exported_tflite.py
git commit -m "feat(ml): add end-to-end tflite model exporter and verification test"
```

---

### Task 2: Android Project Scaffolding & Gradle Build Setup

**Files:**
- Create: `android/settings.gradle.kts`
- Create: `android/build.gradle.kts`
- Create: `android/gradle.properties`
- Create: `android/local.properties`
- Create: `android/app/build.gradle.kts`
- Create: `android/app/src/main/assets/whistle_detector_end_to_end.tflite`
- Create: `android/gradle/wrapper/gradle-wrapper.properties`

**Interfaces:**
- Produces: Build system able to compile Kotlin 2.0 with Jetpack Compose & TensorFlow Lite.

- [ ] **Step 1: Create Android project directory structure and Gradle configs**

Create `android/settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "Whistl"
include(":app")
```

Create `android/build.gradle.kts`:
```kotlin
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
}
```

Create `android/gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
```

Create `android/local.properties`:
```properties
sdk.dir=/home/naseem/Android/Sdk
```

Create `android/gradle/wrapper/gradle-wrapper.properties`:
```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

Create `android/app/build.gradle.kts`:
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.whistl.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.whistl.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    aaptOptions {
        noCompress("tflite")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.3")

    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
```

- [ ] **Step 2: Copy model asset to Android assets directory**

Copy `whistle_detector_end_to_end.tflite` to `android/app/src/main/assets/whistle_detector_end_to_end.tflite`.

- [ ] **Step 3: Setup gradle wrapper executable and verify build resolution**

Run:
```bash
cd /home/naseem/Projects/Whistl/android && gradle wrapper --gradle-version 8.7 || cp -r /home/naseem/.gradle/wrapper/dists/gradle-9.2.0-bin/*/gradle-9.2.0/bin/gradle .gradle-bin
```
Verify:
```bash
cd /home/naseem/Projects/Whistl/android && ./gradlew tasks --no-daemon
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add android/
git commit -m "chore(android): scaffold android project with Gradle and TFLite asset"
```

---

### Task 3: Audio RingBuffer & Audio Capture Engine

**Files:**
- Create: `android/app/src/main/java/com/whistl/app/audio/RingBuffer.kt`
- Create: `android/app/src/main/java/com/whistl/app/audio/AudioRecorderSource.kt`
- Test: `android/app/src/test/java/com/whistl/app/audio/RingBufferTest.kt`

**Interfaces:**
- Produces: `RingBuffer(capacity: Int)`:
  - `fun write(samples: ShortArray, length: Int)`
  - `fun readNormalizedFloatSnapshot(outBuffer: FloatArray)`
  - `fun clear()`
- Produces: `AudioRecorderSource(sampleRate: Int = 16000)`:
  - `fun start(onSamplesRead: (ShortArray, Int) -> Unit)`
  - `fun stop()`

- [ ] **Step 1: Write failing unit test for `RingBuffer`**

```kotlin
// android/app/src/test/java/com/whistl/app/audio/RingBufferTest.kt
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
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /home/naseem/Projects/Whistl/android && ./gradlew :app:testDebugUnitTest --tests com.whistl.app.audio.RingBufferTest`  
Expected: FAIL (class not found)

- [ ] **Step 3: Implement `RingBuffer` and `AudioRecorderSource`**

```kotlin
// android/app/src/main/java/com/whistl/app/audio/RingBuffer.kt
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
```

Implement `AudioRecorderSource.kt`:
```kotlin
// android/app/src/main/java/com/whistl/app/audio/AudioRecorderSource.kt
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
```

- [ ] **Step 4: Run unit tests to verify they pass**

Run: `cd /home/naseem/Projects/Whistl/android && ./gradlew :app:testDebugUnitTest --tests com.whistl.app.audio.RingBufferTest`  
Expected: BUILD SUCCESSFUL, tests pass.

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/java/com/whistl/app/audio/ android/app/src/test/java/com/whistl/app/audio/
git commit -m "feat(audio): implement thread-safe RingBuffer and AudioRecorderSource"
```

---

### Task 4: Whistle Detection State Machine & Debounce Logic

**Files:**
- Create: `android/app/src/main/java/com/whistl/app/state/WhistleStateMachine.kt`
- Test: `android/app/src/test/java/com/whistl/app/state/WhistleStateMachineTest.kt`

**Interfaces:**
- Produces: `enum class WhistleState { IDLE, POSSIBLE, CONFIRMED, COOLDOWN }`
- Produces: `class WhistleStateMachine(threshold: Float = 0.75f, minDurationMs: Long = 1000L, cooldownMs: Long = 4000L)`:
  - `fun process(confidence: Float, currentTimeMs: Long): WhistleEvent?`
  - `fun reset()`
  - `val currentState: WhistleState`
  - `val whistleCount: Int`

- [ ] **Step 1: Write failing unit tests for `WhistleStateMachine`**

```kotlin
// android/app/src/test/java/com/whistl/app/state/WhistleStateMachineTest.kt
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
        // Still in cooldown period (< 4000ms after onset/quiet)
        assertNull(sm.process(0.15f, currentTimeMs = 6000L))
        assertEquals(WhistleState.COOLDOWN, sm.currentState)

        // Cooldown expires at 8600ms
        assertNull(sm.process(0.15f, currentTimeMs = 8600L))
        assertEquals(WhistleState.IDLE, sm.currentState)

        // Next whistle at 9000ms
        assertNull(sm.process(0.85f, currentTimeMs = 9000L))
        val event2 = sm.process(0.88f, currentTimeMs = 10200L)
        assertNotNull(event2)
        assertEquals(2, sm.whistleCount)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /home/naseem/Projects/Whistl/android && ./gradlew :app:testDebugUnitTest --tests com.whistl.app.state.WhistleStateMachineTest`  
Expected: FAIL

- [ ] **Step 3: Implement `WhistleStateMachine`**

```kotlin
// android/app/src/main/java/com/whistl/app/state/WhistleStateMachine.kt
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
                    // Fell below threshold prematurely, discard false alarm
                    currentState = WhistleState.IDLE
                    peakConfidence = 0f
                } else {
                    peakConfidence = maxOf(peakConfidence, confidence)
                    if (currentTimeMs - whistleStartTimeMs >= minDurationMs) {
                        whistleCount++
                        currentState = WhistleState.COOLDOWN
                        cooldownStartTimeMs = currentTimeMs
                        return WhistleEvent(
                            whistleNumber = whistleCount,
                            timestampMs = currentTimeMs,
                            peakConfidence = peakConfidence
                        )
                    }
                }
            }
            WhistleState.CONFIRMED -> {
                currentState = WhistleState.COOLDOWN
                cooldownStartTimeMs = currentTimeMs
            }
            WhistleState.COOLDOWN -> {
                // To exit cooldown, audio must be below cooldownExitThreshold AND cooldownMs must have elapsed
                if (currentTimeMs - cooldownStartTimeMs >= cooldownMs && confidence <= cooldownExitThreshold) {
                    currentState = WhistleState.IDLE
                    peakConfidence = 0f
                }
            }
        }
        return null
    }

    fun reset() {
        currentState = WhistleState.IDLE
        whistleCount = 0
        whistleStartTimeMs = 0L
        cooldownStartTimeMs = 0L
        peakConfidence = 0f
    }
}
```

- [ ] **Step 4: Run unit tests to verify they pass**

Run: `cd /home/naseem/Projects/Whistl/android && ./gradlew :app:testDebugUnitTest --tests com.whistl.app.state.WhistleStateMachineTest`  
Expected: BUILD SUCCESSFUL, tests pass.

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/java/com/whistl/app/state/ android/app/src/test/java/com/whistl/app/state/
git commit -m "feat(state): implement WhistleStateMachine with debouncing and cooldown"
```

---

### Task 5: TFLite Inference Wrapper

**Files:**
- Create: `android/app/src/main/java/com/whistl/app/ml/WhistleDetector.kt`
- Test: `android/app/src/test/java/com/whistl/app/ml/WhistleDetectorTest.kt`

**Interfaces:**
- Produces: `class WhistleDetector(context: Context)`:
  - `fun detect(waveform: FloatArray): DetectionResult`
  - `fun close()`
- Produces: `data class DetectionResult(val backgroundProb: Float, val whistleProb: Float)`

- [ ] **Step 1: Write `WhistleDetector` implementation**

```kotlin
// android/app/src/main/java/com/whistl/app/ml/WhistleDetector.kt
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
```

- [ ] **Step 2: Write unit test verifying detector loads model and predicts on dummy buffer**

```kotlin
// android/app/src/test/java/com/whistl/app/ml/WhistleDetectorTest.kt
package com.whistl.app.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class WhistleDetectorTest {
    @Test
    fun testModelLoadsAndPredicts() {
        val modelFile = File("src/main/assets/whistle_detector_end_to_end.tflite")
        assertTrue("Model asset file must exist", modelFile.exists())

        val inputStream = FileInputStream(modelFile)
        val byteBuffer = inputStream.channel.map(
            FileChannel.MapMode.READ_ONLY, 0, modelFile.length()
        ).apply { order(ByteOrder.nativeOrder()) }

        val detector = WhistleDetector(byteBuffer)
        val silence = FloatArray(15600) { 0f }
        val result = detector.detect(silence)

        // For silence, background probability should dominate
        assertTrue("Background prob should be > 0.5", result.backgroundProb > 0.5f)
        assertEquals(1.0f, result.backgroundProb + result.whistleProb, 0.01f)
        detector.close()
    }
}
```

- [ ] **Step 3: Run unit test**

Run: `cd /home/naseem/Projects/Whistl/android && ./gradlew :app:testDebugUnitTest --tests com.whistl.app.ml.WhistleDetectorTest`  
Expected: BUILD SUCCESSFUL, test passes.

- [ ] **Step 4: Commit**

```bash
git add android/app/src/main/java/com/whistl/app/ml/ android/app/src/test/java/com/whistl/app/ml/
git commit -m "feat(ml): implement WhistleDetector TFLite engine and unit test"
```

---

### Task 6: Foreground Service, Notification Channels & Alarm Controller

**Files:**
- Create: `android/app/src/main/java/com/whistl/app/service/AlarmController.kt`
- Create: `android/app/src/main/java/com/whistl/app/service/WhistleDetectionService.kt`
- Create: `android/app/src/main/java/com/whistl/app/service/ServiceEvents.kt`
- Modify: `android/app/src/main/AndroidManifest.xml`

**Interfaces:**
- Produces: `AlarmController(context: Context)`:
  - `fun triggerAlarm()`
  - `fun stopAlarm()`
  - `val isAlarmPlaying: Boolean`
- Produces: `WhistleDetectionService : Service()`:
  - Listens in background, manages wake-lock, pushes notifications and updates `AppSessionRepository`.

- [ ] **Step 1: Implement `AlarmController`**

```kotlin
// android/app/src/main/java/com/whistl/app/service/AlarmController.kt
package com.whistl.app.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class AlarmController(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    val isAlarmPlaying: Boolean get() = isPlaying

    fun triggerAlarm() {
        if (isPlaying) return
        isPlaying = true

        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alertUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Vibrate pattern: wait 0ms, vibrate 500ms, sleep 250ms, vibrate 500ms
        val pattern = longArrayOf(0, 500, 250, 500)
        vibrator?.let { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, 0)) // 0 means repeat
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(pattern, 0)
            }
        }
    }

    fun stopAlarm() {
        isPlaying = false
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        vibrator?.cancel()
    }
}
```

- [ ] **Step 2: Implement `ServiceEvents` and `AppSessionRepository`**

```kotlin
// android/app/src/main/java/com/whistl/app/service/ServiceEvents.kt
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
```

- [ ] **Step 3: Implement `WhistleDetectionService`**

```kotlin
// android/app/src/main/java/com/whistl/app/service/WhistleDetectionService.kt
package com.whistl.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.whistl.app.R
import com.whistl.app.audio.AudioRecorderSource
import com.whistl.app.audio.RingBuffer
import com.whistl.app.ml.WhistleDetector
import com.whistl.app.state.WhistleStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WhistleDetectionService : Service() {
    companion object {
        const val ACTION_START = "com.whistl.app.action.START"
        const val ACTION_STOP = "com.whistl.app.action.STOP"
        const val ACTION_STOP_ALARM = "com.whistl.app.action.STOP_ALARM"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "whistl_listening_channel"
        const val ALARM_CHANNEL_ID = "whistl_alarm_channel"
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var audioRecorder: AudioRecorderSource? = null
    private var whistleDetector: WhistleDetector? = null
    private var ringBuffer: RingBuffer = RingBuffer(15600)
    private var stateMachine: WhistleStateMachine = WhistleStateMachine()
    private lateinit var alarmController: AlarmController

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var inferenceJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        alarmController = AlarmController(this)
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startListening()
            ACTION_STOP -> stopListening()
            ACTION_STOP_ALARM -> alarmController.stopAlarm()
        }
        return START_STICKY
    }

    private fun startListening() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Whistl::ListeningLock").apply {
            acquire(60 * 60 * 1000L) // 1 hour safety limit
        }

        val notification = buildNotification(AppSessionRepository.sessionState.value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        whistleDetector = WhistleDetector(this)
        ringBuffer.clear()
        stateMachine.reset()

        AppSessionRepository.updateState { it.copy(isListening = true, isTargetReached = false) }

        audioRecorder = AudioRecorderSource(sampleRate = 16000)
        audioRecorder?.start { samples, length ->
            ringBuffer.write(samples, length)
        }

        inferenceJob = serviceScope.launch {
            val inputBuffer = FloatArray(15600)
            while (isActive) {
                ringBuffer.readNormalizedFloatSnapshot(inputBuffer)
                val result = whistleDetector?.detect(inputBuffer)
                val conf = result?.whistleProb ?: 0f
                val now = System.currentTimeMillis()

                val event = stateMachine.process(conf, now)

                AppSessionRepository.updateState { current ->
                    val newHistory = if (event != null) current.whistleHistory + event else current.whistleHistory
                    val targetReached = stateMachine.whistleCount >= current.targetCount
                    current.copy(
                        currentCount = stateMachine.whistleCount,
                        whistleState = stateMachine.currentState,
                        currentConfidence = conf,
                        isTargetReached = targetReached,
                        whistleHistory = newHistory
                    )
                }

                updateNotification()

                if (AppSessionRepository.sessionState.value.isTargetReached && !alarmController.isAlarmPlaying) {
                    alarmController.triggerAlarm()
                }

                delay(400)
            }
        }
    }

    private fun stopListening() {
        inferenceJob?.cancel()
        audioRecorder?.stop()
        audioRecorder = null
        whistleDetector?.close()
        whistleDetector = null
        alarmController.stopAlarm()

        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null

        AppSessionRepository.updateState { it.copy(isListening = false) }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Whistl Listening Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Displays active listening status for cooker whistles" }
            manager.createNotificationChannel(channel)

            val alarmChannel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "Whistle Target Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority alarm when whistle target is reached"
                enableVibration(true)
            }
            manager.createNotificationChannel(alarmChannel)
        }
    }

    private fun buildNotification(state: AppSessionState): Notification {
        val stopIntent = Intent(this, WhistleDetectionService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val contentText = if (state.isTargetReached) {
            "TARGET REACHED! ${state.currentCount}/${state.targetCount} whistles"
        } else {
            "Whistles: ${state.currentCount} / ${state.targetCount} • Listening..."
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Whistl — Cooker Counter")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(AppSessionRepository.sessionState.value))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopListening()
        super.onDestroy()
    }
}
```

- [ ] **Step 4: Update `AndroidManifest.xml`**

Create `android/app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.VIBRATE" />

    <application
        android:allowBackup="true"
        android:icon="@android:drawable/ic_btn_speak_now"
        android:label="Whistl"
        android:roundIcon="@android:drawable/ic_btn_speak_now"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">

        <activity
            android:name=".ui.MainActivity"
            android:exported="true"
            android:theme="@android:style/Theme.Material.Light.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".service.WhistleDetectionService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="microphone" />

    </application>

</manifest>
```

- [ ] **Step 5: Verify build compilation**

Run: `cd /home/naseem/Projects/Whistl/android && ./gradlew :app:assembleDebug`  
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add android/app/src/main/java/com/whistl/app/service/ android/app/src/main/AndroidManifest.xml
git commit -m "feat(service): add WhistleDetectionService, AlarmController, and manifest permissions"
```

---

### Task 7: Jetpack Compose UI & State Management

**Files:**
- Create: `android/app/src/main/java/com/whistl/app/ui/theme/Theme.kt`
- Create: `android/app/src/main/java/com/whistl/app/ui/components/CounterRing.kt`
- Create: `android/app/src/main/java/com/whistl/app/ui/components/AudioConfidenceBar.kt`
- Create: `android/app/src/main/java/com/whistl/app/ui/WhistleCounterScreen.kt`
- Create: `android/app/src/main/java/com/whistl/app/ui/MainActivity.kt`

**Interfaces:**
- Produces: Polished Material 3 user interface with:
  - Whistle Target Stepper and Presets (1–5)
  - Circular Progress Indicator & Live Count
  - Audio confidence meter
  - Start / Stop listening button with runtime audio permission checks
  - Alarm dismissal dialog with haptic feedback
  - Chronological history of detected whistles

- [ ] **Step 1: Create Theme and UI Components**

Create `Theme.kt`:
```kotlin
// android/app/src/main/java/com/whistl/app/ui/theme/Theme.kt
package com.whistl.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF6F00),
    secondary = Color(0xFFFFB300),
    tertiary = Color(0xFF26A69A),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFE65100),
    secondary = Color(0xFFF57C00),
    tertiary = Color(0xFF00897B),
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFFFFFF)
)

@Composable
fun WhistlTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, content = content)
}
```

Create `CounterRing.kt`:
```kotlin
// android/app/src/main/java/com/whistl/app/ui/components/CounterRing.kt
package com.whistl.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CounterRing(
    current: Int,
    target: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (target > 0) (current.toFloat() / target).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(240.dp)) {
        Canvas(modifier = Modifier.size(220.dp)) {
            val strokeWidth = 18.dp.toPx()
            drawCircle(
                color = trackColor,
                style = Stroke(width = strokeWidth)
            )
            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$current",
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Target: $target whistles",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

Create `AudioConfidenceBar.kt`:
```kotlin
// android/app/src/main/java/com/whistl/app/ui/components/AudioConfidenceBar.kt
package com.whistl.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whistl.app.state.WhistleState

@Composable
fun AudioConfidenceBar(
    confidence: Float,
    state: WhistleState,
    modifier: Modifier = Modifier
) {
    val animatedConf by animateFloatAsState(targetValue = confidence, label = "confidence")
    val barColor = when {
        state == WhistleState.CONFIRMED -> Color(0xFF00E676)
        state == WhistleState.POSSIBLE -> Color(0xFFFFEB3B)
        state == WhistleState.COOLDOWN -> Color(0xFF29B6F6)
        else -> MaterialTheme.colorScheme.primary
    }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Text(
            text = "Whistle Confidence: ${(confidence * 100).toInt()}% • State: $state",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        LinearProgressIndicator(
            progress = { animatedConf },
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}
```

- [ ] **Step 2: Create `WhistleCounterScreen.kt`**

```kotlin
// android/app/src/main/java/com/whistl/app/ui/WhistleCounterScreen.kt
package com.whistl.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whistl.app.service.AppSessionState
import com.whistl.app.ui.components.AudioConfidenceBar
import com.whistl.app.ui.components.CounterRing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WhistleCounterScreen(
    state: AppSessionState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSetTarget: (Int) -> Unit,
    onReset: () -> Unit,
    onDismissAlarm: () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Whistl",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onReset) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset")
                }
            }

            // Target selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                IconButton(
                    onClick = { if (state.targetCount > 1) onSetTarget(state.targetCount - 1) },
                    enabled = !state.isListening
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease Target")
                }
                listOf(1, 2, 3, 4, 5).forEach { preset ->
                    FilterChip(
                        selected = state.targetCount == preset,
                        onClick = { onSetTarget(preset) },
                        label = { Text("$preset") },
                        enabled = !state.isListening,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
                IconButton(
                    onClick = { onSetTarget(state.targetCount + 1) },
                    enabled = !state.isListening
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase Target")
                }
            }

            // Counter ring visualizer
            CounterRing(current = state.currentCount, target = state.targetCount)

            // Audio Confidence Bar
            if (state.isListening) {
                AudioConfidenceBar(confidence = state.currentConfidence, state = state.whistleState)
            } else {
                Spacer(modifier = Modifier.height(30.dp))
            }

            // Whistle History List
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Whistle Log",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (state.whistleHistory.isEmpty()) {
                        Text(
                            text = "No whistles detected yet.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        LazyColumn {
                            items(state.whistleHistory.reversed()) { event ->
                                Text(
                                    text = "Whistle #${event.whistleNumber} at ${timeFormat.format(Date(event.timestampMs))} (Conf: ${(event.peakConfidence * 100).toInt()}%)",
                                    fontSize = 13.sp,
                                    color = Color(0xFF00E676),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Primary Start/Stop FAB Button
            Button(
                onClick = { if (state.isListening) onStopListening() else onStartListening() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isListening) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = if (state.isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = null
                )
                Text(
                    text = if (state.isListening) "Stop Listening" else "Start Listening",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        // Target Reached Alarm Dialog
        if (state.isTargetReached) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Cooker Ready!", fontWeight = FontWeight.Bold, color = Color(0xFFFF5252)) },
                text = { Text("Target of ${state.targetCount} whistles reached! Turn off the stove.") },
                confirmButton = {
                    Button(
                        onClick = onDismissAlarm,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                    ) {
                        Text("Stop Alarm", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}
```

- [ ] **Step 3: Implement `MainActivity.kt` with runtime permission management**

```kotlin
// android/app/src/main/java/com/whistl/app/ui/MainActivity.kt
package com.whistl.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.whistl.app.service.AppSessionRepository
import com.whistl.app.service.WhistleDetectionService
import com.whistl.app.ui.theme.WhistlTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (recordAudioGranted) {
            startDetectionService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WhistlTheme {
                val state by AppSessionRepository.sessionState.collectAsState()

                WhistleCounterScreen(
                    state = state,
                    onStartListening = { checkAndRequestPermissions() },
                    onStopListening = { stopDetectionService() },
                    onSetTarget = { newTarget ->
                        AppSessionRepository.updateState { it.copy(targetCount = newTarget) }
                    },
                    onReset = {
                        AppSessionRepository.resetSession()
                    },
                    onDismissAlarm = {
                        val stopAlarmIntent = Intent(this, WhistleDetectionService::class.java).apply {
                            action = WhistleDetectionService.ACTION_STOP_ALARM
                        }
                        startService(stopAlarmIntent)
                        AppSessionRepository.updateState { it.copy(isTargetReached = false) }
                    }
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            startDetectionService()
        } else {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun startDetectionService() {
        val intent = Intent(this, WhistleDetectionService::class.java).apply {
            action = WhistleDetectionService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopDetectionService() {
        val intent = Intent(this, WhistleDetectionService::class.java).apply {
            action = WhistleDetectionService.ACTION_STOP
        }
        startService(intent)
    }
}
```

- [ ] **Step 4: Build and test debug APK assembly**

Run: `cd /home/naseem/Projects/Whistl/android && ./gradlew assembleDebug`  
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/java/com/whistl/app/ui/
git commit -m "feat(ui): implement Jetpack Compose UI, theme, and MainActivity with permission management"
```

---

### Task 8: End-to-End Build & APK Packaging

**Files:**
- Output: `android/app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 1: Run complete test suite and assemble APK**

Run:
```bash
cd /home/naseem/Projects/Whistl/android && ./gradlew testDebugUnitTest assembleDebug
```
Expected: BUILD SUCCESSFUL, all unit tests pass, `app-debug.apk` is generated.

- [ ] **Step 2: Verify APK metadata and asset contents**

Run:
```bash
unzip -l android/app/build/outputs/apk/debug/app-debug.apk | grep -E "(tflite|AndroidManifest.xml|classes.dex)"
```
Expected: `assets/whistle_detector_end_to_end.tflite` is packed without compression.

- [ ] **Step 3: Commit and summarize**

```bash
git commit --allow-empty -m "build: verify final debug APK package assembly"
```
