package com.ustb.exam.question.controller;

import com.ustb.exam.common.PageResult;
import com.ustb.exam.common.RequireRole;
import com.ustb.exam.common.Result;
import com.ustb.exam.common.Roles;
import com.ustb.exam.question.dto.QuestionSaveRequest;
import com.ustb.exam.question.enums.QuestionType;
import com.ustb.exam.question.service.QuestionService;
import com.ustb.exam.question.vo.QuestionVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 题库管理接口。管理员与教师可访问，学生返回 403。
 */
@RestController
@RequestMapping("/api/questions")
@RequireRole({Roles.TEACHER, Roles.ADMIN})
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    /**
     * 分页查询题目。
     * GET /api/questions?page=1&size=10&questionType=SINGLE_CHOICE&keyword=事务
     */
    @GetMapping
    public Result<PageResult<QuestionVO>> page(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) QuestionType questionType,
                                               @RequestParam(required = false) String keyword) {
        return Result.ok(questionService.page(page, size, questionType, keyword));
    }

    /**
     * 题目详情（含选项与标准答案）。
     * GET /api/questions/{id}
     */
    @GetMapping("/{id}")
    public Result<QuestionVO> detail(@PathVariable Long id) {
        return Result.ok(questionService.detail(id));
    }

    /**
     * 新增题目。
     * POST /api/questions
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody QuestionSaveRequest request) {
        return Result.ok(questionService.create(request));
    }

    /**
     * 修改题目。
     * PUT /api/questions/{id}
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody QuestionSaveRequest request) {
        questionService.update(id, request);
        return Result.ok();
    }

    /**
     * 删除题目（被试卷引用时返回 409）。
     * DELETE /api/questions/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return Result.ok();
    }
}
