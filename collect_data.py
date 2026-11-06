import sounddevice as sd
import numpy as np
import scipy.io.wavfile as wav
import os
import time

# ---------- Configuration ----------
SAMPLE_RATE = 16000        # YAMNet requires 16 kHz
DURATION = 3.0             # seconds per clip
OUTPUT_DIR = "dataset"
LABELS = ["whistle", "background"]   # classes to record
CLIPS_PER_LABEL = 30       # number of clips to record per label

os.makedirs(OUTPUT_DIR, exist_ok=True)

def record_clip(label, index):
    """Record a single audio clip and save it as a WAV file."""
    print(f"\n🎙️ Recording '{label}' clip {index + 1}/{CLIPS_PER_LABEL}...")
    print("Recording in 2 seconds... Get ready!")
    time.sleep(2)
    print("🔴 Recording now...")

    audio = sd.rec(
        int(DURATION * SAMPLE_RATE),
        samplerate=SAMPLE_RATE,
        channels=1,
        dtype='float32'
    )
    sd.wait()
    print("✅ Done.")

    # Normalize to [-1.0, 1.0] for YAMNet compatibility
    max_val = np.max(np.abs(audio))
    if max_val > 0:
        audio = audio / max_val

    filename = os.path.join(
        OUTPUT_DIR, f"{label}_{index:03d}.wav"
    )
    wav.write(filename, SAMPLE_RATE, (audio * 32767).astype(np.int16))
    print(f"💾 Saved: {filename}")

def main():
    print("=" * 50)
    print("  Pressure Cooker Whistle — Data Collection")
    print("=" * 50)
    print("\nInstructions:")
    print("• For 'whistle': record actual cooker whistle bursts.")
    print("• For 'background': record kitchen noise (talking, frying, TV, etc.)")
    print("• Keep the microphone close to the cooker for whistle clips.\n")

    for label in LABELS:
        print(f"\n▶️  Starting collection for label: {label}")
        for i in range(CLIPS_PER_LABEL):
            record_clip(label, i)
            input("Press ENTER to record next clip (or Ctrl+C to stop)... ")

    print("\n🎉 Data collection complete! Files saved in:", OUTPUT_DIR)

if __name__ == "__main__":
    main()