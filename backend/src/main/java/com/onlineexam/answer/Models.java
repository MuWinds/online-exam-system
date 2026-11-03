package com.onlineexam.answer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public final class Models {
    private Models() {}
    public record Actor(Long id, String role) {}
    public record Option(String key, String label) {}
    // Server-only: never return Question or Paper directly from student APIs.
    public record Question(Long questionId, String type, String content, List<Option> options,
                           BigDecimal score, List<String> correctAnswer) {}
    public record Paper(Long examId, String title, Long teacherId, boolean published,
                        LocalDateTime startsAt, LocalDateTime endsAt, List<Question> questions) {}
    public record PublicQuestion(Long questionId, String type, String content, List<Option> options, BigDecimal score) {}
    public record AnswerInput(Long questionId, List<String> answer) {}
    public record SaveRequest(List<AnswerInput> answers, Integer version) {}
    public record Context(Long attemptId, Long examId, String examTitle, String status,
                          LocalDateTime deadlineAt, LocalDateTime serverTime, int version,
                          List<PublicQuestion> questions, Map<Long,List<String>> answers) {}
    public record Result(Long attemptId, Long examId, String examTitle, Long studentId,
                         BigDecimal totalScore, BigDecimal maxScore, String submissionType, LocalDateTime submittedAt) {}
    public record Page<T>(List<T> records, long total, int page, int size) {}
    public record Statistics(long participantCount, long gradedCount, BigDecimal averageScore,
                             BigDecimal maxScore, BigDecimal minScore, long passCount,
                             BigDecimal passRate, Map<String,Long> distribution) {}
    public static class Attempt {
        public Long id, examId, studentId;
        public String status, submissionType, paperSnapshot;
        public LocalDateTime startedAt, deadlineAt, submittedAt;
        public int version;
    }
    public static class StoredAnswer {
        public Long questionId;
        public String answerContent;
    }
}
