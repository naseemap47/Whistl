import numpy as np
import tensorflow as tf
import tensorflow_hub as hub
import pyaudio

# ---------- Configuration ----------
SAMPLE_RATE = 16000
CHUNK_DURATION = 0.975  # ~0.975 seconds for YAMNet
CHUNK_SIZE = int(SAMPLE_RATE * CHUNK_DURATION)
TFLITE_MODEL_PATH = "whistle_classifier_int8.tflite"

CLASS_NAMES = ["background", "whistle"]

print("Loading YAMNet feature extractor from TF Hub...")
yamnet = hub.load('https://tfhub.dev/google/yamnet/1')

print(f"Loading TFLite model from {TFLITE_MODEL_PATH}...")
interpreter = tf.lite.Interpreter(model_path=TFLITE_MODEL_PATH)
interpreter.allocate_tensors()

# Get input and output details
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

print(f"TFLite Input Shape: {input_details[0]['shape']}")
print(f"TFLite Input Type:  {input_details[0]['dtype']}")

# Initialize PyAudio for microphone streaming
p = pyaudio.PyAudio()
stream = p.open(
    format=pyaudio.paInt16,
    channels=1,
    rate=SAMPLE_RATE,
    input=True,
    frames_per_buffer=CHUNK_SIZE
)

print("\n--- Starting Live TFLite Inference ---")
print("Listening to your microphone... Press Ctrl+C to stop.\n")

try:
    while True:
        # 1. Read raw audio data from microphone
        data = stream.read(CHUNK_SIZE, exception_on_overflow=False)
        
        # 2. Convert buffer to float32 numpy array normalized to [-1.0, 1.0]
        audio_data = np.frombuffer(data, dtype=np.int16).astype(np.float32) / 32768.0
        
        # 3. Extract YAMNet embeddings (shape: [N, 1024] -> averaged to [1, 1024])
        _, embeddings, _ = yamnet(audio_data)
        clip_embedding = np.mean(embeddings.numpy(), axis=0, keepdims=True)
        
        # 4. Handle INT8 input quantization if the model expects int8 input
        if input_details[0]['dtype'] == np.int8:
            # Get quantization parameters (scale and zero_point)
            scale, zero_point = input_details[0]['quantization']
            # Quantize float32 embedding to int8
            clip_embedding = np.round(clip_embedding / scale + zero_point).astype(np.int8)
        else:
            # Ensure it is float32 if the model expects float
            clip_embedding = clip_embedding.astype(np.float32)

        # 5. Set tensor and invoke TFLite interpreter
        interpreter.set_tensor(input_details[0]['index'], clip_embedding)
        interpreter.invoke()
        
        # 6. Retrieve output predictions
        output_data = interpreter.get_tensor(output_details[0]['index'])[0]
        
        # If output is quantized int8, dequantize it back to float probability (optional, depends on model configuration)
        if output_details[0]['dtype'] == np.int8:
            out_scale, out_zero_point = output_details[0]['quantization']
            output_data = (output_data.astype(np.float32) - out_zero_point) * out_scale

        # 7. Get top prediction
        top_class_idx = np.argmax(output_data)
        top_class_name = CLASS_NAMES[top_class_idx] if top_class_idx < len(CLASS_NAMES) else f"Class {top_class_idx}"
        confidence = float(output_data[top_class_idx])
        
        # Print results live
        print(f"Predicted: {top_class_name:<12} | Confidence: {confidence:.2f}")

except KeyboardInterrupt:
    print("\nStopping live inference...")
finally:
    stream.stop_stream()
    stream.close()
    p.terminate()