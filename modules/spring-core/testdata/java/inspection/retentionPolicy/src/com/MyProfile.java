package com;

import org.springframework.context.annotation.Profile

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Profile("test")
@interface MyProfile {
    @AliasFor(annotation = Profile.class, value = "value")
    String[] profile();
}

@Target({ElementType.TYPE, ElementType.METHOD})
@Profile("test")
@interface MyProfileWithoutRetention {
    @AliasFor(annotation = Profile.class, value = "value")
    String[] profile();
}

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Profile("test")
@interface MyProfileWrongRetention {
    @AliasFor(annotation = Profile.class, value = "value")
    String[] profile();
}

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@MyProfile("test")
@interface MyProfileNested {
    @AliasFor(annotation = Profile.class, value = "value")
    String[] profile();
}

@Target({ElementType.TYPE, ElementType.METHOD})
@MyProfile("test")
@interface MyProfileNestedWithoutRetention {
    @AliasFor(annotation = Profile.class, value = "value")
    String[] profile();
}

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@MyProfile("test")
@interface MyProfileNestedWrongRetention {
    @AliasFor(annotation = Profile.class, value = "value")
    String[] profile();
}
@Target({ElementType.TYPE, ElementType.METHOD})
@interface NotASpringAnnotation {
    String[] value() default {};
}
