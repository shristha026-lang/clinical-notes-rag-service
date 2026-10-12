package dev.shristi.rag.core;

/** Turns text into a fixed-length vector. */
public interface Embedder {

    float[] embed(String text);

    int dimensions();

    String name();
}
