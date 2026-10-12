package dev.shristi.rag.core;

import java.util.List;
import java.util.Objects;

/**
 * Domain records. The core package has no framework dependencies so the
 * retrieval logic can be unit-tested and reused outside Spring.
 */
public final class Model {

    private Model() {
    }

    /** A clinical note. All notes in this project are synthetic. */
    public record Note(String id, String patientRef, String type, String text) {
        public Note {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(text, "text");
            if (text.isBlank()) {
                throw new IllegalArgumentException("note text must not be blank");
            }
        }
    }

    /** A slice of a note that is embedded and indexed. */
    public record Chunk(String id, String noteId, int position, String text) {
    }

    /** A chunk returned by similarity search, with its cosine score. */
    public record ScoredChunk(Chunk chunk, double score) {
    }

    /** The answer returned to the caller, with the chunks it was grounded on. */
    public record Answer(String question, String answer, List<ScoredChunk> sources, String model) {
    }
}
