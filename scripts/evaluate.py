#!/usr/bin/env python3
"""Evaluate the TFLite model on the normalized dataset.

Usage:
    python scripts/evaluate.py
    python scripts/evaluate.py --model models/whistle_detector_end_to_end.tflite
    python scripts/evaluate.py --file path/to/clip.wav
"""

import argparse
import sys
from pathlib import Path

# Add project root to path for direct script execution
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from whistl.config import MODEL_E2E_TFLITE_PATH, NORMALIZED_DATA_DIR
from whistl.inference.file_detector import batch_evaluate, run_file_inference


def main() -> None:
    parser = argparse.ArgumentParser(description="Evaluate the whistle detector TFLite model.")
    parser.add_argument(
        "--model",
        type=Path,
        default=MODEL_E2E_TFLITE_PATH,
        help="Path to the TFLite model (default: models/whistle_detector_end_to_end.tflite).",
    )
    parser.add_argument(
        "--dataset",
        type=Path,
        default=NORMALIZED_DATA_DIR,
        help="Labeled dataset directory for batch evaluation (default: data/normalized/).",
    )
    parser.add_argument(
        "--file",
        type=Path,
        metavar="AUDIO_FILE",
        help="Single WAV file to run inference on instead of a full batch evaluation.",
    )
    args = parser.parse_args()

    if args.file:
        result = run_file_inference(args.file, model_path=args.model)
        print(f"\nFile   : {args.file}")
        print(f"Result : {result['prediction']}  (whistle={result['whistle']:.4f}, bg={result['background']:.4f})")
        print(f"Detected: {'YES 🔔' if result['detected'] else 'no'}")
    else:
        batch_evaluate(dataset_dir=args.dataset, model_path=args.model)


if __name__ == "__main__":
    main()
