package com.onlineexam.task2;

import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.onlineexam.task3.AnswerController;
import com.onlineexam.task3.IdentityGateway;
import static com.onlineexam.task2.PaperExamModels.*;

@RestController
@RequestMapping("/api")
public class PaperExamController {
    private final PaperExamService service;
    private final IdentityGateway identity;

    public PaperExamController(PaperExamService service, IdentityGateway identity) {
        this.service = service;
        this.identity = identity;
    }

    private <T> AnswerController.Response<T> ok(T value) {
        return new AnswerController.Response<>(200, "success", value);
    }

    @GetMapping("/questions")
    public AnswerController.Response<List<QuestionView>> questions(
        @RequestHeader(value = "Authorization", required = false) String token) {
        return ok(service.questions(identity.resolve(token)));
    }

    @GetMapping("/papers")
    public AnswerController.Response<List<PaperView>> papers(
        @RequestHeader(value = "Authorization", required = false) String token) {
        return ok(service.papers(identity.resolve(token)));
    }

    @PostMapping("/papers")
    public AnswerController.Response<PaperView> createPaper(
        @RequestHeader(value = "Authorization", required = false) String token,
        @RequestBody PaperRequest request) {
        return ok(service.createPaper(identity.resolve(token), request));
    }

    @GetMapping("/papers/{id}")
    public AnswerController.Response<PaperView> paper(
        @RequestHeader(value = "Authorization", required = false) String token,
        @PathVariable Long id) {
        return ok(service.paper(identity.resolve(token), id));
    }

    @PutMapping("/papers/{id}")
    public AnswerController.Response<PaperView> updatePaper(
        @RequestHeader(value = "Authorization", required = false) String token,
        @PathVariable Long id, @RequestBody PaperRequest request) {
        return ok(service.updatePaper(identity.resolve(token), id, request));
    }

    @PostMapping("/papers/{id}/questions")
    public AnswerController.Response<PaperView> addQuestion(
        @RequestHeader(value = "Authorization", required = false) String token,
        @PathVariable Long id, @RequestBody PaperQuestionRequest request) {
        return ok(service.addQuestion(identity.resolve(token), id, request));
    }

    @DeleteMapping("/papers/{id}/questions/{questionId}")
    public AnswerController.Response<PaperView> removeQuestion(
        @RequestHeader(value = "Authorization", required = false) String token,
        @PathVariable Long id, @PathVariable Long questionId) {
        return ok(service.removeQuestion(identity.resolve(token), id, questionId));
    }

    @PostMapping("/papers/{id}/publish")
    public AnswerController.Response<PaperView> publishPaper(
        @RequestHeader(value = "Authorization", required = false) String token,
        @PathVariable Long id) {
        return ok(service.publishPaper(identity.resolve(token), id));
    }

    @PostMapping("/papers/{id}/archive")
    public AnswerController.Response<PaperView> archivePaper(
        @RequestHeader(value = "Authorization", required = false) String token,
        @PathVariable Long id) {
        return ok(service.archivePaper(identity.resolve(token), id));
    }

    @GetMapping("/managed/exams")
    public AnswerController.Response<List<ExamView>> exams(
        @RequestHeader(value = "Authorization", required = false) String token) {
        return ok(service.exams(identity.resolve(token)));
    }

    @PostMapping("/managed/exams")
    public AnswerController.Response<ExamView> createExam(
        @RequestHeader(value = "Authorization", required = false) String token,
        @RequestBody ExamRequest request) {
        return ok(service.createExam(identity.resolve(token), request));
    }

    @GetMapping("/exams/{id}")
    public AnswerController.Response<ExamView> exam(
        @RequestHeader(value = "Authorization", required = false) String token,
        @PathVariable Long id) {
        return ok(service.exam(identity.resolve(token), id));
    }

    @PutMapping("/managed/exams/{id}")
    public AnswerController.Response<ExamView> updateExam(
        @RequestHeader(value = "Authorization", required = false) String token,
        @PathVariable Long id, @RequestBody ExamRequest request) {
        return ok(service.updateExam(identity.resolve(token), id, request));
    }

    @PostMapping("/managed/exams/{id}/publish")
    public AnswerController.Response<ExamView> publishExam(
        @RequestHeader(value = "Authorization", required = false) String token,
        @PathVariable Long id) {
        return ok(service.publishExam(identity.resolve(token), id));
    }

    @PostMapping("/managed/exams/{id}/unpublish")
    public AnswerController.Response<ExamView> unpublishExam(
        @RequestHeader(value = "Authorization", required = false) String token,
        @PathVariable Long id) {
        return ok(service.unpublishExam(identity.resolve(token), id));
    }

    @GetMapping("/exams/available")
    public AnswerController.Response<List<ExamView>> available(
        @RequestHeader(value = "Authorization", required = false) String token) {
        return ok(service.available(identity.resolve(token)));
    }
}
