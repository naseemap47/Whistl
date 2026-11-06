import tensorflow as tf
import tensorflow_hub as hub
import numpy as np
import librosa

# ---------- Load YAMNet (feature extractor) and custom classifier ----------
yamnet = hub.load('https://tfhub.dev/google/yamnet/1')
classifier = tf.keras.models.load_model('whistle_classifier.h5')

CLASS_NAMES = ["background", "whistle"]

def infer_file(filepath):
    """Run the trained pipeline on a WAV file."""
    waveform, _ = librosa.load(filepath, sr=16000, mono=True)
    waveform = waveform.astype(np.float32)

    _, embeddings, _ = yamnet(waveform)
    emb_mean = np.mean(embeddings.numpy(), axis=0, keepdims=True)

    probs = classifier.predict(emb_mean, verbose=0)[0]
    pred_idx = np.argmax(probs)

    print(f"\n📄 File: {filepath}")
    print(f"  Prediction: {CLASS_NAMES[pred_idx]}")
    print(f"  Confidence: {probs[pred_idx]:.4f}")
    print(f"  Raw probs:  background={probs[0]:.4f}, whistle={probs[1]:.4f}")

if __name__ == "__main__":
    infer_file("dataset/background/background_010.wav")