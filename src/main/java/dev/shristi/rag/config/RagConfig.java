package dev.shristi.rag.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.shristi.rag.adapters.OpenAiChatClient;
import dev.shristi.rag.core.Chunker;
import dev.shristi.rag.core.Embedder;
import dev.shristi.rag.core.ExtractiveLlmClient;
import dev.shristi.rag.core.HashingEmbedder;
import dev.shristi.rag.core.InMemoryVectorStore;
import dev.shristi.rag.core.LlmClient;
import dev.shristi.rag.core.PhiRedactor;
import dev.shristi.rag.core.RagService;
import dev.shristi.rag.core.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

    private static final Logger LOG = LoggerFactory.getLogger(RagConfig.class);

    @Bean
    Embedder embedder(@Value("${rag.embedding.dimensions:512}") int dimensions) {
        return new HashingEmbedder(dimensions);
    }

    @Bean
    VectorStore vectorStore() {
        return new InMemoryVectorStore();
    }

    /**
     * Uses OpenAI when OPENAI_API_KEY is set, otherwise the offline extractive
     * client. The key is read from the environment and never logged.
     */
    @Bean
    LlmClient llmClient(@Value("${OPENAI_API_KEY:}") String apiKey,
                        @Value("${rag.llm.model:gpt-4o-mini}") String model,
                        ObjectMapper mapper) {
        if (apiKey != null && !apiKey.isBlank()) {
            LOG.info("LLM client: OpenAI ({})", model);
            return new OpenAiChatClient(apiKey, model, mapper);
        }
        LOG.info("LLM client: offline extractive (set OPENAI_API_KEY to use OpenAI)");
        return new ExtractiveLlmClient(3);
    }

    @Bean
    RagService ragService(Embedder embedder, VectorStore store, LlmClient llm,
                          @Value("${rag.chunk.words:60}") int chunkWords,
                          @Value("${rag.chunk.overlap:15}") int overlap,
                          @Value("${rag.retrieval.top-k:4}") int topK,
                          @Value("${rag.retrieval.min-score:0.05}") double minScore) {
        return new RagService(new Chunker(chunkWords, overlap), embedder, store, llm, new PhiRedactor(), topK, minScore);
    }
}
