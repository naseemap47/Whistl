import tensorflow as tf
import tensorflow_hub as hub
import numpy as np
import csv
import scipy.signal
import scipy.io.wavfile as wav

# ---------- Load YAMNet ----------
print("Loading YAMNet from TensorFlow Hub...")
yamnet = hub.load('https://tfhub.dev/google/yamnet/1')

# ---------- Load class names ----------
def class_names_from_csv(csv_path):
    names = []
    with tf.io.gfile.GFile(csv_path) as f:
        reader = csv.DictReader(f)
        for row in reader:
            names.append(row['display_name'])
    return names

class_map_path = yamnet.class_map_path().numpy()
class_names = class_names_from_csv(class_map_path)

# ---------- Helper: resample to 16 kHz ----------
def ensure_sample_rate(original_sr, waveform, desired_sr=16000):
    if original_sr != desired_sr:
        desired_length = int(round(len(waveform) / original_sr * desired_sr))
        waveform = scipy.signal.resample(waveform, desired_length)
    return desired_sr, waveform

# ---------- Inference on a file ----------
def infer_file(filepath):
    sr, data = wav.read(filepath)
    sr, data = ensure_sample_rate(sr, data)

    # Normalize to [-1.0, 1.0]
    if data.dtype == np.int16:
        waveform = data / 32768.0
    else:
        waveform = data.astype(np.float32)

    # Run YAMNet
    scores, embeddings, spectrogram = yamnet(waveform)
    scores_np = scores.numpy()

    # Mean-aggregate scores across frames for clip-level prediction
    mean_scores = np.mean(scores_np, axis=0)

    top5 = np.argsort(mean_scores)[::-1][:5]
    print(f"\n📊 Top 5 predictions for: {filepath}")
    for idx in top5:
        print(f"  {class_names[idx]:30s}  →  {mean_scores[idx]:.4f}")

    # Crucial for pressure cookers: check Steam/Hiss, not Whistle
    for target in ["Steam", "Hiss", "Whistle"]:
        if target in class_names:
            tid = class_names.index(target)
            print(f"  🔔 {target}: {mean_scores[tid]:.4f}")

if __name__ == "__main__":
    infer_file("dataset/whistle_005.wav")   # Replace with your file