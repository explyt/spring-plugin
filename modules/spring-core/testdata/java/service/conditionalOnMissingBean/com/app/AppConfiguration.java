package com.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class AppConfiguration {
    @Bean
    public TestClass testClass() {
        return new TestClass();
    }
}
