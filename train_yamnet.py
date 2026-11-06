"""
📂 Expected Dataset Structure
dataset/
├── train/
│   ├── whistle/
│   │   ├── whistle_001.wav
│   │   └── ...
│   └── background/
│       ├── background_001.wav
│       └── ...
└── test/
    ├── whistle/
    └── background/
"""

import tensorflow as tf
import tensorflow_hub as hub
import numpy as np
import os
import librosa
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, confusion_matrix

# ---------- Configuration ----------
SAMPLE_RATE = 16000
DATASET_DIR = "dataset_norm"
MODEL_SAVE_PATH = "whistle_classifier.h5"
BATCH_SIZE = 32
EPOCHS = 20

# ---------- Load YAMNet as a feature extractor ----------
print("Loading YAMNet feature extractor...")
yamnet = hub.load('https://tfhub.dev/google/yamnet/1')

# ---------- Audio loading helper ----------
def load_wav_16k_mono(filepath):
    """Load a WAV file, resample to 16 kHz mono, and normalize."""
    waveform, sr = librosa.load(filepath, sr=SAMPLE_RATE, mono=True)
    return waveform.astype(np.float32)

def extract_embeddings(filepath):
    """Get the mean YAMNet embedding for one audio clip."""
    waveform = load_wav_16k_mono(filepath)
    _, embeddings, _ = yamnet(waveform)
    # Average the frame-level embeddings into a single clip-level vector
    return np.mean(embeddings.numpy(), axis=0)

# ---------- Build dataset from folder structure ----------
def build_dataset(split="train"):
    features, labels = [], []
    split_dir = os.path.join(DATASET_DIR, split)

    for label_idx, label_name in enumerate(["background", "whistle"]):
        folder = os.path.join(split_dir, label_name)
        if not os.path.isdir(folder):
            continue
        files = [f for f in os.listdir(folder) if f.endswith(".wav")]
        print(f"  {split}/{label_name}: {len(files)} files")
        for fname in files:
            fpath = os.path.join(folder, fname)
            try:
                emb = extract_embeddings(fpath)
                features.append(emb)
                labels.append(label_idx)
            except Exception as e:
                print(f"  ⚠️ Skipping {fname}: {e}")

    return np.array(features, dtype=np.float32), np.array(labels, dtype=np.int32)

print("\n📂 Building training set...")
X_train, y_train = build_dataset("train")
# print("\n📂 Building test set...")
# X_test, y_test = build_dataset("test")

print(f"\nTraining samples: {len(X_train)}") #   |  Test samples: {len(X_test)}
print(f"Feature shape: {X_train.shape}")   # (N, 1024)

# ---------- Build the classifier head ----------
model = tf.keras.Sequential([
    tf.keras.layers.Input(shape=(1024,)),
    tf.keras.layers.BatchNormalization(),
    tf.keras.layers.Dense(512, activation='relu'),
    tf.keras.layers.Dropout(0.3),
    tf.keras.layers.Dense(256, activation='relu'),
    tf.keras.layers.Dropout(0.3),
    tf.keras.layers.Dense(2, activation='softmax')   # 2 classes: background, whistle
])

model.compile(
    optimizer=tf.keras.optimizers.Adam(learning_rate=1e-3),
    loss='sparse_categorical_crossentropy',
    metrics=['accuracy']
)

model.summary()

# ---------- Train ----------
# Split off a validation set from the training data
X_tr, X_val, y_tr, y_val = train_test_split(
    X_train, y_train, test_size=0.2, random_state=42, stratify=y_train
)

history = model.fit(
    X_tr, y_tr,
    validation_data=(X_val, y_val),
    batch_size=BATCH_SIZE,
    epochs=EPOCHS,
    verbose=1
)

# ---------- Save the trained model ----------
model.save(MODEL_SAVE_PATH)
print(f"\n💾 Model saved to: {MODEL_SAVE_PATH}")