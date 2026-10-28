package com.onlineexam.task3;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import java.math.*;
import java.time.*;
import java.util.*;
import static com.onlineexam.task3.Models.*;

@Service
@Transactional(noRollbackFor=BusinessException.class)
public class AnswerService {
    private final TaskMapper db;
    private final ExamGateway exams;
    private final ObjectMapper json;
    private final Clock clock;
    public AnswerService(TaskMapper db, ExamGateway exams, ObjectMapper json, Clock clock) {
        this.db=db; this.exams=exams; this.json=json; this.clock=clock;
    }
    private LocalDateTime now() { return LocalDateTime.now(clock).withNano(0); }
    private void student(Actor a) {
        if (a==null || a.id()==null) throw new BusinessException(401,"请先登录");
        if (!"STUDENT".equals(a.role())) throw new BusinessException(403,"仅学生可以作答");
    }
    public Context start(Actor actor, Long examId) {
        student(actor);
        Paper paper=exams.forStudent(examId,actor.id());
        var existing=db.findExisting(examId,actor.id());
        if(existing!=null) { existing=db.lock(existing.id); finishIfExpired(existing); return view(existing); }
        if(!paper.published() || now().isBefore(paper.startsAt()) || !now().isBefore(paper.endsAt()))
            throw new BusinessException(409,"考试未开始、未发布或已结束");
        if(paper.questions().isEmpty()) throw new BusinessException(409,"试卷没有题目");
        Set<Long> questionIds=new HashSet<>();
        BigDecimal full=BigDecimal.ZERO;
        for(Question q:paper.questions()) {
            if(q.questionId()==null || !questionIds.add(q.questionId()) || q.score()==null || q.score().signum()<=0 || q.score().scale()>2
                || !Set.of("SINGLE_CHOICE","MULTIPLE_CHOICE","JUDGE").contains(q.type()) || q.correctAnswer()==null || q.correctAnswer().isEmpty())
                throw new BusinessException(409,"试卷数据或评分规则无效，请联系教师");
            full=full.add(q.score());
        }
        if(full.compareTo(new BigDecimal("9999.99"))>0) throw new BusinessException(409,"试卷总分超出存储范围");
        Attempt a=new Attempt(); a.examId=examId; a.studentId=actor.id(); a.startedAt=now();
        a.deadlineAt=paper.endsAt(); a.paperSnapshot=write(paper);
        db.create(a);
        return view(db.find(examId,actor.id()));
    }
    private Attempt owned(Actor actor, Long id) {
        student(actor);
        Attempt a=db.lock(id);
        if(a==null) throw new BusinessException(404,"答卷不存在");
        if(!a.studentId.equals(actor.id())) throw new BusinessException(403,"不能访问其他学生的答卷");
        return a;
    }
    public Context context(Actor actor,Long id) {
        Attempt a=owned(actor,id); finishIfExpired(a); return view(a);
    }
    public Context save(Actor actor,Long id,SaveRequest request) {
        Attempt a=owned(actor,id); finishIfExpired(a);
        if(!"ANSWERING".equals(a.status)) throw new BusinessException(409,"已交卷，不能保存");
        if(request==null || request.answers()==null || request.version()==null) throw new BusinessException(400,"需要答案列表和版本号");
        if(request.version()!=a.version) throw new BusinessException(409,"答案版本已更新，请重新载入");
        Paper paper=read(a.paperSnapshot,Paper.class);
        Map<Long,Question> questions=new HashMap<>(); paper.questions().forEach(q->questions.put(q.questionId(),q));
        Set<Long> seen=new HashSet<>();
        // Validate the entire batch before any writes.
        for(AnswerInput answer:request.answers()) {
            if(answer==null || answer.questionId()==null || !seen.add(answer.questionId()) || !questions.containsKey(answer.questionId()))
                throw new BusinessException(400,"题目不属于本试卷或题目重复");
            Question q=questions.get(answer.questionId());
            List<String> value=answer.answer();
            if(value==null || value.stream().anyMatch(Objects::isNull) || new HashSet<>(value).size()!=value.size()) throw new BusinessException(400,"答案格式无效");
            if(!"MULTIPLE_CHOICE".equals(q.type()) && value.size()>1) throw new BusinessException(400,"本题只能选择一个选项");
            Set<String> keys=new HashSet<>(); q.options().forEach(o->keys.add(o.key()));
            if(!keys.containsAll(value)) throw new BusinessException(400,"包含无效选项");
        }
        for(AnswerInput answer:request.answers()) {
            db.deleteAnswer(id,answer.questionId()); db.answer(id,answer.questionId(),write(answer.answer()));
        }
        a.version++; db.update(a); return view(a);
    }
    public Result submit(Actor actor,Long id) {
        Attempt a=owned(actor,id);
        if("ANSWERING".equals(a.status)) finish(a,!now().isBefore(a.deadlineAt)?"AUTO_SUBMITTED":"SUBMITTED");
        return resultView(a);
    }
    public Result result(Actor actor,Long id) {
        Attempt a=owned(actor,id); finishIfExpired(a);
        if(!"GRADED".equals(a.status)) throw new BusinessException(409,"答卷尚未交卷");
        return resultView(a);
    }
    private void finishIfExpired(Attempt a) {
        if("ANSWERING".equals(a.status) && !now().isBefore(a.deadlineAt)) finish(a,"AUTO_SUBMITTED");
    }
    private void finish(Attempt a,String type) {
        Paper paper=read(a.paperSnapshot,Paper.class);
        Map<Long,List<String>> answers=answers(a.id);
        BigDecimal total=BigDecimal.ZERO, max=BigDecimal.ZERO;
        for(Question q:paper.questions()) {
            BigDecimal score=Grading.score(q.type(),answers.getOrDefault(q.questionId(),List.of()),q.correctAnswer(),q.score());
            if(!answers.containsKey(q.questionId())) db.answer(a.id,q.questionId(),"[]");
            db.grade(a.id,q.questionId(),score); total=total.add(score); max=max.add(q.score());
        }
        db.score(a.id,total,max);
        a.status="GRADED"; a.submissionType=type;
        a.submittedAt="AUTO_SUBMITTED".equals(type)?a.deadlineAt:now(); a.version++; db.update(a);
    }
    @Scheduled(fixedDelayString="${task3.expire-interval-ms:5000}")
    public void expire() { for(Long id:db.expired(now())) { Attempt a=db.lock(id); if(a!=null) finishIfExpired(a); } }
    public Page<Result> myScores(Actor actor,int page,int size) {
        student(actor);
        if(page<1 || size<1 || size>100) throw new BusinessException(400,"分页参数无效，size范围1到100");
        List<Attempt> all=db.mine(actor.id());
        // Lock in ascending ID order across requests to avoid lock inversion.
        all.stream().sorted(Comparator.comparing(a->a.id)).forEach(a->{Attempt locked=db.lock(a.id);finishIfExpired(locked);});
        List<Result> results=db.mine(actor.id()).stream().filter(a->"GRADED".equals(a.status)).map(this::resultView).toList();
        long from=(long)(page-1)*size;
        return new Page<>(results.stream().skip(from).limit(size).toList(),results.size(),page,size);
    }
    public Statistics statistics(Actor actor,Long examId) {
        if(actor==null) throw new BusinessException(401,"请先登录");
        if(!Set.of("TEACHER","ADMIN").contains(actor.role())) throw new BusinessException(403,"无统计权限");
        exams.forManager(examId,actor);
        List<Attempt> attempts=db.forExam(examId);
        for(Attempt a:attempts) { Attempt locked=db.lock(a.id); finishIfExpired(locked); }
        List<Result> results=db.forExam(examId).stream().filter(a->"GRADED".equals(a.status)).map(this::resultView).toList();
        Map<String,Long> bins=new LinkedHashMap<>(); for(String key:List.of("0-59","60-69","70-79","80-89","90-100")) bins.put(key,0L);
        long pass=0; BigDecimal sum=BigDecimal.ZERO, min=null,max=null;
        for(Result r:results) {
            BigDecimal percent=r.maxScore().signum()==0?BigDecimal.ZERO:r.totalScore().multiply(new BigDecimal("100")).divide(r.maxScore(),4,RoundingMode.HALF_UP);
            if(percent.compareTo(new BigDecimal("60"))>=0) pass++;
            String key=percent.intValue()<60?"0-59":percent.intValue()<70?"60-69":percent.intValue()<80?"70-79":percent.intValue()<90?"80-89":"90-100";
            bins.put(key,bins.get(key)+1); sum=sum.add(r.totalScore()); min=min==null?r.totalScore():min.min(r.totalScore()); max=max==null?r.totalScore():max.max(r.totalScore());
        }
        int n=results.size();
        return new Statistics(attempts.size(),n,n==0?null:sum.divide(BigDecimal.valueOf(n),2,RoundingMode.HALF_UP),max,min,pass,
            n==0?new BigDecimal("0.00"):BigDecimal.valueOf(pass*100).divide(BigDecimal.valueOf(n),2,RoundingMode.HALF_UP),bins);
    }
    private Context view(Attempt a) {
        Paper p=read(a.paperSnapshot,Paper.class);
        return new Context(a.id,a.examId,p.title(),a.status,a.deadlineAt,now(),a.version,
            p.questions().stream().map(q->new PublicQuestion(q.questionId(),q.type(),q.content(),q.options(),q.score())).toList(),answers(a.id));
    }
    private Result resultView(Attempt a) {
        Paper p=read(a.paperSnapshot,Paper.class);
        BigDecimal max=p.questions().stream().map(Question::score).reduce(BigDecimal.ZERO,BigDecimal::add).setScale(2);
        return new Result(a.id,a.examId,p.title(),a.studentId,db.total(a.id),max,a.submissionType,a.submittedAt);
    }
    private Map<Long,List<String>> answers(Long id) {
        Map<Long,List<String>> result=new LinkedHashMap<>();
        for(StoredAnswer a:db.answers(id)) {
            try { result.put(a.questionId,json.readValue(a.answerContent,new TypeReference<List<String>>(){})); }
            catch(Exception e) { throw new IllegalStateException("答案数据无法读取",e); }
        }
        return result;
    }
    private String write(Object value) { try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException(e);} }
    private <T> T read(String value,Class<T> type) { try{return json.readValue(value,type);}catch(Exception e){throw new IllegalStateException(e);} }
}
