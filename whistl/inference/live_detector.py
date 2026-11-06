"""Real-time microphone inference using the end-to-end TFLite model."""

from pathlib import Path

import numpy as np
import pyaudio
import tensorflow as tf

from whistl.config import (
    CHUNK_SIZE,
    CLASS_NAMES,
    MODEL_E2E_TFLITE_PATH,
    SAMPLE_RATE,
)


def run_live_detection(
    model_path: Path = MODEL_E2E_TFLITE_PATH,
    threshold: float = 0.85,
) -> None:
    """Stream microphone audio and print whistle detection results in real time.

    Args:
        model_path: Path to the end-to-end TFLite model.
        threshold: Confidence threshold above which a detection is printed in bold.
    """
    model_path = Path(model_path)
    if not model_path.exists():
        raise FileNotFoundError(f"TFLite model not found: {model_path}")

    print(f"Loading TFLite model from {model_path} ...")
    interpreter = tf.lite.Interpreter(model_path=str(model_path))
    interpreter.allocate_tensors()

    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    print(f"  Input  shape: {input_details[0]['shape']}, dtype: {input_details[0]['dtype'].__name__}")
    print(f"  Output shape: {output_details[0]['shape']}, dtype: {output_details[0]['dtype'].__name__}")

    p = pyaudio.PyAudio()
    stream = p.open(
        format=pyaudio.paInt16,
        channels=1,
        rate=SAMPLE_RATE,
        input=True,
        frames_per_buffer=CHUNK_SIZE,
    )

    print(f"\n--- Live Whistle Detection (threshold={threshold}) ---")
    print("Listening… Press Ctrl+C to stop.\n")

    try:
        while True:
            raw = stream.read(CHUNK_SIZE, exception_on_overflow=False)
            audio = np.frombuffer(raw, dtype=np.int16).astype(np.float32) / 32768.0

            interpreter.set_tensor(input_details[0]["index"], audio)
            interpreter.invoke()

            probs: np.ndarray = interpreter.get_tensor(output_details[0]["index"])
            # Output may be shape [2] or [1,2]
            if probs.ndim > 1:
                probs = probs[0]

            whistle_conf = float(probs[CLASS_NAMES.index("whistle")])
            detected = whistle_conf >= threshold
            tag = "🔔 WHISTLE" if detected else "   "
            print(f"{tag}  whistle={whistle_conf:.3f}  bg={probs[CLASS_NAMES.index('background')]:.3f}")

    except KeyboardInterrupt:
        print("\nStopped.")
    finally:
        stream.stop_stream()
        stream.close()
        p.terminate()
