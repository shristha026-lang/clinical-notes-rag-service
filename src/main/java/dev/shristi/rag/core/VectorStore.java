package dev.shristi.rag.core;

import java.util.List;

/** Stores chunk vectors and returns the closest matches to a query vector. */
public interface VectorStore {

    void upsert(Model.Chunk chunk, float[] vector);

    List<Model.ScoredChunk> search(float[] query, int topK, double minScore);

    int size();
}
