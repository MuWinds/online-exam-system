package com.onlineexam.answer;

import com.onlineexam.paper.ExamEligibilityGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Set;

@Configuration
@Profile("docker")
public class DockerAdapters {
    @Bean
    ExamEligibilityGateway dockerExamEligibility() {
        Set<Long> students = Set.of(100L, 101L);
        return (examId, studentId) -> students.contains(studentId);
    }
}
