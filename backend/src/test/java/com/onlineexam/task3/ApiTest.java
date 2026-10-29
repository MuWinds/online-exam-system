package com.onlineexam.task3;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("demo")
class ApiTest {
    @Autowired MockMvc mvc;
    @Test void rejectsMissingAndForgedIdentity() throws Exception {
        mvc.perform(post("/api/exams/1/start")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/exams/1/start").header("Authorization","Bearer student-999")).andExpect(status().isUnauthorized());
    }
    @Test void hidesCorrectAnswersAndFormatsDates() throws Exception {
        mvc.perform(post("/api/exams/1/start").header("Authorization","Bearer demo-student"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.questions[0].correctAnswer").doesNotExist())
            .andExpect(jsonPath("$.data.deadlineAt",matchesPattern("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")));
    }
    @Test void malformedPayloadReturnsPublicError() throws Exception {
        mvc.perform(put("/api/attempts/1/answers").header("Authorization","Bearer demo-student").contentType("application/json").content("{"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
    }

}
