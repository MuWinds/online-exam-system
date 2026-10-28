package com.onlineexam.task3;

import org.springframework.web.bind.annotation.*;
import static com.onlineexam.task3.Models.*;

@RestController
@RequestMapping("/api")
public class AnswerController {
    private final AnswerService service;
    private final IdentityGateway identity;
    public AnswerController(AnswerService service,IdentityGateway identity) {this.service=service;this.identity=identity;}
    public record Response<T>(int code,String message,T data) {}
    private <T> Response<T> ok(T value) {return new Response<>(200,"success",value);}
    @PostMapping("/exams/{examId}/start")
    public Response<Context> start(@RequestHeader(value="Authorization",required=false) String token,@PathVariable Long examId) {return ok(service.start(identity.resolve(token),examId));}
    @GetMapping("/attempts/{id}")
    public Response<Context> context(@RequestHeader(value="Authorization",required=false) String token,@PathVariable Long id) {return ok(service.context(identity.resolve(token),id));}
    @PutMapping("/attempts/{id}/answers")
    public Response<Context> save(@RequestHeader(value="Authorization",required=false) String token,@PathVariable Long id,@RequestBody SaveRequest request) {return ok(service.save(identity.resolve(token),id,request));}
    @PostMapping("/attempts/{id}/submit")
    public Response<Result> submit(@RequestHeader(value="Authorization",required=false) String token,@PathVariable Long id) {return ok(service.submit(identity.resolve(token),id));}
    @GetMapping("/attempts/{id}/result")
    public Response<Result> result(@RequestHeader(value="Authorization",required=false) String token,@PathVariable Long id) {return ok(service.result(identity.resolve(token),id));}
    @GetMapping("/scores/my")
    public Response<Page<Result>> mine(@RequestHeader(value="Authorization",required=false) String token,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size) {return ok(service.myScores(identity.resolve(token),page,size));}
    @GetMapping("/exams/{examId}/statistics")
    public Response<Statistics> statistics(@RequestHeader(value="Authorization",required=false) String token,@PathVariable Long examId) {return ok(service.statistics(identity.resolve(token),examId));}
}
