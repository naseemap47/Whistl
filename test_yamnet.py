import tensorflow as tf
import tensorflow_hub as hub
import numpy as np
import librosa
import os
from sklearn.metrics import (classification_report, confusion_matrix,
                             accuracy_score, precision_score, recall_score,
                             f1_score)

# ---------- Load models ----------
yamnet = hub.load('https://tfhub.dev/google/yamnet/1')
classifier = tf.keras.models.load_model('whistle_classifier.h5')
CLASS_NAMES = ["background", "whistle"]

# ---------- Configuration ----------
TEST_DIR = "dataset/test"     # folder with background/ and whistle/
SAMPLE_RATE = 16000

def load_test_data():
    """Walk the test folder and return embeddings + true labels."""
    features, labels = [], []

    for label_idx, label_name in enumerate(CLASS_NAMES):
        folder = os.path.join(TEST_DIR, label_name)
        if not os.path.isdir(folder):
            print(f"⚠️ Folder not found: {folder}")
            continue
        files = [f for f in os.listdir(folder) if f.endswith(".wav")]
        print(f"Loading {len(files)} files from {label_name}...")
        for fname in files:
            fpath = os.path.join(folder, fname)
            try:
                waveform, _ = librosa.load(fpath, sr=SAMPLE_RATE, mono=True)
                waveform = waveform.astype(np.float32)
                _, embeddings, _ = yamnet(waveform)
                emb = np.mean(embeddings.numpy(), axis=0)
                features.append(emb)
                labels.append(label_idx)
            except Exception as e:
                print(f"  ⚠️ Skipped {fname}: {e}")

    return np.array(features, dtype=np.float32), np.array(labels, dtype=np.int32)

def evaluate():
    print("=" * 60)
    print("  YAMNet Transfer-Learning Classifier — Evaluation")
    print("=" * 60)

    X_test, y_test = load_test_data()
    print(f"\nTest samples: {len(X_test)}")

    if len(X_test) == 0:
        print("❌ No test samples found. Check your dataset/test/ folder.")
        return

    # Predict
    y_pred_probs = classifier.predict(X_test, verbose=0)
    y_pred = np.argmax(y_pred_probs, axis=1)

    # ---------- Metrics ----------
    print("\n📊 Classification Report:")
    print(classification_report(y_test, y_pred, target_names=CLASS_NAMES))

    print("📉 Confusion Matrix:")
    cm = confusion_matrix(y_test, y_pred)
    print(cm)
    print("\n  (rows = true labels, columns = predicted labels)")

    # ---------- Scalar metrics ----------
    acc  = accuracy_score(y_test, y_pred)
    prec = precision_score(y_test, y_pred, average='binary')
    rec  = recall_score(y_test, y_pred, average='binary')
    f1   = f1_score(y_test, y_pred, average='binary')

    print(f"\n✅ Accuracy : {acc:.4f}")
    print(f"✅ Precision: {prec:.4f}")
    print(f"✅ Recall   : {rec:.4f}")
    print(f"✅ F1-score : {f1:.4f}")

    # ---------- Per-class confidence analysis ----------
    print("\n🔍 Per-sample confidence (first 10):")
    for i in range(min(10, len(y_test))):
        true_label = CLASS_NAMES[y_test[i]]
        pred_label = CLASS_NAMES[y_pred[i]]
        conf = y_pred_probs[i][y_pred[i]]
        status = "✔" if true_label == pred_label else "✘"
        print(f"  {status}  true={true_label:10s}  pred={pred_label:10s}  conf={conf:.3f}")

if __name__ == "__main__":
    evaluate()