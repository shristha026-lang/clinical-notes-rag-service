package dev.shristi.rag.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits text into overlapping word windows. Overlap keeps a sentence that
 * straddles a boundary retrievable from either chunk.
 */
public class Chunker {

    private final int chunkWords;
    private final int overlapWords;

    public Chunker(int chunkWords, int overlapWords) {
        if (chunkWords <= 0) {
            throw new IllegalArgumentException("chunkWords must be positive");
        }
        if (overlapWords < 0 || overlapWords >= chunkWords) {
            throw new IllegalArgumentException("overlapWords must be >= 0 and < chunkWords");
        }
        this.chunkWords = chunkWords;
        this.overlapWords = overlapWords;
    }

    public List<Model.Chunk> chunk(Model.Note note) {
        String[] words = note.text().trim().split("\\s+");
        List<Model.Chunk> chunks = new ArrayList<>();
        int step = chunkWords - overlapWords;
        int position = 0;
        for (int start = 0; start < words.length; start += step) {
            int end = Math.min(words.length, start + chunkWords);
            String text = String.join(" ", java.util.Arrays.copyOfRange(words, start, end));
            chunks.add(new Model.Chunk(note.id() + "#" + position, note.id(), position, text));
            position++;
            if (end == words.length) {
                break;
            }
        }
        return chunks;
    }
}
