package dev.shristi.rag.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Offline stand-in for an LLM: returns the context sentences that share the
 * most terms with the question. Deterministic, so tests and demos run without
 * a network call or API key.
 */
public class ExtractiveLlmClient implements LlmClient {

    private static final double RELATIVE_SCORE_CUTOFF = 0.6;

    private final int maxSentences;

    public ExtractiveLlmClient(int maxSentences) {
        this.maxSentences = maxSentences;
    }

    @Override
    public String answer(String question, List<Model.ScoredChunk> context) {
        if (context.isEmpty()) {
            return "No relevant information was found in the indexed notes.";
        }
        Set<String> questionTerms = terms(question);
        record Candidate(String sentence, long overlap, int order) {
        }
        List<Candidate> candidates = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int order = 0;
        // Only answer from chunks close to the best match, so weakly related notes do not leak in.
        double cutoff = context.get(0).score() * RELATIVE_SCORE_CUTOFF;
        for (Model.ScoredChunk scored : context) {
            if (scored.score() < cutoff) {
                continue;
            }
            for (String sentence : scored.chunk().text().split("(?<=[.!?])\\s+")) {
                String s = sentence.trim();
                if (s.isEmpty() || !seen.add(s)) {
                    continue;
                }
                long overlap = terms(s).stream().filter(questionTerms::contains).count();
                candidates.add(new Candidate(s, overlap, order++));
            }
        }
        List<String> picked = candidates.stream()
                .filter(c -> c.overlap() > 0)
                .sorted(Comparator.comparingLong(Candidate::overlap).reversed().thenComparingInt(Candidate::order))
                .limit(maxSentences)
                .map(Candidate::sentence)
                .toList();
        return picked.isEmpty() ? "No relevant information was found in the indexed notes." : String.join(" ", picked);
    }

    private static Set<String> terms(String text) {
        Set<String> result = new HashSet<>(Arrays.asList(
                text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").trim().split("\\s+")));
        result.removeIf(t -> t.length() < 3);
        return result;
    }

    @Override
    public String name() {
        return "extractive-offline";
    }
}
