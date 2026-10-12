package dev.shristi.rag.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.shristi.rag.core.Model;
import dev.shristi.rag.core.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/** Loads the synthetic notes at startup so the API is usable immediately. */
@Component
@ConditionalOnProperty(name = "rag.sample-data.enabled", havingValue = "true", matchIfMissing = true)
public class SampleDataLoader implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SampleDataLoader.class);

    private final RagService rag;
    private final ObjectMapper mapper;

    public SampleDataLoader(RagService rag, ObjectMapper mapper) {
        this.rag = rag;
        this.mapper = mapper;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        try (InputStream in = new ClassPathResource("data/synthetic-notes.json").getInputStream()) {
            List<Model.Note> notes = mapper.readValue(in, new TypeReference<List<Model.Note>>() { });
            int chunks = notes.stream().mapToInt(rag::ingest).sum();
            LOG.info("Loaded {} synthetic notes as {} chunks", notes.size(), chunks);
        }
    }
}
