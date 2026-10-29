package com.ustb.exam.question.dto;

import com.ustb.exam.question.enums.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 新增 / 修改题目请求体。
 * 正确答案与选项分离：选择题提交 answer = ["A"] / ["A","C"]，
 * 判断题提交 answer = ["TRUE"] / ["FALSE"]，选项内容由 options 提供。
 */
public record QuestionSaveRequest(

        @NotNull(message = "题型不能为空")
        QuestionType questionType,

        @NotBlank(message = "题干不能为空")
        @Size(max = 1000, message = "题干长度不能超过1000字")
        String content,

        @NotNull(message = "分值不能为空")
        @DecimalMin(value = "0.01", message = "分值必须大于0")
        @DecimalMax(value = "100.00", message = "分值不能超过100")
        BigDecimal score,

        @Size(max = 1000, message = "解析长度不能超过1000字")
        String analysis,

        List<String> answer,

        @Valid
        List<OptionItem> options
) {

    /**
     * 选项。判断题不需要传 options，服务端会自动生成 TRUE / FALSE 两个选项。
     */
    public record OptionItem(

            @NotBlank(message = "选项标号不能为空")
            String optionKey,

            @NotBlank(message = "选项内容不能为空")
            @Size(max = 200, message = "选项内容不能超过200字")
            String content
    ) {
    }
}
