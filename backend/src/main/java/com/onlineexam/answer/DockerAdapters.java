package com.onlineexam.answer;

import com.onlineexam.paper.ExamEligibilityGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Profile("docker")
public class DockerAdapters {
    @Bean
    ExamEligibilityGateway dockerExamEligibility(JdbcTemplate jdbc) {
        // Course exams are open to all active student accounts, not just seed IDs.
        return (examId, studentId) -> studentId != null && Integer.valueOf(1).equals(
            jdbc.queryForObject("""
                SELECT COUNT(*) FROM sys_user u JOIN sys_role r ON r.id = u.role_id
                WHERE u.id = ? AND u.status = 1 AND u.deleted = 0 AND r.code = 'STUDENT'
                """, Integer.class, studentId));
    }
}
