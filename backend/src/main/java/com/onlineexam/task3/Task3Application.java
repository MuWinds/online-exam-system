package com.onlineexam.task3;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.mybatis.spring.annotation.MapperScan;
import org.apache.ibatis.annotations.Mapper;
import java.time.*;

@SpringBootApplication(scanBasePackages = {"com.onlineexam.task2", "com.onlineexam.task3"})
@MapperScan(basePackages = {"com.onlineexam.task2", "com.onlineexam.task3"}, annotationClass = Mapper.class)
@EnableScheduling
public class Task3Application {
    public static void main(String[] args) { SpringApplication.run(Task3Application.class,args); }
    @Bean Clock clock() { return Clock.system(ZoneId.of("Asia/Shanghai")); }
}
