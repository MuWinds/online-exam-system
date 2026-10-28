package com.onlineexam.task3;

import static com.onlineexam.task3.Models.*;

/** Task2 integration point. Must enforce student enrollment and return server-side correct answers. */
public interface ExamGateway {
    Paper forStudent(Long examId, Long studentId);
    Paper forManager(Long examId, Actor actor);
}
