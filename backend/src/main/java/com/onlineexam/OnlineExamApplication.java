package com.onlineexam;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.mybatis.spring.annotation.MapperScan;
import org.apache.ibatis.annotations.Mapper;
import java.time.*;

@SpringBootApplication(scanBasePackages = "com.onlineexam")
@MapperScan(basePackages = "com.onlineexam", annotationClass = Mapper.class)
@EnableScheduling
public class OnlineExamApplication {
    public static void main(String[] args) { SpringApplication.run(OnlineExamApplication.class,args); }
    @Bean Clock clock() { return Clock.system(ZoneId.of("Asia/Shanghai")); }
}
