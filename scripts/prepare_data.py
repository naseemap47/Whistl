#!/usr/bin/env python3
"""Prepare raw audio: split long recordings and normalize the dataset.

Usage:
    python scripts/prepare_data.py --split path/to/long_recording.wav --label whistle
    python scripts/prepare_data.py --normalize
    python scripts/prepare_data.py --split path/to/file.wav --label background --normalize
"""

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from whistl.data.splitter import split_audio_file
from whistl.data.normalizer import balance_and_normalize_dataset


def main() -> None:
    parser = argparse.ArgumentParser(description="Split and normalize audio training data.")
    parser.add_argument(
        "--split",
        type=Path,
        metavar="AUDIO_FILE",
        help="Long WAV file to split into clips.",
    )
    parser.add_argument(
        "--label",
        choices=["whistle", "background"],
        help="Class label for the split clips (required with --split).",
    )
    parser.add_argument(
        "--normalize",
        action="store_true",
        help="Balance and normalize the processed dataset into data/normalized/.",
    )
    args = parser.parse_args()

    if args.split:
        if not args.label:
            parser.error("--label is required when --split is specified.")
        split_audio_file(args.split, label=args.label)

    if args.normalize:
        balance_and_normalize_dataset()

    if not args.split and not args.normalize:
        parser.print_help()


if __name__ == "__main__":
    main()
