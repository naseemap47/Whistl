import time
import numpy as np
import tensorflow as tf
import tensorflow_hub as hub
import pyaudio

# YAMNet requires 16kHz sample rate, mono-channel input
SAMPLE_RATE = 16000
# YAMNet operates ideally on ~0.975 second chunks (15600 samples)
CHUNK_DURATION = 0.975 
CHUNK_SIZE = int(SAMPLE_RATE * CHUNK_DURATION)

print("Loading YAMNet model from TensorFlow Hub...")
model = hub.load('https://tfhub.dev/google/yamnet/1')

# Extract class names mapping from the model
class_map_path = model.class_map_path().numpy()
class_names = []
with tf.io.gfile.GFile(class_map_path) as csv_file:
    import csv
    reader = csv.DictReader(csv_file)
    for row in reader:
        class_names.append(row['display_name'])

# Initialize PyAudio for microphone streaming
p = pyaudio.PyAudio()
stream = p.open(
    format=pyaudio.paInt16,
    channels=1,
    rate=SAMPLE_RATE,
    input=True,
    frames_per_buffer=CHUNK_SIZE
)

print("\n--- Starting Live YAMNet Inference ---")
print("Listening to your microphone... Press Ctrl+C to stop.\n")

try:
    while True:
        # Read raw audio data from microphone
        data = stream.read(CHUNK_SIZE, exception_on_overflow=False)
        
        # Convert raw buffer to a numpy array (int16) and normalize to float32 [-1.0, 1.0]
        audio_data = np.frombuffer(data, dtype=np.int16).astype(np.float32) / 32768.0
        
        # Run inference using YAMNet
        scores, embeddings, spectrogram = model(audio_data)
        
        # Average scores across time frames of this snippet
        prediction = tf.reduce_mean(scores, axis=0).numpy()
        
        # Get the top matching sound class
        top_class_index = np.argmax(prediction)
        top_class_name = class_names[top_class_index]
        confidence = prediction[top_class_index]
        
        # Print results live in the terminal
        print(f"Predicted: {top_class_name:<30} | Confidence: {confidence:.2f}")

except KeyboardInterrupt:
    print("\nStopping live inference...")
finally:
    stream.stop_stream()
    stream.close()
    p.terminate()