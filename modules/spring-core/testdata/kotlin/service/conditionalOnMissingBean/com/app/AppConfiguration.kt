package com.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
class AppConfiguration {
    @Bean
    fun testClass():TestClass {
        return TestClass()
    }
}
