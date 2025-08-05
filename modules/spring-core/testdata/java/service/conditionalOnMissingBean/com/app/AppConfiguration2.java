package com.app;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfiguration2 {
    @Bean
    @ConditionalOnMissingBean(TestClass.class)
    public TestClass testClass() {
        return new TestClass();
    }
}
