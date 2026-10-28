package com.onlineexam.task3;

import com.onlineexam.task2.ExamEligibilityGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Set;

import static com.onlineexam.task3.Models.Actor;

@Configuration
@Profile("docker")
public class DockerAdapters {
    @Bean
    IdentityGateway dockerIdentity() {
        return header -> {
            if (header == null) throw new BusinessException(401, "请先登录");
            return switch (header) {
                case "Bearer demo-student" -> new Actor(100L, "STUDENT");
                case "Bearer demo-student-2" -> new Actor(101L, "STUDENT");
                case "Bearer demo-teacher" -> new Actor(1L, "TEACHER");
                default -> throw new BusinessException(401, "无效的演示凭证");
            };
        };
    }

    @Bean
    ExamEligibilityGateway dockerExamEligibility() {
        Set<Long> students = Set.of(100L, 101L);
        return (examId, studentId) -> students.contains(studentId);
    }
}
