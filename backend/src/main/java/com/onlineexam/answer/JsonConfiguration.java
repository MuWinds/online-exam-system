package com.onlineexam.answer;

import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import java.time.format.DateTimeFormatter;

@Configuration
public class JsonConfiguration {
    @Bean Jackson2ObjectMapperBuilderCustomizer dateFormat() {
        var format=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return builder->builder.serializers(new LocalDateTimeSerializer(format)).deserializers(new LocalDateTimeDeserializer(format));
    }
}
