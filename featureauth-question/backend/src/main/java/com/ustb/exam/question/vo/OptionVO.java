package com.ustb.exam.question.vo;

import com.ustb.exam.question.entity.QuestionOption;

/**
 * 题目选项视图对象。
 */
public record OptionVO(
        Long id,
        String optionKey,
        String optionContent,
        Integer sortOrder
) {

    public static OptionVO from(QuestionOption option) {
        return new OptionVO(option.getId(), option.getOptionKey(),
                option.getOptionContent(), option.getSortOrder());
    }
}
