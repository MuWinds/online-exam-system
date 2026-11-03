package com.onlineexam.answer;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class DockerEligibilityTest {
    @Test void databaseRejectsTeachersDisabledDeletedAndMissingUsers() {
        var source = new org.springframework.jdbc.datasource.DriverManagerDataSource(
            "jdbc:h2:mem:eligibility;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE sys_role(id BIGINT PRIMARY KEY, code VARCHAR(32))");
        jdbc.execute("CREATE TABLE sys_user(id BIGINT PRIMARY KEY, role_id BIGINT, status INT, deleted INT)");
        jdbc.update("INSERT INTO sys_role VALUES(1,'STUDENT'),(2,'TEACHER')");
        jdbc.update("INSERT INTO sys_user VALUES(999,1,1,0),(1000,1,0,0),(1001,1,1,1),(1002,2,1,0)");
        var gateway = new DockerAdapters().dockerExamEligibility(jdbc);
        assertTrue(gateway.isEligible(1L,999L));
        for (long id : new long[]{1000,1001,1002,1003}) assertFalse(gateway.isEligible(1L,id));
    }
    @Test void checksLiveStudentRoleAndAccountStatusInsteadOfDemoIds() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        var gateway = new DockerAdapters().dockerExamEligibility(jdbc);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(999L))).thenReturn(1);
        assertTrue(gateway.isEligible(1L, 999L));
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(999L))).thenReturn(0);
        assertFalse(gateway.isEligible(1L, 999L));
        assertFalse(gateway.isEligible(1L, null));
        verify(jdbc, times(2)).queryForObject(contains("u.status = 1"), eq(Integer.class), eq(999L));
    }
}
