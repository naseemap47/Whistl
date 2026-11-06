import tensorflow as tf
import numpy as np
import librosa
import glob

def test_exported_model():
    interpreter = tf.lite.Interpreter(model_path='whistle_detector_end_to_end.tflite')
    interpreter.allocate_tensors()
    inputs = interpreter.get_input_details()
    outputs = interpreter.get_output_details()

    whistle_files = glob.glob('dataset/whistle/*.wav') + glob.glob('dataset_norm/**/whistle/*.wav', recursive=True)
    assert len(whistle_files) > 0, "No whistle files found for testing"
    
    waveform, sr = librosa.load(whistle_files[0], sr=16000, mono=True)
    waveform = np.pad(waveform, (0, max(0, 15600 - len(waveform))))[:15600].astype(np.float32)

    interpreter.set_tensor(inputs[0]['index'], waveform)
    interpreter.invoke()
    preds = interpreter.get_tensor(outputs[0]['index'])
    
    whistle_prob = float(preds[1])
    print(f"Tested {whistle_files[0]} -> whistle confidence: {whistle_prob:.4f}")
    assert whistle_prob > 0.85, f"Expected whistle prob > 0.85, got {whistle_prob}"

    bg_files = glob.glob('dataset/background/*.wav') + glob.glob('dataset_norm/**/background/*.wav', recursive=True)
    if bg_files:
        bg_wave, _ = librosa.load(bg_files[0], sr=16000, mono=True)
        bg_wave = np.pad(bg_wave, (0, max(0, 15600 - len(bg_wave))))[:15600].astype(np.float32)
        interpreter.set_tensor(inputs[0]['index'], bg_wave)
        interpreter.invoke()
        bg_preds = interpreter.get_tensor(outputs[0]['index'])
        bg_whistle_prob = float(bg_preds[1])
        print(f"Tested {bg_files[0]} -> whistle confidence: {bg_whistle_prob:.4f}")
        assert bg_whistle_prob < 0.20, f"Expected bg whistle prob < 0.20, got {bg_whistle_prob}"

    print("Verification PASSED!")

if __name__ == '__main__':
    test_exported_model()
