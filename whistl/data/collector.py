import os
import time
from pathlib import Path
import numpy as np
import scipy.io.wavfile as wav
import sounddevice as sd
from whistl.config import PROCESSED_DATA_DIR, SAMPLE_RATE

def record_clip(
    output_path: Path,
    duration: float = 3.0,
    sample_rate: int = SAMPLE_RATE
) -> None:
    """Record a single audio clip from the microphone and save it as normalized 16-bit PCM WAV."""
    output_path.parent.mkdir(parents=True, exist_ok=True)

    audio = sd.rec(
        int(duration * sample_rate),
        samplerate=sample_rate,
        channels=1,
        dtype='float32'
    )
    sd.wait()

    # Peak normalization
    max_val = np.max(np.abs(audio))
    if max_val > 0:
        audio = audio / max_val

    wav.write(str(output_path), sample_rate, (audio * 32767).astype(np.int16))


def interactive_collect(
    output_dir: Path = PROCESSED_DATA_DIR,
    labels: list[str] = ("whistle", "background"),
    clips_per_label: int = 20,
    duration: float = 3.0
) -> None:
    """CLI wizard to record labeled audio samples."""
    print("=" * 55)
    print("  Whistl: Interactive Microphone Audio Collection")
    print("=" * 55)
    print("Instructions:")
    print("• For 'whistle': record cooker steam whistle bursts.")
    print("• For 'background': record ambient kitchen noise (talking, frying, clatter).")

    for label in labels:
        target_dir = output_dir / label
        target_dir.mkdir(parents=True, exist_ok=True)
        print(f"\n▶️ Starting collection for: {label} ({clips_per_label} clips)")

        for i in range(clips_per_label):
            filename = target_dir / f"{label}_{i+1:03d}.wav"
            print(f"\n🎙️ Clip {i + 1}/{clips_per_label} ready...")
            print("Recording in 1 second...")
            time.sleep(1)
            print("🔴 Recording now...")
            record_clip(filename, duration=duration)
            print(f"💾 Saved: {filename}")
            if i < clips_per_label - 1:
                input("Press ENTER to record next (or Ctrl+C to stop)... ")

    print(f"\n🎉 Collection complete! Files saved in: {output_dir}")
