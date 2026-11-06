from pathlib import Path
import librosa
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, confusion_matrix
import tensorflow as tf
import tensorflow_hub as hub

from whistl.config import (
    NORMALIZED_DATA_DIR,
    MODEL_H5_PATH,
    SAMPLE_RATE,
    BATCH_SIZE,
    EPOCHS,
    LEARNING_RATE,
    YAMNET_HUB_URL,
    CLASS_NAMES,
)
from whistl.models.classifier import build_whistle_classifier_head


def load_wav_16k_mono(filepath: Path) -> np.ndarray:
    """Load a WAV audio file, resample to 16 kHz mono float32."""
    waveform, _ = librosa.load(str(filepath), sr=SAMPLE_RATE, mono=True)
    return waveform.astype(np.float32)


def extract_embeddings(filepath: Path, yamnet_model) -> np.ndarray:
    """Extract mean YAMNet frame embedding (shape: 1024)."""
    waveform = load_wav_16k_mono(filepath)
    _, embeddings, _ = yamnet_model(waveform)
    return np.mean(embeddings.numpy(), axis=0)


def build_dataset_from_dir(dataset_dir: Path, yamnet_model):
    features, labels = [], []

    for label_idx, label_name in enumerate(CLASS_NAMES):
        folder = dataset_dir / label_name
        if not folder.is_dir():
            print(f"⚠️ Folder not found: {folder}")
            continue

        files = sorted(list(folder.glob("*.wav")))
        print(f"  {label_name}: {len(files)} files")

        for fpath in files:
            try:
                emb = extract_embeddings(fpath, yamnet_model)
                features.append(emb)
                labels.append(label_idx)
            except Exception as e:
                print(f"  ⚠️ Skipping {fpath.name}: {e}")

    return np.array(features, dtype=np.float32), np.array(labels, dtype=np.int32)


def train_model(
    data_dir: Path = NORMALIZED_DATA_DIR / "train",
    model_output_path: Path = MODEL_H5_PATH,
    epochs: int = EPOCHS,
    batch_size: int = BATCH_SIZE
) -> tf.keras.Model:
    print(f"Loading YAMNet from {YAMNET_HUB_URL}...")
    yamnet = hub.load(YAMNET_HUB_URL)

    print(f"\n📂 Building dataset from {data_dir}...")
    X, y = build_dataset_from_dir(data_dir, yamnet)
    print(f"Total samples: {len(X)}, Feature shape: {X.shape}")

    if len(X) == 0:
        raise ValueError(f"No audio files found in {data_dir}")

    X_train, X_val, y_train, y_val = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    model = build_whistle_classifier_head(num_classes=len(CLASS_NAMES))
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=LEARNING_RATE),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"]
    )

    print("\n🧠 Training Whistle Classifier Head...")
    model.fit(
        X_train,
        y_train,
        validation_data=(X_val, y_val),
        epochs=epochs,
        batch_size=batch_size,
        verbose=1
    )

    model_output_path.parent.mkdir(parents=True, exist_ok=True)
    model.save(str(model_output_path))
    print(f"\n✅ Model saved to: {model_output_path}")

    # Validation evaluation
    y_pred = np.argmax(model.predict(X_val, verbose=0), axis=1)
    print("\n📊 Validation Report:")
    print(classification_report(y_val, y_pred, target_names=CLASS_NAMES))

    return model
