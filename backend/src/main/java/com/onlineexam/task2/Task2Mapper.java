package com.onlineexam.task2;

import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

import static com.onlineexam.task2.PaperExamModels.*;

@Mapper
public interface Task2Mapper {
    @Select("""
        SELECT * FROM question
        WHERE status = 'ACTIVE'
        ORDER BY id
        """)
    List<QuestionRow> questions();

    @Select("""
        SELECT * FROM question
        WHERE created_by=#{createdBy} AND status = 'ACTIVE'
        ORDER BY id
        """)
    List<QuestionRow> questionsByOwner(Long createdBy);

    @Select("SELECT * FROM question WHERE id=#{id}")
    QuestionRow question(Long id);

    @Select("SELECT * FROM question_option WHERE question_id=#{questionId} ORDER BY sort_order, id")
    List<OptionRow> options(Long questionId);

    @Select("SELECT * FROM exam_paper WHERE id=#{id}")
    PaperRow paper(Long id);

    @Select("SELECT * FROM exam_paper WHERE created_by=#{createdBy} ORDER BY id DESC")
    List<PaperRow> papersByOwner(Long createdBy);

    @Select("SELECT * FROM exam_paper ORDER BY id DESC")
    List<PaperRow> allPapers();

    @Select("SELECT * FROM paper_question WHERE paper_id=#{paperId} ORDER BY sort_order, id")
    List<PaperQuestionRow> paperQuestions(Long paperId);

    @Insert("""
        INSERT INTO exam_paper(created_by, paper_name, total_score, status)
        VALUES(#{createdBy}, #{paperName}, #{totalScore}, 'DRAFT')
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertPaper(PaperRow paper);

    @Update("""
        UPDATE exam_paper
        SET paper_name=#{paperName}, total_score=#{totalScore}, status=#{status},
            updated_at=CURRENT_TIMESTAMP
        WHERE id=#{id}
        """)
    void updatePaper(PaperRow paper);

    @Insert("""
        INSERT INTO paper_question(paper_id, question_id, sort_order, score)
        VALUES(#{paperId}, #{questionId}, #{sortOrder}, #{score})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertPaperQuestion(PaperQuestionRow row);

    @Delete("DELETE FROM paper_question WHERE paper_id=#{paperId} AND question_id=#{questionId}")
    void deletePaperQuestion(@Param("paperId") Long paperId, @Param("questionId") Long questionId);

    @Select("""
        SELECT COUNT(*) FROM paper_question
        WHERE paper_id=#{paperId} AND (question_id=#{questionId} OR sort_order=#{sortOrder})
        """)
    int duplicatePaperQuestion(@Param("paperId") Long paperId, @Param("questionId") Long questionId,
                               @Param("sortOrder") Integer sortOrder);

    @Select("SELECT COALESCE(MAX(sort_order), 0) FROM paper_question WHERE paper_id=#{paperId}")
    int maxSortOrder(Long paperId);

    @Select("SELECT COALESCE(SUM(score), 0) FROM paper_question WHERE paper_id=#{paperId}")
    BigDecimal paperTotal(Long paperId);

    @Select("SELECT COUNT(*) FROM paper_question WHERE paper_id=#{paperId}")
    int paperQuestionCount(Long paperId);

    @Select("SELECT * FROM exam WHERE id=#{id}")
    ExamRow exam(Long id);

    @Select("SELECT * FROM exam WHERE created_by=#{createdBy} ORDER BY id DESC")
    List<ExamRow> examsByOwner(Long createdBy);

    @Select("SELECT * FROM exam ORDER BY id DESC")
    List<ExamRow> allExams();

    @Insert("""
        INSERT INTO exam(paper_id, created_by, exam_name, start_at, end_at, status, published)
        VALUES(#{paperId}, #{createdBy}, #{examName}, #{startAt}, #{endAt}, 'NOT_STARTED', FALSE)
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertExam(ExamRow exam);

    @Update("""
        UPDATE exam
        SET exam_name=#{examName}, start_at=#{startAt}, end_at=#{endAt},
            status=#{status}, published=#{published}, updated_at=CURRENT_TIMESTAMP
        WHERE id=#{id}
        """)
    void updateExam(ExamRow exam);

    @Select("SELECT COUNT(*) FROM exam WHERE paper_id=#{paperId}")
    int examCountForPaper(Long paperId);
}
