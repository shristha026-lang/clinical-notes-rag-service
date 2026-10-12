package dev.shristi.rag.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "OPENAI_API_KEY=")
@AutoConfigureMockMvc
class NotesControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void answersFromSyntheticNotesLoadedAtStartup() throws Exception {
        mvc.perform(post("/api/v1/ask").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What is the time in range on the glucose monitor?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("extractive-offline"))
                .andExpect(jsonPath("$.sources[0].noteId").value("note-004"));
    }

    @Test
    void rejectsBlankQuestion() throws Exception {
        mvc.perform(post("/api/v1/ask").contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void ingestsANote() throws Exception {
        mvc.perform(post("/api/v1/notes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"t1\",\"text\":\"Synthetic test note about hydration.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.chunks").value(1));
    }
}
