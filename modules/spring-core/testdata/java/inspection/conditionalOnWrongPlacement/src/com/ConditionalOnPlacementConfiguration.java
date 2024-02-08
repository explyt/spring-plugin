package com;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
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
}

@ConditionalOnPropertyA
@ConditionalOnBean
@ConditionalOnClass
@ConditionalOnMissingBean
@ConditionalOnMissingClass
@ConditionalOnProperty
@Configuration
class DependsOnPropertyConfiguration {
    
    @ConditionalOnPropertyA
    @ConditionalOnBean
    @ConditionalOnClass
    @ConditionalOnMissingBean
    @ConditionalOnMissingClass
    @ConditionalOnProperty
    @Bean
    String valid() {return "";}
    
    @ConditionalOnPropertyA
    @ConditionalOnBean
    @ConditionalOnClass
    @ConditionalOnMissingBean
    @ConditionalOnMissingClass
    @ConditionalOnProperty
    String notABean() {return "";}
    
}

@ConditionalOnPropertyA
@ConditionalOnBean
@ConditionalOnClass
@ConditionalOnMissingBean
@ConditionalOnMissingClass
@ConditionalOnProperty
class NotAComponent { }

