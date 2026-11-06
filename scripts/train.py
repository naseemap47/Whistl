#!/usr/bin/env python3
"""Train the YAMNet-based whistle classifier.

Usage:
    python scripts/train.py
    python scripts/train.py --epochs 30 --lr 0.0005
"""

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from whistl.models.trainer import train_model


def main() -> None:
    parser = argparse.ArgumentParser(description="Train the whistle classifier head on YAMNet embeddings.")
    parser.add_argument(
        "--epochs",
        type=int,
        default=None,
        help="Number of training epochs (default: value in config).",
    )
    parser.add_argument(
        "--lr",
        type=float,
        default=None,
        dest="learning_rate",
        help="Learning rate (default: value in config).",
    )
    parser.add_argument(
        "--batch-size",
        type=int,
        default=None,
        help="Mini-batch size (default: value in config).",
    )
    args = parser.parse_args()

    kwargs: dict = {}
    if args.epochs is not None:
        kwargs["epochs"] = args.epochs
    if args.learning_rate is not None:
        kwargs["learning_rate"] = args.learning_rate
    if args.batch_size is not None:
        kwargs["batch_size"] = args.batch_size

    train_model(**kwargs)


if __name__ == "__main__":
    main()
