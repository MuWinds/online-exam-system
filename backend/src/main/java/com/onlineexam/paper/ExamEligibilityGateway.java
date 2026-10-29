package com.onlineexam.paper;

public interface ExamEligibilityGateway {
    boolean isEligible(Long examId, Long studentId);
}
