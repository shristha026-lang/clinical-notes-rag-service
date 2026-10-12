package dev.shristi.rag.core;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Orchestrates the RAG flow: redact, chunk, embed, index; then for a question,
 * embed, retrieve the top chunks and ask the LLM client to answer from them.
 */
public class RagService {

    private static final Logger LOG = Logger.getLogger(RagService.class.getName());

    private final Chunker chunker;
    private final Embedder embedder;
    private final VectorStore store;
    private final LlmClient llm;
    private final PhiRedactor redactor;
    private final int topK;
    private final double minScore;

    public RagService(Chunker chunker, Embedder embedder, VectorStore store, LlmClient llm,
                      PhiRedactor redactor, int topK, double minScore) {
        this.chunker = Objects.requireNonNull(chunker);
        this.embedder = Objects.requireNonNull(embedder);
        this.store = Objects.requireNonNull(store);
        this.llm = Objects.requireNonNull(llm);
        this.redactor = Objects.requireNonNull(redactor);
        this.topK = topK;
        this.minScore = minScore;
    }

    /** Indexes a note and returns how many chunks were stored. */
    public int ingest(Model.Note note) {
        Model.Note clean = new Model.Note(note.id(), note.patientRef(), note.type(), redactor.redact(note.text()));
        List<Model.Chunk> chunks = chunker.chunk(clean);
        for (Model.Chunk chunk : chunks) {
            store.upsert(chunk, embedder.embed(chunk.text()));
        }
        LOG.fine(() -> "Indexed note " + note.id() + " as " + chunks.size() + " chunks");
        return chunks.size();
    }

    public Model.Answer ask(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be blank");
        }
        String cleanQuestion = redactor.redact(question);
        List<Model.ScoredChunk> hits = store.search(embedder.embed(cleanQuestion), topK, minScore);
        String answer = llm.answer(cleanQuestion, hits);
        return new Model.Answer(cleanQuestion, answer, hits, llm.name());
    }

    public int indexedChunks() {
        return store.size();
    }
}
