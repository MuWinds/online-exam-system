package com.onlineexam.answer;

import static com.onlineexam.answer.Models.*;

/** Task2 integration point. Must enforce student enrollment and return server-side correct answers. */
public interface ExamGateway {
    Paper forStudent(Long examId, Long studentId);
    Paper forManager(Long examId, Actor actor);
}
