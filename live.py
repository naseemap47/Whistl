import numpy as np
import tensorflow as tf
import tensorflow_hub as hub
import pyaudio

# ---------- Configuration ----------
SAMPLE_RATE = 16000
CHUNK_DURATION = 0.975  # ~0.975 seconds works well for YAMNet
CHUNK_SIZE = int(SAMPLE_RATE * CHUNK_DURATION)
MODEL_PATH = "whistle_classifier.h5"

# Class names matching your training script folder structure
CLASS_NAMES = ["background", "whistle"]

print("Loading YAMNet feature extractor from TF Hub...")
yamnet = hub.load('https://tfhub.dev/google/yamnet/1')

print(f"Loading your custom classifier model from: {MODEL_PATH}...")
classifier = tf.keras.models.load_model(MODEL_PATH)

# Initialize PyAudio for microphone streaming
p = pyaudio.PyAudio()
stream = p.open(
    format=pyaudio.paInt16,
    channels=1,
    rate=SAMPLE_RATE,
    input=True,
    frames_per_buffer=CHUNK_SIZE
)

print("\n--- Starting Live Custom Whistle Inference ---")
print("Listening to your microphone... Press Ctrl+C to stop.\n")

try:
    while True:
        # 1. Read raw audio data from microphone
        data = stream.read(CHUNK_SIZE, exception_on_overflow=False)
        
        # 2. Convert raw buffer to float32 numpy array normalized to [-1.0, 1.0]
        audio_data = np.frombuffer(data, dtype=np.int16).astype(np.float32) / 32768.0
        
        # 3. Extract YAMNet embeddings (same as training)
        _, embeddings, _ = yamnet(audio_data)
        clip_embedding = np.mean(embeddings.numpy(), axis=0, keepdims=True) # Shape: (1, 1024)
        
        # 4. Run prediction through your custom classifier head
        predictions = classifier.predict(clip_embedding, verbose=0)[0]
        
        # 5. Get top class and confidence
        top_class_idx = np.argmax(predictions)
        top_class_name = CLASS_NAMES[top_class_idx]
        confidence = predictions[top_class_idx]
        
        # Print results live in terminal
        print(f"Predicted: {top_class_name:<12} | Confidence: {confidence:.2f}")

except KeyboardInterrupt:
    print("\nStopping live inference...")
finally:
    stream.stop_stream()
    stream.close()
    p.terminate()