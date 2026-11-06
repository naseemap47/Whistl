#!/usr/bin/env python3
"""Record labeled audio clips interactively.

Usage:
    python scripts/record.py --label whistle --count 20
    python scripts/record.py --label background --count 20
"""

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from whistl.data.collector import interactive_collect


def main() -> None:
    parser = argparse.ArgumentParser(description="Record labeled audio clips for training.")
    parser.add_argument(
        "--label",
        required=True,
        choices=["whistle", "background"],
        help="Class label for the recorded clips.",
    )
    parser.add_argument(
        "--count",
        type=int,
        default=20,
        help="Number of clips to record (default: 20).",
    )
    args = parser.parse_args()
    interactive_collect(label=args.label, n_clips=args.count)


if __name__ == "__main__":
    main()
