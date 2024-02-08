package com;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Profile
@interface ProfileA {
}

@Profile
@ProfileA
@Configuration
public class DependsOnPropertyConfiguration {
    
    @Profile
    @ProfileA
    @Bean
    String valid() {return "";}
    
    @Profile
    @ProfileA
    String notABean() {return "";}
    
}

@Profile
@ProfileA
public class NotAComponent { }

