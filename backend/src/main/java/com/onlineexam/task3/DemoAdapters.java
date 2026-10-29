package com.onlineexam.task3;

import org.springframework.context.annotation.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static com.onlineexam.task3.Models.*;

@Configuration
@Profile("demo")
public class DemoAdapters {
    @Bean IdentityGateway demoIdentity() {
        return header -> {
            if(header==null) throw new BusinessException(401,"请先登录");
            return switch(header) {
                case "Bearer demo-student" -> new Actor(100L,"STUDENT");
                case "Bearer demo-student-2" -> new Actor(101L,"STUDENT");
                case "Bearer demo-teacher" -> new Actor(1L,"TEACHER");
                default -> throw new BusinessException(401,"无效的演示凭证");
            };
        };
    }
    @Bean ExamGateway demoExams(Clock clock) {
        return new ExamGateway() {
            private Paper paper(Long id) {
                if(id==null || (id!=1L && id!=2L)) throw new BusinessException(404,"考试不存在");
                LocalDateTime today=LocalDate.now(clock).atStartOfDay();
                return new Paper(id,"在线考试演示",1L,true,id==2L?today.plusDays(1):today,today.plusDays(2),List.of(
                    new Question(11L,"SINGLE_CHOICE","Java 中用于定义类的关键字是？",List.of(new Option("A","class"),new Option("B","def")),new BigDecimal("30.00"),List.of("A")),
                    new Question(12L,"MULTIPLE_CHOICE","请选择关系型数据库",List.of(new Option("A","MySQL"),new Option("B","PostgreSQL"),new Option("C","Redis")),new BigDecimal("40.00"),List.of("A","B")),
                    new Question(13L,"JUDGE","正式成绩应由后端计算。",List.of(new Option("TRUE","正确"),new Option("FALSE","错误")),new BigDecimal("30.00"),List.of("TRUE"))));
            }
            public Paper forStudent(Long examId,Long studentId) {
                if(!Set.of(100L,101L).contains(studentId)) throw new BusinessException(403,"未报名演示考试");
                return paper(examId);
            }
            public Paper forManager(Long examId,Actor a) {
                Paper p=paper(examId);
                if(!"ADMIN".equals(a.role()) && !p.teacherId().equals(a.id())) throw new BusinessException(403,"无权管理本考试");
                return p;
            }
        };
    }
}
