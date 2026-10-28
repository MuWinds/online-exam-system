package com.onlineexam.task2;

public interface ExamEligibilityGateway {
    boolean isEligible(Long examId, Long studentId);
}
