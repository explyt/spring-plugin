package com;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

interface I {
    void existingMethod();
}

abstract class A implements I {}

@Component
class B extends A {
    public void existingMethod() {}
}

@Configuration
public class MethodsInitDestroyConfiguration {
    private final static String EXISTING_METHOD = "existingMethod";
    private final static String NOT_EXISTING_METHOD = "notExistingMethod";

    @Bean I i() { return new B(); }
    @Bean() I i1() { return new B(); }
    @Bean(initMethod="") I i2() { return new B(); }
    @Bean(initMethod="") I i3() { return new B(); }
    @Bean(destroyMethod="") I i4() { return new B(); }
    @Bean(destroyMethod=EXISTING_METHOD) I i5() { return new B(); }
    @Bean(destroyMethod=NOT_EXISTING_METHOD) I i6() { return new B(); } //expecting error
    @Bean(initMethod=EXISTING_METHOD, destroyMethod=EXISTING_METHOD) A a() { return new B(); }
    @Bean B b() { return new B(); }
    @Bean(initMethod=EXISTING_METHOD) B b1() { return new B(); }
    @Bean(initMethod=NOT_EXISTING_METHOD) B b2() { return new B(); } //expecting error
    @Bean(destroyMethod="foo") B b3() { return new B(); } //expecting error
}