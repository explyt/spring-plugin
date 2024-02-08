package com;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Configuration
public class InterchangeableAliasForConfiguration {

    @Bean(value="iafc", name="iafc")
    InterchangeableAliasClass someMethod() {}
    
    @Bean(value="iafc2")
    InterchangeableAliasClass2 someMethod2() {}
    
    @BeanA(renamedValue="iafc3", name="iaf3c")
    InterchangeableAliasClas3s someMetho3d() {}
    
}

class InterchangeableAliasClass {}
class InterchangeableAliasClass2 {}

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Bean
@interface BeanA {
    @AliasFor(annotation = Bean.class, value = "value")
    String[] renamedValue() default {};
    
    @AliasFor(annotation = Bean.class, value = "name")
    String[] name() default {};
    
}