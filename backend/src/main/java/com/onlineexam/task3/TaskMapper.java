package com.onlineexam.task3;

import org.apache.ibatis.annotations.*;
import java.util.*;
import java.math.BigDecimal;
import static com.onlineexam.task3.Models.*;

@Mapper
public interface TaskMapper {
    @Insert("INSERT INTO exam_attempt(exam_id,student_id,status,started_at,deadline_at,paper_snapshot,version) VALUES(#{examId},#{studentId},'ANSWERING',#{startedAt},#{deadlineAt},#{paperSnapshot},0) ON DUPLICATE KEY UPDATE id=id")
    void create(Attempt a);
    @Select("SELECT * FROM exam_attempt WHERE exam_id=#{examId} AND student_id=#{studentId}")
    Attempt findExisting(@Param("examId") Long examId,@Param("studentId") Long studentId);
    @Select("SELECT * FROM exam_attempt WHERE exam_id=#{examId} AND student_id=#{studentId} FOR UPDATE")
    Attempt find(@Param("examId") Long examId,@Param("studentId") Long studentId);
    @Select("SELECT * FROM exam_attempt WHERE id=#{id} FOR UPDATE") Attempt lock(Long id);
    @Select("SELECT * FROM exam_attempt WHERE student_id=#{studentId} ORDER BY id DESC") List<Attempt> mine(Long studentId);
    @Select("SELECT * FROM exam_attempt WHERE exam_id=#{examId} ORDER BY id") List<Attempt> forExam(Long examId);
    @Select("SELECT id FROM exam_attempt WHERE status='ANSWERING' AND deadline_at <= #{now} ORDER BY id") List<Long> expired(java.time.LocalDateTime now);
    @Update("UPDATE exam_attempt SET status=#{status},submission_type=#{submissionType},submitted_at=#{submittedAt},version=#{version},updated_at=CURRENT_TIMESTAMP WHERE id=#{id}") void update(Attempt a);
    @Delete("DELETE FROM exam_answer WHERE attempt_id=#{attemptId} AND question_id=#{questionId}")
    void deleteAnswer(@Param("attemptId") Long attemptId,@Param("questionId") Long questionId);
    @Insert("INSERT INTO exam_answer(attempt_id,question_id,answer_content) VALUES(#{attemptId},#{questionId},#{content})")
    void answer(@Param("attemptId") Long attemptId,@Param("questionId") Long questionId,@Param("content") String content);
    @Select("SELECT question_id,answer_content FROM exam_answer WHERE attempt_id=#{id}") List<StoredAnswer> answers(Long id);
    @Update("UPDATE exam_answer SET score=#{score},updated_at=CURRENT_TIMESTAMP WHERE attempt_id=#{attemptId} AND question_id=#{questionId}")
    void grade(@Param("attemptId") Long attemptId,@Param("questionId") Long questionId,@Param("score") BigDecimal score);
    @Insert("INSERT INTO exam_score(attempt_id,total_score,max_score) VALUES(#{attemptId},#{score},#{max})")
    void score(@Param("attemptId") Long attemptId,@Param("score") BigDecimal score,@Param("max") BigDecimal max);
    @Select("SELECT total_score FROM exam_score WHERE attempt_id=#{id}") BigDecimal total(Long id);
}
