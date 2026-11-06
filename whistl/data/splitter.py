from pathlib import Path
from pydub import AudioSegment
from pydub.utils import make_chunks
from whistl.config import RAW_DATA_DIR, PROCESSED_DATA_DIR, SAMPLE_RATE

def split_audio_file(
    file_path: Path,
    chunk_length_ms: int = 1000,
    output_dir: Path = PROCESSED_DATA_DIR / "background",
    prefix: str = "chunk"
) -> list[Path]:
    """
    Split a long WAV audio recording into fixed-duration chunks resampled to 16 kHz mono.
    """
    output_dir.mkdir(parents=True, exist_ok=True)

    print(f"Loading {file_path}...")
    audio = AudioSegment.from_wav(str(file_path))

    # Resample to 16kHz mono
    audio = audio.set_frame_rate(SAMPLE_RATE).set_channels(1)

    chunks = make_chunks(audio, chunk_length_ms)
    saved_paths = []

    for i, chunk in enumerate(chunks):
        chunk_name = output_dir / f"{prefix}_{file_path.stem}_{i:04d}.wav"
        chunk.export(str(chunk_name), format="wav")
        saved_paths.append(chunk_name)

    print(f"Exported {len(saved_paths)} chunks to {output_dir}")
    return saved_paths
