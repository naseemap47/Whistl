from pathlib import Path

# Paths
PROJECT_ROOT = Path(__file__).resolve().parent.parent

DATA_DIR = PROJECT_ROOT / "data"
RAW_DATA_DIR = DATA_DIR / "raw"
PROCESSED_DATA_DIR = DATA_DIR / "processed"
NORMALIZED_DATA_DIR = DATA_DIR / "normalized"

MODELS_DIR = PROJECT_ROOT / "models"
MODEL_H5_PATH = MODELS_DIR / "whistle_classifier.h5"
MODEL_INT8_TFLITE_PATH = MODELS_DIR / "whistle_classifier_int8.tflite"
MODEL_E2E_TFLITE_PATH = MODELS_DIR / "whistle_detector_end_to_end.tflite"

ANDROID_ASSETS_MODEL_PATH = PROJECT_ROOT / "android" / "app" / "src" / "main" / "assets" / "whistle_detector_end_to_end.tflite"

# Audio parameters
SAMPLE_RATE = 16000
CHUNK_DURATION = 0.975  # ~0.975 seconds for YAMNet input
CHUNK_SIZE = int(SAMPLE_RATE * CHUNK_DURATION)  # 15600 samples

# Classes
CLASS_NAMES = ["background", "whistle"]
CLASS_MAP = {name: idx for idx, name in enumerate(CLASS_NAMES)}

# Training defaults
BATCH_SIZE = 32
EPOCHS = 20
LEARNING_RATE = 1e-3
YAMNET_HUB_URL = "https://tfhub.dev/google/yamnet/1"
