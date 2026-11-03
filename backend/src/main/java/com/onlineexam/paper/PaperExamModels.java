package com.onlineexam.paper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.onlineexam.answer.Models.Option;

public final class PaperExamModels {
    private PaperExamModels() {}

    public record PaperRequest(String paperName) {}
    public record PaperQuestionRequest(Long questionId, Integer sortOrder, BigDecimal score) {}
    public record ExamRequest(Long paperId, String examName, LocalDateTime startAt, LocalDateTime endAt) {}

    public record QuestionView(Long questionId, String type, String content,
                               List<Option> options, BigDecimal defaultScore, String status) {}

    public record PaperQuestionView(Long questionId, String type, String content,
                                    List<Option> options, Integer sortOrder, BigDecimal score) {}

    public record PaperView(Long paperId, Long createdBy, String paperName, BigDecimal totalScore,
                            String status, LocalDateTime createdAt, LocalDateTime updatedAt,
                            List<PaperQuestionView> questions) {}

    public record ExamView(Long examId, Long paperId, String examName, LocalDateTime startAt,
                           LocalDateTime endAt, String status, boolean published, Long createdBy,
                           BigDecimal totalScore, int questionCount, LocalDateTime createdAt,
                           LocalDateTime updatedAt) {}

    public static class QuestionRow {
        public Long id;
        public Long createdBy;
        public String questionType;
        public String content;
        public String standardAnswer;
        public String status;
        public BigDecimal score;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
    }

    public static class OptionRow {
        public Long id;
        public Long questionId;
        public String optionKey;
        public String optionContent;
        public Integer sortOrder;
    }

    public static class PaperRow {
        public Long id;
        public Long createdBy;
        public String paperName;
        public BigDecimal totalScore;
        public String status;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
    }

    public static class PaperQuestionRow {
        public Long id;
        public Long paperId;
        public Long questionId;
        public Integer sortOrder;
        public BigDecimal score;
    }

    public static class ExamRow {
        public Long id;
        public Long paperId;
        public Long createdBy;
        public String examName;
        public LocalDateTime startAt;
        public LocalDateTime endAt;
        public String status;
        public Boolean published;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
    }
}
