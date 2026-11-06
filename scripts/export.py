#!/usr/bin/env python3
"""Export the trained classifier as an end-to-end TFLite model.

Usage:
    python scripts/export.py
    python scripts/export.py --no-android-copy
    python scripts/export.py --quantized
"""

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from whistl.models.exporter import export_end_to_end_tflite, export_quantized_int8_tflite


def main() -> None:
    parser = argparse.ArgumentParser(description="Export the whistle classifier as a TFLite model.")
    parser.add_argument(
        "--no-android-copy",
        action="store_true",
        help="Skip copying the exported model to the Android assets directory.",
    )
    parser.add_argument(
        "--quantized",
        action="store_true",
        help="Also export an INT8 quantized TFLite model.",
    )
    args = parser.parse_args()

    copy_to_android = not args.no_android_copy
    export_end_to_end_tflite(copy_to_android=copy_to_android)

    if args.quantized:
        export_quantized_int8_tflite()


if __name__ == "__main__":
    main()
