package com;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Configuration
public class AliasAnnotationConfiguration {

    @InheritorAnnotation
    @Bean
    MethodAliasClass someMethod() {}
    
}

class MethodAliasClass {}

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@interface AncestorAnnotation {
    @AliasFor("value")
    String[] name() default {};
    
    @AliasFor("name")
    String[] value() default {};
    
}

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@interface OtherAnnotation {
    @AliasFor("value")
    String[] name() default {};
    
    @AliasFor("name")
    String[] value() default {};
}

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@AncestorAnnotation
@interface InheritorAnnotation {
    
    @AliasFor(annotation=AncestorAnnotation.class, attribute="value")
    String[] valid() default {};
    
    @AliasFor(annotation=MethodAliasClass.class)
    String[] notAnAnnotation() default {};
    
    @AliasFor(annotation=OtherAnnotation.class, attribute="value")
    String[] notMetaAnnotated() default {};
    
}
