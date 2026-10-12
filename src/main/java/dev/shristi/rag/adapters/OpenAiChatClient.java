package dev.shristi.rag.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.shristi.rag.core.LlmClient;
import dev.shristi.rag.core.Model;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Minimal OpenAI Chat Completions client using java.net.http. The prompt
 * restricts the model to the retrieved context and asks it to say when the
 * context does not contain the answer.
 */
public class OpenAiChatClient implements LlmClient {

    private static final URI ENDPOINT = URI.create("https://api.openai.com/v1/chat/completions");
    private static final String SYSTEM_PROMPT = """
            You answer questions about clinical notes using ONLY the numbered context.
            Cite the context numbers you used, like [1]. If the context does not contain
            the answer, say so. Do not give medical advice.""";

    private final String apiKey;
    private final String model;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public OpenAiChatClient(String apiKey, String model, ObjectMapper mapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.mapper = mapper;
    }

    @Override
    public String answer(String question, List<Model.ScoredChunk> context) {
        String numbered = java.util.stream.IntStream.range(0, context.size())
                .mapToObj(i -> "[" + (i + 1) + "] " + context.get(i).chunk().text())
                .collect(Collectors.joining("\n"));
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", "Context:\n" + numbered + "\n\nQuestion: " + question)));
        try {
            HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new LlmUnavailableException("OpenAI returned HTTP " + response.statusCode());
            }
            JsonNode json = mapper.readTree(response.body());
            return json.path("choices").path(0).path("message").path("content").asText();
        } catch (IOException e) {
            throw new LlmUnavailableException("OpenAI request failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmUnavailableException("OpenAI request interrupted");
        }
    }

    @Override
    public String name() {
        return "openai:" + model;
    }

    public static class LlmUnavailableException extends RuntimeException {
        public LlmUnavailableException(String message) {
            super(message);
        }
    }
}
