"""File-based inference using the end-to-end TFLite model."""

from pathlib import Path

import librosa
import numpy as np
import tensorflow as tf
from sklearn.metrics import classification_report

from whistl.config import (
    CLASS_NAMES,
    MODEL_E2E_TFLITE_PATH,
    NORMALIZED_DATA_DIR,
    SAMPLE_RATE,
    CHUNK_SIZE,
)


def _load_interpreter(model_path: Path) -> tuple[tf.lite.Interpreter, dict, dict]:
    """Load TFLite interpreter and return (interpreter, input_details, output_details)."""
    interpreter = tf.lite.Interpreter(model_path=str(model_path))
    interpreter.allocate_tensors()
    return (
        interpreter,
        interpreter.get_input_details()[0],
        interpreter.get_output_details()[0],
    )


def run_file_inference(
    audio_path: Path,
    model_path: Path = MODEL_E2E_TFLITE_PATH,
    threshold: float = 0.85,
) -> dict:
    """Run the end-to-end TFLite model on a single WAV file.

    Args:
        audio_path: Path to a WAV audio clip (any sample rate — resampled to 16 kHz).
        model_path: Path to the end-to-end TFLite model.
        threshold: Confidence threshold for binary detection label.

    Returns:
        dict with keys: ``background``, ``whistle`` (float probabilities),
        ``prediction`` (class name), ``detected`` (bool).
    """
    audio_path = Path(audio_path)
    model_path = Path(model_path)

    waveform, _ = librosa.load(str(audio_path), sr=SAMPLE_RATE, mono=True)
    waveform = waveform.astype(np.float32)

    # Pad or trim to exactly CHUNK_SIZE samples
    if len(waveform) < CHUNK_SIZE:
        waveform = np.pad(waveform, (0, CHUNK_SIZE - len(waveform)))
    else:
        waveform = waveform[:CHUNK_SIZE]

    interpreter, in_det, out_det = _load_interpreter(model_path)
    interpreter.set_tensor(in_det["index"], waveform)
    interpreter.invoke()

    probs: np.ndarray = interpreter.get_tensor(out_det["index"])
    if probs.ndim > 1:
        probs = probs[0]

    bg_conf = float(probs[CLASS_NAMES.index("background")])
    wh_conf = float(probs[CLASS_NAMES.index("whistle")])
    pred_idx = int(np.argmax(probs))

    return {
        "background": bg_conf,
        "whistle": wh_conf,
        "prediction": CLASS_NAMES[pred_idx],
        "detected": wh_conf >= threshold,
    }


def batch_evaluate(
    dataset_dir: Path = NORMALIZED_DATA_DIR,
    model_path: Path = MODEL_E2E_TFLITE_PATH,
    threshold: float = 0.85,
) -> dict:
    """Evaluate the TFLite model against a labeled directory structure.

    Expected layout::

        dataset_dir/
            background/   *.wav
            whistle/      *.wav
        (or dataset_dir/train/...)

    Args:
        dataset_dir: Root directory with per-class sub-folders.
        model_path: Path to the end-to-end TFLite model.
        threshold: Whistle confidence threshold.

    Returns:
        dict with keys ``report`` (str sklearn classification report),
        ``y_true`` and ``y_pred`` (lists of ints).
    """
    dataset_dir = Path(dataset_dir)
    if (dataset_dir / "train").is_dir():
        dataset_dir = dataset_dir / "train"
    model_path = Path(model_path)

    interpreter, in_det, out_det = _load_interpreter(model_path)

    y_true: list[int] = []
    y_pred: list[int] = []

    for class_name in CLASS_NAMES:
        class_dir = dataset_dir / class_name
        if not class_dir.is_dir():
            print(f"[warn] Directory not found, skipping: {class_dir}")
            continue
        true_idx = CLASS_NAMES.index(class_name)
        wav_files = sorted(class_dir.glob("*.wav"))
        print(f"Evaluating {len(wav_files)} files from '{class_name}'...")

        for wav_path in wav_files:
            try:
                waveform, _ = librosa.load(str(wav_path), sr=SAMPLE_RATE, mono=True)
                waveform = waveform.astype(np.float32)
                if len(waveform) < CHUNK_SIZE:
                    waveform = np.pad(waveform, (0, CHUNK_SIZE - len(waveform)))
                else:
                    waveform = waveform[:CHUNK_SIZE]

                interpreter.set_tensor(in_det["index"], waveform)
                interpreter.invoke()
                probs: np.ndarray = interpreter.get_tensor(out_det["index"])
                if probs.ndim > 1:
                    probs = probs[0]
                pred_idx = int(np.argmax(probs))

                y_true.append(true_idx)
                y_pred.append(pred_idx)
            except Exception as exc:
                print(f"  [error] {wav_path.name}: {exc}")

    report = classification_report(y_true, y_pred, target_names=CLASS_NAMES)
    print(report)
    return {"report": report, "y_true": y_true, "y_pred": y_pred}
