package com.onlineexam.paper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Set;

@Configuration
@Profile("demo")
public class DemoEnrollmentAdapter {
    @Bean
    ExamEligibilityGateway demoExamEligibility() {
        Set<Long> demoStudents = Set.of(100L, 101L);
        return (examId, studentId) -> demoStudents.contains(studentId);
    }
}
