package dev.shristi.rag.api;

import dev.shristi.rag.core.Model;
import dev.shristi.rag.core.RagService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class NotesController {

    private final RagService rag;

    public NotesController(RagService rag) {
        this.rag = rag;
    }

    public record NoteRequest(@NotBlank String id, String patientRef, String type,
                              @NotBlank @Size(max = 20_000) String text) {
    }

    public record AskRequest(@NotBlank @Size(max = 1_000) String question) {
    }

    public record Source(String chunkId, String noteId, double score, String text) {
    }

    public record AskResponse(String answer, String model, List<Source> sources) {
    }

    @PostMapping("/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> ingest(@Valid @RequestBody NoteRequest request) {
        int chunks = rag.ingest(new Model.Note(request.id(), request.patientRef(), request.type(), request.text()));
        return Map.of("noteId", request.id(), "chunks", chunks);
    }

    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        Model.Answer answer = rag.ask(request.question());
        List<Source> sources = answer.sources().stream()
                .map(s -> new Source(s.chunk().id(), s.chunk().noteId(), Math.round(s.score() * 1000) / 1000.0, s.chunk().text()))
                .toList();
        return new AskResponse(answer.answer(), answer.model(), sources);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return Map.of("indexedChunks", rag.indexedChunks());
    }
}
