package com.onlineexam.task2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import com.onlineexam.task3.ExamGateway;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

import com.onlineexam.task3.BusinessException;

import static com.onlineexam.task3.Models.*;
import static com.onlineexam.task2.PaperExamModels.*;

@Service
@Transactional
public class PaperExamService implements ExamGateway {
    private static final BigDecimal MAX_SCORE = new BigDecimal("9999.99");
    private static final Set<String> QUESTION_TYPES = Set.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "JUDGE");

    private final Task2Mapper db;
    private final ObjectMapper json;
    private final Clock clock;
    private final ExamEligibilityGateway eligibility;

    public PaperExamService(Task2Mapper db, ObjectMapper json, Clock clock, ExamEligibilityGateway eligibility) {
        this.db = db;
        this.json = json;
        this.clock = clock;
        this.eligibility = eligibility;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).withNano(0);
    }

    private void manager(Actor actor) {
        if (actor == null || actor.id() == null) throw new BusinessException(401, "请先登录");
        if (!Set.of("TEACHER", "ADMIN").contains(actor.role())) {
            throw new BusinessException(403, "仅教师或管理员可以管理试卷和考试");
        }
    }

    private void student(Actor actor) {
        if (actor == null || actor.id() == null) throw new BusinessException(401, "请先登录");
        if (!"STUDENT".equals(actor.role())) throw new BusinessException(403, "仅学生可以查看可参加考试");
    }

    public List<QuestionView> questions(Actor actor) {
        manager(actor);
        List<QuestionRow> rows = "ADMIN".equals(actor.role()) ? db.questions() : db.questionsByOwner(actor.id());
        return rows.stream().map(this::questionView).toList();
    }

    public List<PaperView> papers(Actor actor) {
        manager(actor);
        List<PaperRow> rows = "ADMIN".equals(actor.role()) ? db.allPapers() : db.papersByOwner(actor.id());
        return rows.stream().map(this::paperView).toList();
    }

    public PaperView paper(Actor actor, Long id) {
        manager(actor);
        PaperRow row = requirePaper(id);
        assertOwner(row.createdBy, actor);
        return paperView(row);
    }

    public PaperView createPaper(Actor actor, PaperRequest request) {
        manager(actor);
        String name = requiredName(request == null ? null : request.paperName(), "试卷名称");
        PaperRow row = new PaperRow();
        row.createdBy = actor.id();
        row.paperName = name;
        row.totalScore = BigDecimal.ZERO.setScale(2);
        db.insertPaper(row);
        return paperView(requirePaper(row.id));
    }

    public PaperView updatePaper(Actor actor, Long id, PaperRequest request) {
        manager(actor);
        PaperRow row = ownedPaper(actor, id);
        ensurePaperDraft(row);
        row.paperName = requiredName(request == null ? null : request.paperName(), "试卷名称");
        db.updatePaper(row);
        return paperView(requirePaper(id));
    }

    public PaperView addQuestion(Actor actor, Long paperId, PaperQuestionRequest request) {
        manager(actor);
        PaperRow paper = ownedPaper(actor, paperId);
        ensurePaperDraft(paper);
        if (request == null || request.questionId() == null) throw new BusinessException(400, "题目不能为空");
        QuestionRow question = db.question(request.questionId());
        if (question == null || !"ACTIVE".equals(question.status)) throw new BusinessException(404, "题目不存在");
        if (!"ADMIN".equals(actor.role()) && !Objects.equals(question.createdBy, actor.id())) {
            throw new BusinessException(403, "只能使用自己创建的题目");
        }
        if (!QUESTION_TYPES.contains(question.questionType)) throw new BusinessException(409, "题型不受支持");
        BigDecimal score = request.score() == null ? question.score : request.score();
        validateScore(score);
        int sortOrder = request.sortOrder() == null ? db.maxSortOrder(paperId) + 1 : request.sortOrder();
        if (sortOrder < 1) throw new BusinessException(400, "题目顺序必须从1开始");
        if (db.duplicatePaperQuestion(paperId, request.questionId(), sortOrder) > 0) {
            throw new BusinessException(409, "题目已加入试卷或顺序已被占用");
        }
        PaperQuestionRow relation = new PaperQuestionRow();
        relation.paperId = paperId;
        relation.questionId = request.questionId();
        relation.sortOrder = sortOrder;
        relation.score = score.setScale(2);
        db.insertPaperQuestion(relation);
        refreshPaperTotal(paper);
        return paperView(requirePaper(paperId));
    }

    public PaperView removeQuestion(Actor actor, Long paperId, Long questionId) {
        manager(actor);
        PaperRow paper = ownedPaper(actor, paperId);
        ensurePaperDraft(paper);
        if (db.question(questionId) == null) throw new BusinessException(404, "题目不存在");
        db.deletePaperQuestion(paperId, questionId);
        refreshPaperTotal(paper);
        return paperView(requirePaper(paperId));
    }

    public PaperView publishPaper(Actor actor, Long id) {
        manager(actor);
        PaperRow paper = ownedPaper(actor, id);
        ensurePaperDraft(paper);
        int count = db.paperQuestionCount(id);
        if (count == 0 || paper.totalScore == null || paper.totalScore.signum() <= 0) {
            throw new BusinessException(409, "空试卷不能发布");
        }
        paper.status = "PUBLISHED";
        db.updatePaper(paper);
        return paperView(requirePaper(id));
    }

    public PaperView archivePaper(Actor actor, Long id) {
        manager(actor);
        PaperRow paper = ownedPaper(actor, id);
        if ("ARCHIVED".equals(paper.status)) return paperView(paper);
        if (db.examCountForPaper(id) > 0) throw new BusinessException(409, "试卷已被考试使用，不能归档");
        paper.status = "ARCHIVED";
        db.updatePaper(paper);
        return paperView(requirePaper(id));
    }

    public List<ExamView> exams(Actor actor) {
        manager(actor);
        List<ExamRow> rows = "ADMIN".equals(actor.role()) ? db.allExams() : db.examsByOwner(actor.id());
        return rows.stream().map(this::examView).toList();
    }

    public ExamView exam(Actor actor, Long id) {
        if (actor != null && "STUDENT".equals(actor.role())) {
            ExamRow row = requireExam(id);
            if (!Boolean.TRUE.equals(row.published) || !eligibility.isEligible(row.id, actor.id())) {
                throw new BusinessException(404, "考试不存在或不可参加");
            }
            return examView(row);
        }
        manager(actor);
        ExamRow row = requireExam(id);
        assertOwner(row.createdBy, actor);
        return examView(row);
    }

    public ExamView createExam(Actor actor, ExamRequest request) {
        manager(actor);
        validateExamRequest(request);
        PaperRow paper = requirePaper(request.paperId());
        assertOwner(paper.createdBy, actor);
        if (!"PUBLISHED".equals(paper.status)) throw new BusinessException(409, "只有已发布试卷可以配置考试");
        ExamRow row = new ExamRow();
        row.paperId = request.paperId();
        row.createdBy = actor.id();
        row.examName = requiredName(request.examName(), "考试名称");
        row.startAt = request.startAt();
        row.endAt = request.endAt();
        row.status = "NOT_STARTED";
        row.published = false;
        db.insertExam(row);
        return examView(requireExam(row.id));
    }

    public ExamView updateExam(Actor actor, Long id, ExamRequest request) {
        manager(actor);
        ExamRow row = ownedExam(actor, id);
        if (Boolean.TRUE.equals(row.published) || !"NOT_STARTED".equals(row.status)) {
            throw new BusinessException(409, "考试发布后不能修改");
        }
        validateExamRequest(request);
        PaperRow paper = requirePaper(request.paperId());
        assertOwner(paper.createdBy, actor);
        if (!"PUBLISHED".equals(paper.status)) throw new BusinessException(409, "只有已发布试卷可以配置考试");
        row.paperId = request.paperId();
        row.examName = requiredName(request.examName(), "考试名称");
        row.startAt = request.startAt();
        row.endAt = request.endAt();
        db.updateExam(row);
        return examView(requireExam(id));
    }

    public ExamView publishExam(Actor actor, Long id) {
        manager(actor);
        ExamRow row = ownedExam(actor, id);
        PaperRow paper = requirePaper(row.paperId);
        if (!"PUBLISHED".equals(paper.status)) throw new BusinessException(409, "试卷尚未发布");
        if (!row.startAt.isBefore(row.endAt)) throw new BusinessException(400, "结束时间必须晚于开始时间");
        if (db.paperQuestionCount(row.paperId) == 0) throw new BusinessException(409, "试卷没有题目");
        row.published = true;
        row.status = statusAt(row, now());
        db.updateExam(row);
        return examView(requireExam(id));
    }

    public ExamView unpublishExam(Actor actor, Long id) {
        manager(actor);
        ExamRow row = ownedExam(actor, id);
        if (Boolean.TRUE.equals(row.published) && !now().isBefore(row.startAt)) {
            throw new BusinessException(409, "考试开始后不能撤回发布");
        }
        row.published = false;
        row.status = "NOT_STARTED";
        db.updateExam(row);
        return examView(requireExam(id));
    }

    public List<ExamView> available(Actor actor) {
        student(actor);
        return db.allExams().stream()
            .filter(e -> Boolean.TRUE.equals(e.published))
            .filter(e -> eligibility.isEligible(e.id, actor.id()))
            .map(this::examView)
            .filter(e -> !"FINISHED".equals(e.status()))
            .toList();
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Paper forStudent(Long examId, Long studentId) {
        ExamRow exam = requireExam(examId);
        if (!Boolean.TRUE.equals(exam.published)) throw new BusinessException(404, "考试不存在");
        if (studentId == null || !eligibility.isEligible(examId, studentId)) {
            throw new BusinessException(403, "未获得本场考试的参与资格");
        }
        PaperRow paper = requirePaper(exam.paperId);
        List<Question> questions = db.paperQuestions(paper.id).stream().map(relation -> {
            QuestionRow q = db.question(relation.questionId);
            if (q == null) throw new BusinessException(409, "试卷引用了不存在的题目");
            return new Question(q.id, q.questionType, q.content, db.options(q.id).stream()
                .map(o -> new Option(o.optionKey, o.optionContent)).toList(),
                relation.score, readAnswers(q.standardAnswer));
        }).toList();
        return new Paper(exam.id, exam.examName, exam.createdBy, Boolean.TRUE.equals(exam.published),
            exam.startAt, exam.endAt, questions);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Paper forManager(Long examId, Actor actor) {
        manager(actor);
        ExamRow exam = requireExam(examId);
        assertOwner(exam.createdBy, actor);
        PaperRow paper = requirePaper(exam.paperId);
        List<Question> questions = db.paperQuestions(paper.id).stream().map(relation -> {
            QuestionRow q = db.question(relation.questionId);
            if (q == null) throw new BusinessException(409, "试卷引用了不存在的题目");
            return new Question(q.id, q.questionType, q.content, db.options(q.id).stream()
                .map(o -> new Option(o.optionKey, o.optionContent)).toList(),
                relation.score, readAnswers(q.standardAnswer));
        }).toList();
        return new Paper(exam.id, exam.examName, exam.createdBy, Boolean.TRUE.equals(exam.published),
            exam.startAt, exam.endAt, questions);
    }

    private PaperRow ownedPaper(Actor actor, Long id) {
        PaperRow row = requirePaper(id);
        assertOwner(row.createdBy, actor);
        return row;
    }

    private ExamRow ownedExam(Actor actor, Long id) {
        ExamRow row = requireExam(id);
        assertOwner(row.createdBy, actor);
        return row;
    }

    private void assertOwner(Long ownerId, Actor actor) {
        if (!"ADMIN".equals(actor.role()) && !Objects.equals(ownerId, actor.id())) {
            throw new BusinessException(403, "无权管理该资源");
        }
    }

    private PaperRow requirePaper(Long id) {
        if (id == null) throw new BusinessException(400, "试卷ID不能为空");
        PaperRow row = db.paper(id);
        if (row == null) throw new BusinessException(404, "试卷不存在");
        return row;
    }

    private ExamRow requireExam(Long id) {
        if (id == null) throw new BusinessException(400, "考试ID不能为空");
        ExamRow row = db.exam(id);
        if (row == null) throw new BusinessException(404, "考试不存在");
        return row;
    }

    private String requiredName(String name, String label) {
        String value = name == null ? "" : name.trim();
        if (value.isEmpty() || value.length() > 150) throw new BusinessException(400, label + "不能为空且不能超过150个字符");
        return value;
    }

    private void validateScore(BigDecimal score) {
        if (score == null || score.signum() <= 0 || score.scale() > 2 || score.compareTo(MAX_SCORE) > 0) {
            throw new BusinessException(400, "分值必须是大于0且最多两位小数的数字");
        }
    }

    private void validateExamRequest(ExamRequest request) {
        if (request == null || request.paperId() == null || request.startAt() == null || request.endAt() == null) {
            throw new BusinessException(400, "考试名称、试卷和时间不能为空");
        }
        requiredName(request.examName(), "考试名称");
        if (!request.startAt().isBefore(request.endAt())) {
            throw new BusinessException(400, "结束时间必须晚于开始时间");
        }
    }

    private void ensurePaperDraft(PaperRow paper) {
        if (!"DRAFT".equals(paper.status)) throw new BusinessException(409, "只有草稿试卷可以编辑");
    }

    private void refreshPaperTotal(PaperRow paper) {
        paper.totalScore = db.paperTotal(paper.id).setScale(2);
        db.updatePaper(paper);
    }

    private String statusAt(ExamRow exam, LocalDateTime time) {
        if (!Boolean.TRUE.equals(exam.published)) return "NOT_STARTED";
        if (!time.isBefore(exam.endAt)) return "FINISHED";
        if (!time.isBefore(exam.startAt)) return "ONGOING";
        return "NOT_STARTED";
    }

    private QuestionView questionView(QuestionRow row) {
        return new QuestionView(row.id, row.questionType, row.content, db.options(row.id).stream()
            .map(o -> new Option(o.optionKey, o.optionContent)).toList(), row.score, row.status);
    }

    private PaperView paperView(PaperRow row) {
        List<PaperQuestionView> questions = db.paperQuestions(row.id).stream().map(relation -> {
            QuestionRow q = db.question(relation.questionId);
            if (q == null) throw new BusinessException(409, "试卷引用了不存在的题目");
            return new PaperQuestionView(q.id, q.questionType, q.content, db.options(q.id).stream()
                .map(o -> new Option(o.optionKey, o.optionContent)).toList(), relation.sortOrder, relation.score);
        }).toList();
        return new PaperView(row.id, row.createdBy, row.paperName, row.totalScore, row.status,
            row.createdAt, row.updatedAt, questions);
    }

    private ExamView examView(ExamRow row) {
        PaperRow paper = requirePaper(row.paperId);
        String status = statusAt(row, now());
        return new ExamView(row.id, row.paperId, row.examName, row.startAt, row.endAt, status,
            Boolean.TRUE.equals(row.published), row.createdBy, paper.totalScore,
            db.paperQuestionCount(row.paperId), row.createdAt, row.updatedAt);
    }

    private List<String> readAnswers(String value) {
        try {
            return json.readValue(value, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            throw new BusinessException(409, "题目标准答案格式无效");
        }
    }
}
