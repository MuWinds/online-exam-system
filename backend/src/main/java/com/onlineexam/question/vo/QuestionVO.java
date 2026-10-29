package com.onlineexam.question.vo;

import com.onlineexam.question.entity.Question;
import com.onlineexam.question.entity.QuestionOption;
import com.onlineexam.question.enums.QuestionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目视图对象。列表接口不返回 options（为 null），详情接口返回完整选项。
 */
public record QuestionVO(
        Long id,
        String questionType,
        String typeName,
        String content,
        List<String> standardAnswer,
        String analysis,
        BigDecimal score,
        Long createdBy,
        String creatorName,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OptionVO> options
) {

    public static QuestionVO from(Question question, List<QuestionOption> options,
                                  String creatorName, List<String> standardAnswer) {
        QuestionType type = parseType(question.getQuestionType());
        List<OptionVO> optionVOs = options == null ? null
                : options.stream().map(OptionVO::from).toList();
        return new QuestionVO(
                question.getId(),
                question.getQuestionType(),
                type == null ? question.getQuestionType() : type.getLabel(),
                question.getContent(),
                standardAnswer,
                question.getAnalysis(),
                question.getScore(),
                question.getCreatedBy(),
                creatorName,
                question.getStatus(),
                question.getCreatedAt(),
                question.getUpdatedAt(),
                optionVOs);
    }

    private static QuestionType parseType(String value) {
        try {
            return QuestionType.valueOf(value);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
