package com.onlineexam.paper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlineexam.OnlineExamApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = OnlineExamApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class PaperExamApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void teacherCanPublishExamAndStudentCanStartIt() throws Exception {
        String teacher = "Bearer demo-teacher";
        String student = "Bearer demo-student";
        var paperResponse = mvc.perform(post("/api/papers").header("Authorization", teacher)
                .contentType("application/json").content("{\"paperName\":\"HTTP发布流程试卷\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("DRAFT"))
            .andReturn().getResponse().getContentAsString();
        long paperId = json.readTree(paperResponse).path("data").path("paperId").asLong();

        mvc.perform(post("/api/papers/" + paperId + "/questions").header("Authorization", teacher)
                .contentType("application/json").content("{\"questionId\":11,\"score\":100}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalScore").value(100));
        mvc.perform(post("/api/papers/" + paperId + "/publish").header("Authorization", teacher))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        LocalDateTime start = LocalDateTime.now().withNano(0).minusMinutes(1);
        LocalDateTime end = start.plusHours(1);
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String examRequest = json.writeValueAsString(Map.of(
            "paperId", paperId,
            "examName", "HTTP发布流程考试",
            "startAt", start.format(format),
            "endAt", end.format(format)
        ));
        var examResponse = mvc.perform(post("/api/managed/exams").header("Authorization", teacher)
                .contentType("application/json").content(examRequest))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.published").value(false))
            .andReturn().getResponse().getContentAsString();
        long examId = json.readTree(examResponse).path("data").path("examId").asLong();

        mvc.perform(post("/api/managed/exams/" + examId + "/publish").header("Authorization", teacher))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("ONGOING"));
        mvc.perform(get("/api/exams/available").header("Authorization", student))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data[*].examName", hasItem("HTTP发布流程考试")));
        mvc.perform(post("/api/exams/" + examId + "/start").header("Authorization", student))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.questions[0].correctAnswer").doesNotExist());
    }
}
