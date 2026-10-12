package dev.shristi.rag.core;

import java.util.List;

/** Generates an answer from a question and retrieved context. */
public interface LlmClient {

    String answer(String question, List<Model.ScoredChunk> context);

    String name();
}
