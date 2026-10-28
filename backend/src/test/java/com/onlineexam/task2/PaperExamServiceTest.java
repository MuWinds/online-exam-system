package com.onlineexam.task2;

import com.onlineexam.task3.BusinessException;
import com.onlineexam.task3.Models.Actor;
import com.onlineexam.task3.Task3Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.onlineexam.task2.PaperExamModels.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Task3Application.class)
@ActiveProfiles("demo")
class PaperExamServiceTest {
    private static final Actor TEACHER = new Actor(1L, "TEACHER");
    private static final Actor STUDENT = new Actor(100L, "STUDENT");

    @Autowired PaperExamService service;

    @Test
    void teacherCanAssemblePublishAndScheduleAnExamForStudents() {
        PaperView paper = service.createPaper(TEACHER, new PaperRequest("集成测试试卷"));
        assertEquals("DRAFT", paper.status());
        assertEquals(0, paper.totalScore().compareTo(BigDecimal.ZERO.setScale(2)));

        paper = service.addQuestion(TEACHER, paper.paperId(),
            new PaperQuestionRequest(11L, null, new BigDecimal("25")));
        paper = service.addQuestion(TEACHER, paper.paperId(),
            new PaperQuestionRequest(12L, null, new BigDecimal("75.50")));
        assertEquals(new BigDecimal("100.50"), paper.totalScore());
        assertEquals(2, paper.questions().size());

        PaperView publishedPaper = service.publishPaper(TEACHER, paper.paperId());
        assertEquals("PUBLISHED", publishedPaper.status());
        Long paperId = paper.paperId();
        assertThrows(BusinessException.class, () -> service.addQuestion(TEACHER, paperId,
            new PaperQuestionRequest(13L, null, BigDecimal.ONE)));

        LocalDateTime start = LocalDateTime.now().withNano(0).plusDays(1);
        ExamView exam = service.createExam(TEACHER,
            new ExamRequest(paper.paperId(), "集成测试考试", start, start.plusHours(2)));
        assertFalse(exam.published());

        ExamView publishedExam = service.publishExam(TEACHER, exam.examId());
        assertTrue(publishedExam.published());
        assertEquals("NOT_STARTED", publishedExam.status());
        assertTrue(service.available(STUDENT).stream().anyMatch(e -> e.examId().equals(exam.examId())));

        var serverPaper = service.forStudent(exam.examId(), STUDENT.id());
        assertEquals(2, serverPaper.questions().size());
        assertEquals("A", serverPaper.questions().get(0).correctAnswer().get(0));
        BusinessException notEnrolled = assertThrows(BusinessException.class,
            () -> service.forStudent(exam.examId(), 102L));
        assertEquals(403, notEnrolled.status);
    }

    @Test
    void blocksEmptyPapersAndRejectsStudentManagement() {
        PaperView empty = service.createPaper(TEACHER, new PaperRequest("空试卷"));
        BusinessException emptyPaper = assertThrows(BusinessException.class,
            () -> service.publishPaper(TEACHER, empty.paperId()));
        assertEquals(409, emptyPaper.status);

        BusinessException forbidden = assertThrows(BusinessException.class,
            () -> service.createPaper(STUDENT, new PaperRequest("越权试卷")));
        assertEquals(403, forbidden.status);
    }

    @Test
    void onlyOwnerCanManagePapersAndUnpublishedExamsStayHidden() {
        PaperView paper = service.createPaper(TEACHER, new PaperRequest("权限测试试卷"));
        BusinessException forbidden = assertThrows(BusinessException.class,
            () -> service.paper(new Actor(2L, "TEACHER"), paper.paperId()));
        assertEquals(403, forbidden.status);
        service.addQuestion(TEACHER, paper.paperId(), new PaperQuestionRequest(11L, null, BigDecimal.ONE));
        service.publishPaper(TEACHER, paper.paperId());
        var unpublished = service.createExam(TEACHER, new ExamRequest(paper.paperId(),
            "未发布考试", LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)));
        assertThrows(BusinessException.class, () -> service.exam(STUDENT, unpublished.examId()));
    }
}
