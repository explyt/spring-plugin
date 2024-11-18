package com;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnProperty
@interface ConditionalOnPropertyA {
    @AliasFor(annotation = ConditionalOnProperty.class, value = "value")
    String[] value() default {};
    
    @AliasFor(annotation = ConditionalOnProperty.class, value = "name")
    String[] nameEx() default {};
    
    @AliasFor(annotation = ConditionalOnProperty.class, value = "prefix")
    String prefixEx() default "";
}

@Configuration
public class DependsOnPropertyConfiguration {
    
    @ConditionalOnPropertyA("")
    @Bean
    String valid1() {return "";}
    
    @ConditionalOnPropertyA(nameEx = "")
    @Bean
    String valid2() {return "";}
    
    @ConditionalOnProperty
    @Bean
    String valueIsNotSet1() {return "";}
    
    @ConditionalOnPropertyA(prefixEx = "")
    @Bean
    String valueIsNotSet2() {return "";}
    
}
