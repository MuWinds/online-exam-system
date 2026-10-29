package com.onlineexam.task3;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.time.*;

@SpringBootApplication
@EnableScheduling
public class Task3Application {
    public static void main(String[] args) { SpringApplication.run(Task3Application.class,args); }
    @Bean Clock clock() { return Clock.system(ZoneId.of("Asia/Shanghai")); }
}
