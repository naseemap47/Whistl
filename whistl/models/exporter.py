import shutil
from pathlib import Path
import numpy as np
import tensorflow as tf
import tensorflow_hub as hub

from whistl.config import (
    MODEL_H5_PATH,
    MODEL_E2E_TFLITE_PATH,
    MODEL_INT8_TFLITE_PATH,
    ANDROID_ASSETS_MODEL_PATH,
    CHUNK_SIZE,
    YAMNET_HUB_URL,
)


def export_end_to_end_tflite(
    classifier_h5_path: Path = MODEL_H5_PATH,
    output_tflite_path: Path = MODEL_E2E_TFLITE_PATH,
    copy_to_android: bool = True
) -> Path:
    """
    Fuses Google YAMNet with the trained classifier into a single TFLite model
    accepting float32[15600] audio waveform and outputting float32[2] probabilities.
    """
    print(f"Loading YAMNet from {YAMNET_HUB_URL}...")
    yamnet = hub.load(YAMNET_HUB_URL)
    print(f"Loading trained classifier from {classifier_h5_path}...")
    classifier = tf.keras.models.load_model(str(classifier_h5_path))

    class EndToEndWhistleModel(tf.Module):
        def __init__(self, yamnet_model, classifier_model):
            super().__init__()
            self.yamnet = yamnet_model
            self.classifier = classifier_model

        @tf.function(input_signature=[tf.TensorSpec(shape=[CHUNK_SIZE], dtype=tf.float32, name="waveform")])
        def __call__(self, waveform):
            _, embeddings, _ = self.yamnet(waveform)
            mean_embedding = tf.reduce_mean(embeddings, axis=0, keepdims=True)
            probs = self.classifier(mean_embedding)
            return {"probabilities": probs[0]}

    e2e = EndToEndWhistleModel(yamnet, classifier)
    concrete_fn = e2e.__call__.get_concrete_function()

    converter = tf.lite.TFLiteConverter.from_concrete_functions([concrete_fn])
    tflite_model = converter.convert()

    output_tflite_path.parent.mkdir(parents=True, exist_ok=True)
    with open(output_tflite_path, "wb") as f:
        f.write(tflite_model)
    print(f"✅ Exported end-to-end TFLite model to: {output_tflite_path} ({len(tflite_model)} bytes)")

    if copy_to_android:
        ANDROID_ASSETS_MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(str(output_tflite_path), str(ANDROID_ASSETS_MODEL_PATH))
        print(f"📱 Synced to Android assets: {ANDROID_ASSETS_MODEL_PATH}")

    return output_tflite_path


def export_quantized_int8_tflite(
    classifier_h5_path: Path = MODEL_H5_PATH,
    output_tflite_path: Path = MODEL_INT8_TFLITE_PATH
) -> Path:
    """
    Exports a quantized INT8 model of the classifier head (expects 1024-dim embedding).
    """
    print(f"Loading classifier from {classifier_h5_path}...")
    model = tf.keras.models.load_model(str(classifier_h5_path))

    def representative_dataset_gen():
        for _ in range(100):
            yield [np.random.randn(1, 1024).astype(np.float32)]

    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.representative_dataset = representative_dataset_gen
    converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
    converter.inference_input_type = tf.int8
    converter.inference_output_type = tf.float32

    tflite_model = converter.convert()
    output_tflite_path.parent.mkdir(parents=True, exist_ok=True)
    with open(output_tflite_path, "wb") as f:
        f.write(tflite_model)
    print(f"✅ Exported quantized INT8 TFLite model to: {output_tflite_path} ({len(tflite_model)} bytes)")
    return output_tflite_path
