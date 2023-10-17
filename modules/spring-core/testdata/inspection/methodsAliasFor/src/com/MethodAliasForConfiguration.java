package com

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Configuration
public class MethodAliasForConfiguration {

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
    
    private final static java.lang.String EXISTING_METHOD = "value";
    private final static java.lang.String NOT_EXISTING_METHOD = "notExistingMethod";
    
    @AliasFor("unknown")
    String[] name() default {};
    
    @AliasFor(annotation=AncestorAnnotation.class, attribute="value")
    String[] value() default {};
    
    @AliasFor(annotation=AncestorAnnotation.class, attribute=EXISTING_METHOD)
    String[] value2() default {};
    
    @AliasFor(annotation=AncestorAnnotation.class, attribute=NOT_EXISTING_METHOD)
    String[] unknownConstForAncestor() default {};
    
    @AliasFor(annotation=AncestorAnnotation.class, attribute="unknownForAncestor")
    String[] unknownForAncestor() default {};
    
    @AliasFor(annotation=OtherAnnotation.class, attribute="value")
    String[] notMetaAnnotated() default {};
    
}
