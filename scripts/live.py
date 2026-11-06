#!/usr/bin/env python3
"""Run real-time whistle detection from the microphone.

Usage:
    python scripts/live.py
    python scripts/live.py --model models/whistle_detector_end_to_end.tflite --threshold 0.80
"""

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from whistl.config import MODEL_E2E_TFLITE_PATH
from whistl.inference.live_detector import run_live_detection


def main() -> None:
    parser = argparse.ArgumentParser(description="Live microphone whistle detection.")
    parser.add_argument(
        "--model",
        type=Path,
        default=MODEL_E2E_TFLITE_PATH,
        help="Path to the TFLite model (default: models/whistle_detector_end_to_end.tflite).",
    )
    parser.add_argument(
        "--threshold",
        type=float,
        default=0.85,
        help="Whistle confidence threshold for detection (default: 0.85).",
    )
    args = parser.parse_args()
    run_live_detection(model_path=args.model, threshold=args.threshold)


if __name__ == "__main__":
    main()
