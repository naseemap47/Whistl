# Whistl 🍲💨

**Whistl** is an AI-powered smart kitchen assistant that automatically listens to and counts pressure cooker whistles in real-time. Once your target whistle count is reached, it rings a loud alarm and vibrates to alert you to turn off the stove flame—even while running in the background with your phone locked on the kitchen counter.

---

## 🌟 Key Features

### 📱 Android Application
* **On-Device Edge ML**: Runs an end-to-end TensorFlow Lite model locally on your smartphone with zero latency and complete privacy—no internet connection required.
* **Background Foreground Service**: Continues listening and counting accurately even when the screen is locked, phone is asleep, or you are using other apps.
* **Smart Whistle Debounce Engine**: Uses an acoustic finite-state machine with a 1.0s duration validation gate (to reject transient clangs, dropped spoons, or speech) and a 4.0s cooldown period to ensure each continuous whistle blast is counted exactly once.
* **Target Alarm & Notification**: Continuous alarm ringtone, custom vibration pattern, and full-screen alert when the target whistle count is reached.
* **Modern Material 3 UI**:
  * **Dynamic Counter Ring**: Animated progress circle showing whistles counted and whistles remaining.
  * **Quick Target Stepper**: Preset selector chips (`1` to `5`) and steppers (`-` / `+`).
  * **Live Acoustic Visualizer**: Multi-bar animated audio equalizer showing live confidence.
  * **Whistle Timeline Log**: Detailed history with exact timestamps for every detected whistle.
* **Custom Adaptive App Icon**: Custom pressure cooker illustration with steam jets and acoustic sonic wave ripples.

### 🧠 Machine Learning Pipeline
* **Acoustic Backbone**: Leverages Google's **YAMNet** audio neural network (pre-trained on AudioSet) for 1024-dimensional acoustic feature extraction.
* **Custom Classifier Head**: Dense neural network trained on normalized pressure cooker steam whistle audio vs. diverse ambient kitchen noise.
* **Unified End-to-End TFLite Model**: Combined YAMNet + classifier exported directly into a single `.tflite` model (`whistle_detector_end_to_end.tflite`) that takes raw 16 kHz audio samples (`float32[15600]`) and directly outputs whistle confidence probabilities.

---

## 🏗️ Architecture & State Machine

```
   [ Microphone Audio ] (16 kHz Mono PCM)
            │
            ▼
   [ Circular RingBuffer ] (15,600 samples / ~0.975s window)
            │
            ▼  (Inference every 400ms)
 [ End-to-End TFLite Model ] (YAMNet + Classifier)
            │
            ▼  Whistle Confidence (0.0 to 1.0)
   ┌─────────────────────────────────────────────────────────┐
   │             Whistle State Machine                       │
   │                                                         │
   │  [IDLE] ──(Conf >= 0.75)──> [POSSIBLE]                  │
   │    ▲                          │                         │
   │    │ (Fell below 0.65)        │ (Sustained >= 1.0s)     │
   │    └──────────────────────────┤                         │
   │                               ▼                         │
   │                    [CONFIRMED WHISTLE]                  │
   │                     (Count += 1, Haptic)                │
   │                               │                         │
   │                               ▼                         │
   │                          [COOLDOWN]                     │
   │              (Locked for 4.0s until cooker settles)     │
   │                               │                         │
   │             (Elapsed >= 4.0s & Conf <= 0.35)            │
   │                               ▼                         │
   │                            [IDLE]                       │
   └─────────────────────────────────────────────────────────┘
            │
            ▼ (When Count == Target)
 [ AlarmController: Ringtone Loop + Vibration + Cooker Ready Modal ]
```

---

## 📁 Repository Structure

```
Whistl/
├── android/                         # Native Kotlin Android Application
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── assets/              # Bundled end-to-end TFLite model
│   │   │   │   └── whistle_detector_end_to_end.tflite
│   │   │   ├── java/com/whistl/app/
│   │   │   │   ├── audio/           # AudioRecord stream & RingBuffer
│   │   │   │   ├── ml/              # TFLite inference detector wrapper
│   │   │   │   ├── service/         # Foreground Service, Notification & Alarm
│   │   │   │   ├── state/           # WhistleStateMachine & AppSessionRepository
│   │   │   │   └── ui/              # Jetpack Compose UI, components, theme
│   │   │   ├── res/                 # Adaptive app icons, drawables, values
│   │   │   └── AndroidManifest.xml  # Permissions & service declarations
│   │   └── build.gradle.kts
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── gradlew
│
├── dataset/                         # Raw audio recordings (whistle & background)
├── dataset_norm/                    # Normalized and split training dataset
├── collect_data.py                  # Microphone audio sample recorder
├── split_audio.py                   # Slices audio into 1-second WAV snippets
├── data_norm.py                     # Peak-normalizes audio dataset
├── train_yamnet.py                  # Trains transfer learning classifier head
├── test_yamnet.py                   # Evaluates precision, recall, F1-score & confusion matrix
├── export_end_to_end_tflite.py      # Combines YAMNet + classifier into unified TFLite model
├── test_exported_tflite.py          # Verifies exported TFLite model against real samples
├── live.py                          # Live microphone test in terminal (Keras .h5)
└── infer_tflite.py                  # Live microphone test in terminal (TFLite)
```

---

## 🚀 Getting Started

### Prerequisites

* **Python Environment**: Python 3.10+ (managed via `uv` or `venv`)
* **Android Development**:
  * Android Studio / Android SDK (API 34+)
  * Java 17
  * Connected Android phone (Android 8.0 Oreo or higher) with USB Debugging enabled

---

### 1. Python ML Pipeline

#### Setup Virtual Environment
```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r pyproject.toml  # or uv sync
```

#### Train the Whistle Classifier
```bash
python train_yamnet.py
```

#### Export Unified End-to-End TFLite Model
```bash
python export_end_to_end_tflite.py
python test_exported_tflite.py
```

#### Test Live Whistle Detection via PC Microphone
```bash
python live.py
```

---

### 2. Android App

#### Build Debug APK via Gradle
```bash
cd android
./gradlew assembleDebug
```
The output APK is generated at:
```
android/app/build/outputs/apk/debug/app-debug.apk
```

#### Run Unit Tests
```bash
cd android
./gradlew testDebugUnitTest
```

#### Install on Connected Android Device
Ensure your device is connected via USB and authorized with `adb`:
```bash
adb devices
./gradlew installDebug
```
Or directly using ADB:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📋 Required Android Permissions

The app requests the following permissions to ensure continuous operation:
* `android.permission.RECORD_AUDIO`: Captures audio from the microphone for real-time steam whistle classification.
* `android.permission.POST_NOTIFICATIONS`: Displays persistent status and target completion alerts on Android 13+.
* `android.permission.FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MICROPHONE`: Keeps audio capture running when the app is minimized or phone is locked.
* `android.permission.WAKE_LOCK`: Prevents CPU sleep while actively cooking.
* `android.permission.VIBRATE`: Provides haptic feedback and alarm alerts.

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
