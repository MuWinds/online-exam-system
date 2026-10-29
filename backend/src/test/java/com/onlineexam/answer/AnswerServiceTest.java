package com.onlineexam.answer;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.concurrent.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.onlineexam.answer.Models.*;

@SpringBootTest
@ActiveProfiles("demo")
class AnswerServiceTest {
    @Autowired AnswerService service;
    @Autowired JdbcTemplate jdbc;
    final Actor student = new Actor(100L, "STUDENT");
    @BeforeEach void clean() { jdbc.update("DELETE FROM exam_score"); jdbc.update("DELETE FROM exam_answer"); jdbc.update("DELETE FROM exam_attempt"); }

    @Test void scoresExactSetsAndUnansweredAsZero() {
        assertEquals(new BigDecimal("5.00"), Grading.score("MULTIPLE_CHOICE", List.of("B","A"), List.of("A","B"), new BigDecimal("5")));
        assertEquals(new BigDecimal("0.00"), Grading.score("MULTIPLE_CHOICE", List.of("A"), List.of("A","B"), new BigDecimal("5")));
        assertEquals(new BigDecimal("0.00"), Grading.score("JUDGE", List.of(), List.of("TRUE"), new BigDecimal("5")));
    }
    @Test void restoresSavedAnswersAndGradesOnlyOnce() {
        var a = service.start(student, 1L);
        service.save(student, a.attemptId(), new SaveRequest(List.of(new AnswerInput(11L,List.of("A"))),0));
        assertEquals(a.attemptId(), service.start(student,1L).attemptId());
        assertEquals(List.of("A"), service.context(student,a.attemptId()).answers().get(11L));
        var result = service.submit(student,a.attemptId());
        assertEquals(new BigDecimal("30.00"), result.totalScore());
        assertEquals(result, service.submit(student,a.attemptId()));
        assertEquals(1,service.myScores(student,1,10).total());
    }
    @Test void deniesOtherStudentsAndStudentStatistics() {
        var a = service.start(student,1L);
        assertStatus(403, () -> service.context(new Actor(101L,"STUDENT"),a.attemptId()));
        assertStatus(403, () -> service.statistics(student,1L));
        assertStatus(403, () -> service.start(new Actor(1L,"TEACHER"),1L));
    }
    @Test void rejectsInvalidBatchWithoutSavingEarlierAnswers() {
        var a=service.start(student,1L);
        assertStatus(400, () -> service.save(student,a.attemptId(),new SaveRequest(List.of(new AnswerInput(11L,List.of("A")),new AnswerInput(999L,List.of("X"))),0)));
        assertTrue(service.context(student,a.attemptId()).answers().isEmpty());
    }
    @Test void rejectsStaleVersionAndChangesAfterSubmission() {
        var a=service.start(student,1L);
        service.save(student,a.attemptId(),new SaveRequest(List.of(new AnswerInput(11L,List.of("A"))),0));
        assertStatus(409, () -> service.save(student,a.attemptId(),new SaveRequest(List.of(),0)));
        service.submit(student,a.attemptId());
        assertStatus(409, () -> service.save(student,a.attemptId(),new SaveRequest(List.of(),1)));
    }
    @Test void expiredAttemptIsSubmittedByServerAndRejectsLateSave() {
        var a=service.start(student,1L);
        jdbc.update("UPDATE exam_attempt SET deadline_at='2000-01-01 00:00:00' WHERE id=?",a.attemptId());
        service.expire();
        assertEquals("GRADED", service.context(student,a.attemptId()).status());
        assertEquals("AUTO_SUBMITTED",service.result(student,a.attemptId()).submissionType());
        assertStatus(409, () -> service.save(student,a.attemptId(),new SaveRequest(List.of(),0)));
    }
    @Test void statisticsExcludeOpenAttemptsAndHandleEmpty() {
        var teacher=new Actor(1L,"TEACHER");
        assertEquals(0,service.statistics(teacher,1L).gradedCount());
        assertNull(service.statistics(teacher,1L).averageScore());
        service.start(student,1L);
        assertEquals(0,service.statistics(teacher,1L).gradedCount());
        var a=service.start(new Actor(101L,"STUDENT"),1L);
        service.submit(new Actor(101L,"STUDENT"),a.attemptId());
        assertEquals(new BigDecimal("0.00"),service.statistics(teacher,1L).averageScore());
        assertStatus(403, () -> service.statistics(new Actor(99L,"TEACHER"),1L));
    }
    @Test void futureAndUnknownExamsCannotBeStarted() {
        assertStatus(409, () -> service.start(student,2L));
        assertStatus(404, () -> service.start(student,999L));
    }
    @Test void rejectsDuplicateOptionsAndUnknownOptions() {
        var a=service.start(student,1L);
        assertStatus(400, () -> service.save(student,a.attemptId(),new SaveRequest(List.of(new AnswerInput(12L,List.of("A","A"))),0)));
        assertStatus(400, () -> service.save(student,a.attemptId(),new SaveRequest(List.of(new AnswerInput(11L,List.of("X"))),0)));
    }
    @Test void concurrentStartsAndSubmissionsShareOneAttemptAndScore() throws Exception {
        ExecutorService executor=Executors.newFixedThreadPool(4);
        try {
            var starts=executor.invokeAll(java.util.stream.IntStream.range(0,4).mapToObj(i->(Callable<Context>)()->service.start(student,1L)).toList());
            Long id=starts.get(0).get().attemptId();
            for(var f:starts) assertEquals(id,f.get().attemptId());
            var submissions=executor.invokeAll(java.util.stream.IntStream.range(0,4).mapToObj(i->(Callable<Result>)()->service.submit(student,id)).toList());
            for(var f:submissions) assertEquals(new BigDecimal("0.00"),f.get().totalScore());
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM exam_attempt",Integer.class));
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM exam_score",Integer.class));
        } finally { executor.shutdownNow(); }
    }
    private void assertStatus(int expected, Runnable action) {
        assertEquals(expected,assertThrows(BusinessException.class,action::run).status);
    }
}
