import os
from pydub import AudioSegment
from pydub.utils import make_chunks

def split_audio_by_duration(file_path, chunk_length_ms, output_dir="chunks"):
    """
    Splits a WAV file into fixed-duration chunks.
    
    :param file_path: Path to the input WAV file
    :param chunk_length_ms: Length of each chunk in milliseconds (e.g., 30000 for 30s)
    :param output_dir: Directory where chunks will be saved
    """
    # Create the output directory if it doesn't exist
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
        
    # Load the audio file
    print(f"Loading {file_path}...")
    audio = AudioSegment.from_wav(file_path)
    
    # Split audio into chunks
    chunks = make_chunks(audio, chunk_length_ms)
    
    # Export each chunk as a separate WAV file
    for i, chunk in enumerate(chunks):
        chunk_name = os.path.join(output_dir, f"chunk_{i}.wav")
        print(f"Exporting {chunk_name}")
        chunk.export(chunk_name, format="wav")

# Example Usage: Split a file into 30-second chunks (30,000 milliseconds)
split_audio_by_duration("audio/bg_01.wav", chunk_length_ms=30000)
