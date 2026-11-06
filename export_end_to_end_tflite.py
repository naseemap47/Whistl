import tensorflow as tf
import tensorflow_hub as hub

def export_model():
    print("Loading YAMNet feature extractor...")
    yamnet = hub.load('https://tfhub.dev/google/yamnet/1')
    print("Loading custom whistle classifier head...")
    classifier = tf.keras.models.load_model('whistle_classifier.h5')

    class EndToEndWhistleModel(tf.Module):
        def __init__(self, yamnet_model, classifier_model):
            super().__init__()
            self.yamnet = yamnet_model
            self.classifier = classifier_model

        @tf.function(input_signature=[tf.TensorSpec(shape=[15600], dtype=tf.float32, name='waveform')])
        def __call__(self, waveform):
            _, embeddings, _ = self.yamnet(waveform)
            mean_embedding = tf.reduce_mean(embeddings, axis=0, keepdims=True)
            probs = self.classifier(mean_embedding)
            return {'probabilities': probs[0]}

    e2e = EndToEndWhistleModel(yamnet, classifier)
    concrete_fn = e2e.__call__.get_concrete_function()
    converter = tf.lite.TFLiteConverter.from_concrete_functions([concrete_fn])
    tflite_model = converter.convert()

    output_path = 'whistle_detector_end_to_end.tflite'
    with open(output_path, 'wb') as f:
        f.write(tflite_model)
    print(f"Exported end-to-end model to {output_path} ({len(tflite_model)} bytes)")

if __name__ == '__main__':
    export_model()
