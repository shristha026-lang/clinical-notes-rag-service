package dev.shristi.rag.core;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brute-force cosine search held in memory. Fine for thousands of chunks;
 * replace with pgvector, OpenSearch or Pinecone behind the same interface
 * for production volumes.
 */
public class InMemoryVectorStore implements VectorStore {

    private record Entry(Model.Chunk chunk, float[] vector) {
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    @Override
    public void upsert(Model.Chunk chunk, float[] vector) {
        entries.put(chunk.id(), new Entry(chunk, vector.clone()));
    }

    @Override
    public List<Model.ScoredChunk> search(float[] query, int topK, double minScore) {
        if (topK <= 0) {
            throw new IllegalArgumentException("topK must be positive");
        }
        return entries.values().stream()
                .map(e -> new Model.ScoredChunk(e.chunk(), cosine(query, e.vector())))
                .filter(s -> s.score() >= minScore)
                .sorted(Comparator.comparingDouble(Model.ScoredChunk::score).reversed()
                        .thenComparing(s -> s.chunk().id()))
                .limit(topK)
                .toList();
    }

    static double cosine(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("vector dimensions differ: " + a.length + " vs " + b.length);
        }
        double dot = 0;
        double na = 0;
        double nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) {
            return 0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    @Override
    public int size() {
        return entries.size();
    }
}
