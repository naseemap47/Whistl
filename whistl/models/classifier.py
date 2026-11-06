import tensorflow as tf

def build_whistle_classifier_head(num_classes: int = 2) -> tf.keras.Model:
    """
    Builds the classifier neural network taking 1024-dim YAMNet embeddings as input.
    """
    model = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(1024,), name="yamnet_embedding"),
        tf.keras.layers.BatchNormalization(),
        tf.keras.layers.Dense(512, activation="relu"),
        tf.keras.layers.Dropout(0.3),
        tf.keras.layers.Dense(256, activation="relu"),
        tf.keras.layers.Dropout(0.3),
        tf.keras.layers.Dense(num_classes, activation="softmax", name="probabilities")
    ])
    return model
