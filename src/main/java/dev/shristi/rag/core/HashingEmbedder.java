package dev.shristi.rag.core;

import java.util.Locale;
import java.util.Set;

/**
 * Offline embedder using the hashing trick over word unigrams and bigrams.
 * It needs no API key, so the service and its tests run anywhere. Swap in a
 * model-based embedder (for example OpenAI text-embedding-3-small) for
 * semantic quality.
 */
public class HashingEmbedder implements Embedder {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "of", "to", "in", "on", "for", "with", "is", "was", "are",
            "were", "be", "by", "at", "as", "it", "this", "that", "has", "had", "have", "patient", "pt");

    private final int dimensions;

    public HashingEmbedder(int dimensions) {
        if (dimensions < 16) {
            throw new IllegalArgumentException("dimensions must be >= 16");
        }
        this.dimensions = dimensions;
    }

    @Override
    public float[] embed(String text) {
        float[] vector = new float[dimensions];
        String[] tokens = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").trim().split("\\s+");
        String previous = null;
        for (String token : tokens) {
            if (token.isBlank() || STOP_WORDS.contains(token)) {
                previous = null;
                continue;
            }
            add(vector, token, 1.0f);
            if (previous != null) {
                add(vector, previous + "_" + token, 0.5f);
            }
            previous = token;
        }
        normalize(vector);
        return vector;
    }

    private void add(float[] vector, String feature, float weight) {
        int hash = feature.hashCode();
        int index = Math.floorMod(hash, dimensions);
        float sign = (hash >>> 31) == 0 ? 1f : -1f;
        vector[index] += sign * weight;
    }

    private static void normalize(float[] vector) {
        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        if (norm == 0) {
            return;
        }
        float inv = (float) (1.0 / Math.sqrt(norm));
        for (int i = 0; i < vector.length; i++) {
            vector[i] *= inv;
        }
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    @Override
    public String name() {
        return "hashing-" + dimensions;
    }
}
