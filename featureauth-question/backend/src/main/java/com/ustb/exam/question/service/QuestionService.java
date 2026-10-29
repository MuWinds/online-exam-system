package com.ustb.exam.question.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ustb.exam.auth.entity.SysUser;
import com.ustb.exam.auth.mapper.UserMapper;
import com.ustb.exam.common.BizException;
import com.ustb.exam.common.LoginUser;
import com.ustb.exam.common.PageResult;
import com.ustb.exam.common.ResultCode;
import com.ustb.exam.common.Roles;
import com.ustb.exam.common.UserContext;
import com.ustb.exam.question.dto.QuestionSaveRequest;
import com.ustb.exam.question.entity.Question;
import com.ustb.exam.question.entity.QuestionOption;
import com.ustb.exam.question.enums.QuestionType;
import com.ustb.exam.question.mapper.QuestionMapper;
import com.ustb.exam.question.mapper.QuestionOptionMapper;
import com.ustb.exam.question.vo.QuestionVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 题库管理服务。
 * 权限规则：管理员可管理全部题目；教师只能管理本人创建的题目；学生无访问权限。
 * 表结构沿用任务2/任务3 的约定：correctAnswer 以 JSON 数组保存在 question.standard_answer。
 */
@Service
public class QuestionService {

    private static final Logger log = LoggerFactory.getLogger(QuestionService.class);

    private static final int MAX_OPTIONS = 6;

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper optionMapper;
    private final UserMapper userMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public QuestionService(QuestionMapper questionMapper, QuestionOptionMapper optionMapper,
                           UserMapper userMapper, JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.questionMapper = questionMapper;
        this.optionMapper = optionMapper;
        this.userMapper = userMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 分页查询题目。教师只能看到自己创建的题目。
     */
    public PageResult<QuestionVO> page(long page, long size, QuestionType questionType, String keyword) {
        LoginUser me = UserContext.require();
        long current = page < 1 ? 1 : page;
        long pageSize = size < 1 ? 10 : Math.min(size, 100);

        LambdaQueryWrapper<Question> wrapper = Wrappers.lambdaQuery();
        if (!Roles.ADMIN.equals(me.role())) {
            wrapper.eq(Question::getCreatedBy, me.id());
        }
        if (questionType != null) {
            wrapper.eq(Question::getQuestionType, questionType.name());
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Question::getContent, keyword.trim());
        }
        wrapper.orderByDesc(Question::getCreatedAt);

        Page<Question> result = questionMapper.selectPage(new Page<>(current, pageSize), wrapper);
        Map<Long, String> creatorNames = loadCreatorNames(result.getRecords());
        List<QuestionVO> records = result.getRecords().stream()
                .map(question -> QuestionVO.from(question, null,
                        creatorNames.get(question.getCreatedBy()),
                        parseAnswer(question.getStandardAnswer())))
                .toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 题目详情（含选项）。
     */
    public QuestionVO detail(Long id) {
        Question question = requireQuestion(id);
        checkReadable(question);
        List<QuestionOption> options = optionMapper.selectList(Wrappers.<QuestionOption>lambdaQuery()
                .eq(QuestionOption::getQuestionId, id)
                .orderByAsc(QuestionOption::getSortOrder));
        SysUser creator = userMapper.selectById(question.getCreatedBy());
        return QuestionVO.from(question, options,
                creator == null ? null : creator.getRealName(),
                parseAnswer(question.getStandardAnswer()));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(QuestionSaveRequest request) {
        LoginUser me = UserContext.require();
        List<OptionDraft> options = normalizeOptions(request);
        String standardAnswer = buildStandardAnswer(request.questionType(), request.answer(), options);

        Question question = new Question();
        question.setCreatedBy(me.id());
        question.setQuestionType(request.questionType().name());
        question.setContent(request.content().trim());
        question.setStandardAnswer(standardAnswer);
        question.setAnalysis(trimToNull(request.analysis()));
        question.setScore(request.score());
        question.setStatus(STATUS_ACTIVE);
        questionMapper.insert(question);

        insertOptions(question.getId(), options);
        return question.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, QuestionSaveRequest request) {
        Question exists = requireQuestion(id);
        checkWritable(exists);
        List<OptionDraft> options = normalizeOptions(request);
        String standardAnswer = buildStandardAnswer(request.questionType(), request.answer(), options);

        Question update = new Question();
        update.setId(id);
        update.setQuestionType(request.questionType().name());
        update.setContent(request.content().trim());
        update.setStandardAnswer(standardAnswer);
        update.setAnalysis(trimToNull(request.analysis()));
        update.setScore(request.score());
        questionMapper.updateById(update);

        // 选项整体替换，保证题目与选项一致
        optionMapper.delete(Wrappers.<QuestionOption>lambdaQuery().eq(QuestionOption::getQuestionId, id));
        insertOptions(id, options);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Question exists = requireQuestion(id);
        checkWritable(exists);

        long references = countPaperReferences(id);
        if (references > 0) {
            throw new BizException(ResultCode.CONFLICT,
                    "该题目已被 " + references + " 份试卷引用，不能删除");
        }
        // question_option 对 question 有外键约束，必须先删选项
        optionMapper.delete(Wrappers.<QuestionOption>lambdaQuery().eq(QuestionOption::getQuestionId, id));
        questionMapper.deleteById(id);
    }

    // ------------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------------

    /** 选项草稿，创建后写入 question_option */
    private record OptionDraft(String key, String content) {
    }

    private Question requireQuestion(Long id) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new BizException(ResultCode.NOT_FOUND, "题目不存在");
        }
        return question;
    }

    private void checkReadable(Question question) {
        LoginUser me = UserContext.require();
        if (Roles.ADMIN.equals(me.role())) {
            return;
        }
        if (!me.id().equals(question.getCreatedBy())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权查看他人创建的题目");
        }
    }

    private void checkWritable(Question question) {
        checkReadable(question);
    }

    /**
     * 校验并规范化选项：选择题 2~6 个，标号 A~F 不重复；判断题自动生成 TRUE / FALSE。
     */
    private List<OptionDraft> normalizeOptions(QuestionSaveRequest request) {
        if (request.questionType() == QuestionType.JUDGE) {
            return List.of(new OptionDraft("TRUE", "正确"), new OptionDraft("FALSE", "错误"));
        }
        List<QuestionSaveRequest.OptionItem> items = request.options() == null
                ? List.of() : request.options();
        if (items.size() < 2) {
            throw new BizException(ResultCode.BAD_REQUEST, "选择题至少需要 2 个选项");
        }
        if (items.size() > MAX_OPTIONS) {
            throw new BizException(ResultCode.BAD_REQUEST, "选择题最多支持 " + MAX_OPTIONS + " 个选项");
        }
        Set<String> keys = new HashSet<>();
        List<OptionDraft> drafts = new ArrayList<>();
        for (QuestionSaveRequest.OptionItem item : items) {
            String key = item.optionKey() == null ? "" : item.optionKey().trim().toUpperCase(Locale.ROOT);
            if (!key.matches("[A-F]")) {
                throw new BizException(ResultCode.BAD_REQUEST, "选项标号必须为 A~F");
            }
            if (!keys.add(key)) {
                throw new BizException(ResultCode.BAD_REQUEST, "选项标号重复：" + key);
            }
            drafts.add(new OptionDraft(key, item.content().trim()));
        }
        return drafts;
    }

    /**
     * 生成标准答案 JSON。
     * 单选 ["A"]，多选 ["A","C"]（升序），判断 ["TRUE"] / ["FALSE"]。
     */
    private String buildStandardAnswer(QuestionType type, List<String> answer, List<OptionDraft> options) {
        if (type == QuestionType.JUDGE) {
            if (answer == null || answer.size() != 1) {
                throw new BizException(ResultCode.BAD_REQUEST, "判断题标准答案必须为 TRUE 或 FALSE");
            }
            return writeJson(List.of(normalizeJudgeAnswer(answer.get(0))));
        }

        Set<String> optionKeys = options.stream().map(OptionDraft::key).collect(Collectors.toSet());
        List<String> answers = new ArrayList<>();
        if (answer != null) {
            for (String item : answer) {
                if (!StringUtils.hasText(item)) {
                    continue;
                }
                String key = item.trim().toUpperCase(Locale.ROOT);
                if (!optionKeys.contains(key)) {
                    throw new BizException(ResultCode.BAD_REQUEST, "正确答案 " + key + " 不在选项中");
                }
                if (!answers.contains(key)) {
                    answers.add(key);
                }
            }
        }
        answers.sort(Comparator.naturalOrder());

        if (answers.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "请选择正确答案");
        }
        if (type == QuestionType.SINGLE_CHOICE && answers.size() != 1) {
            throw new BizException(ResultCode.BAD_REQUEST, "单选题必须有且只有一个正确答案");
        }
        if (type == QuestionType.MULTIPLE_CHOICE && answers.size() < 2) {
            throw new BizException(ResultCode.BAD_REQUEST, "多选题至少需要两个正确答案");
        }
        return writeJson(answers);
    }

    private String normalizeJudgeAnswer(String value) {
        String text = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if ("TRUE".equals(text) || "T".equals(text) || "正确".equals(text)) {
            return "TRUE";
        }
        if ("FALSE".equals(text) || "F".equals(text) || "错误".equals(text)) {
            return "FALSE";
        }
        throw new BizException(ResultCode.BAD_REQUEST, "判断题标准答案必须为 TRUE 或 FALSE");
    }

    private void insertOptions(Long questionId, List<OptionDraft> options) {
        int index = 1;
        for (OptionDraft draft : options) {
            QuestionOption option = new QuestionOption();
            option.setQuestionId(questionId);
            option.setOptionKey(draft.key());
            option.setOptionContent(draft.content());
            option.setSortOrder(index++);
            optionMapper.insert(option);
        }
    }

    private List<String> parseAnswer(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            log.warn("标准答案格式不正确：{}", json);
            return List.of();
        }
    }

    private String writeJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new BizException(ResultCode.ERROR, "标准答案序列化失败");
        }
    }

    private Map<Long, String> loadCreatorNames(List<Question> questions) {
        List<Long> ids = questions.stream()
                .map(Question::getCreatedBy)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId,
                        user -> user.getRealName() == null ? user.getUsername() : user.getRealName()));
    }

    /**
     * 统计题目被试卷引用的次数。
     * paper_question 表由任务2维护，任务1单独运行时可能不存在，此时跳过校验并记录告警。
     */
    private long countPaperReferences(Long questionId) {
        try {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM paper_question WHERE question_id = ?", Long.class, questionId);
            return count == null ? 0L : count;
        } catch (DataAccessException e) {
            log.warn("paper_question 表不可用，跳过题目引用校验：{}", e.getMessage());
            return 0L;
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
