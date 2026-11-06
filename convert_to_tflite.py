import numpy as np
import tensorflow as tf

# 1. Load your trained Keras model
MODEL_PATH = "whistle_classifier.h5"  # Replace with your .h5 file path
TFLITE_PATH = "whistle_classifier_int8.tflite"

print(f"Loading model from {MODEL_PATH}...")
model = tf.keras.models.load_model(MODEL_PATH)

# 2. Define a representative dataset generator function
# This should yield batches of input data matching what your model expects during training.
# Since your whistle model takes 1024-dim YAMNet embeddings, we generate dummy or real sample embeddings.
# (If your model takes raw audio or another shape, adjust the shape accordingly).
def representative_data_gen():
    # Provide 100-200 representative samples from your training set (shape: [1, 1024])
    # For demonstration, we use random normal data matching your input shape.
    # Ideally, replace this loop with real numpy arrays extracted from your actual training data.
    for _ in range(100):
        # Example shape: (1, 1024) for your dense classifier head
        dummy_input = np.random.randn(1, 1024).astype(np.float32)
        yield [dummy_input]

# 3. Initialize the TFLite Converter
converter = tf.lite.TFLiteConverter.from_keras_model(model)

# 4. Configure optimization and full integer quantization flags
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.representative_dataset = representative_data_gen

# Enforce full integer quantization (throws an error if any operation cannot be quantized to int8)
converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]

# Set input and output types to int8 (or keep float/int depending on your Android app preference)
converter.inference_input_type = tf.int8  # or tf.float32 if you prefer feeding float from Android
converter.inference_output_type = tf.float32

print("Converting model to INT8 TFLite...")
tflite_model = converter.convert()

# 5. Save the quantized .tflite model
with open(TFLITE_PATH, "wb") as f:
    f.write(tflite_model)

print(f"✅ Success! Quantized TFLite model saved to: {TFLITE_PATH}")