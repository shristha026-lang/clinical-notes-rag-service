package dev.shristi.rag.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoreTest {

    private RagService newService() {
        return new RagService(new Chunker(20, 5), new HashingEmbedder(256), new InMemoryVectorStore(),
                new ExtractiveLlmClient(2), new PhiRedactor(), 3, 0.0);
    }

    @Test
    void chunkerCoversAllWordsWithOverlap() {
        String text = String.join(" ", java.util.Collections.nCopies(45, "word"));
        List<Model.Chunk> chunks = new Chunker(20, 5).chunk(new Model.Note("n1", "p", "t", text));
        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0).id()).isEqualTo("n1#0");
        assertThat(chunks.get(2).text().split(" ")).hasSize(15);
    }

    @Test
    void chunkerRejectsInvalidOverlap() {
        assertThatThrownBy(() -> new Chunker(10, 10)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void redactorMasksIdentifiers() {
        String out = new PhiRedactor().redact("Call (555) 010-2233, SSN 123-45-6789, MRN 00482913, a@b.com on 03/14/2026");
        assertThat(out).contains("[PHONE]", "[SSN]", "[MRN]", "[EMAIL]", "[DATE]")
                .doesNotContain("123-45-6789", "a@b.com", "00482913");
    }

    @Test
    void cosineOfIdenticalVectorsIsOne() {
        float[] v = new HashingEmbedder(64).embed("asthma inhaler peak flow");
        assertThat(InMemoryVectorStore.cosine(v, v)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-6));
    }

    @Test
    void retrievesTheRelevantNoteFirst() {
        RagService rag = newService();
        rag.ingest(new Model.Note("asthma", "p1", "progress", "Asthma controlled on inhaled fluticasone. Peak flow stable."));
        rag.ingest(new Model.Note("knee", "p2", "therapy", "Knee flexion improved to 100 degrees after surgery."));
        rag.ingest(new Model.Note("feeding", "p3", "progress", "Infant weight gain meets goal with breastfeeding."));

        Model.Answer answer = rag.ask("How is the knee flexion after surgery?");

        assertThat(answer.sources()).isNotEmpty();
        assertThat(answer.sources().get(0).chunk().noteId()).isEqualTo("knee");
        assertThat(answer.answer()).contains("flexion");
    }

    @Test
    void redactsBeforeIndexing() {
        RagService rag = newService();
        rag.ingest(new Model.Note("n", "p", "triage", "Rash reported. Test identifier 123-45-6789 for demo."));
        Model.Answer answer = rag.ask("rash identifier");
        assertThat(answer.sources().get(0).chunk().text()).contains("[SSN]").doesNotContain("123-45-6789");
    }

    @Test
    void blankQuestionIsRejected() {
        assertThatThrownBy(() -> newService().ask("  ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptyIndexReturnsNoInformationMessage() {
        assertThat(newService().ask("anything").answer()).startsWith("No relevant information");
    }
}
