package com.onlineexam.task3;

import org.springframework.context.annotation.*;
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
}
