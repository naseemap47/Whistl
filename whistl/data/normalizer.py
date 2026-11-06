import shutil
from pathlib import Path
from whistl.config import PROCESSED_DATA_DIR, NORMALIZED_DATA_DIR, CLASS_NAMES

def balance_and_normalize_dataset(
    source_dir: Path = PROCESSED_DATA_DIR,
    target_dir: Path = NORMALIZED_DATA_DIR / "train",
    samples_per_class: int = 30
) -> None:
    """
    Balance dataset by copying up to `samples_per_class` WAV files per category.
    """
    target_dir.mkdir(parents=True, exist_ok=True)

    for class_name in CLASS_NAMES:
        class_src = source_dir / class_name
        class_dst = target_dir / class_name
        class_dst.mkdir(parents=True, exist_ok=True)

        if not class_src.exists():
            print(f"⚠️ Source folder not found: {class_src}")
            continue

        wav_files = sorted(list(class_src.glob("*.wav")))
        selected = wav_files[:samples_per_class]

        for file_path in selected:
            shutil.copy2(str(file_path), str(class_dst / file_path.name))

        print(f"Copied {len(selected)}/{len(wav_files)} files for '{class_name}' to {class_dst}")
