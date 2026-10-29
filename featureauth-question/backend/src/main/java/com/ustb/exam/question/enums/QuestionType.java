package com.ustb.exam.question.enums;

/**
 * 题型枚举，与《公共契约确认单》E1 保持一致。
 */
public enum QuestionType {

    SINGLE_CHOICE("单选题"),
    MULTIPLE_CHOICE("多选题"),
    JUDGE("判断题");

    private final String label;

    QuestionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isChoice() {
        return this != JUDGE;
    }
}
