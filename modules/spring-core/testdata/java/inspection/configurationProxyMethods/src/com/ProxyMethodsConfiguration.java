package com;

import lombok.Getter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

class ProxyBeanA {
}

@Getter
class ProxyBeanAA {
    private final ProxyBeanA proxyBeanA;

    ProxyBeanAA(ProxyBeanA proxyBeanA) {
        this.proxyBeanA = proxyBeanA;
    }

}

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Configuration
@interface ConfigurationWithAlias {
    @AliasFor(
            annotation = Configuration.class
    )
    boolean proxyBeanMethods() default true;
}

@ConfigurationWithAlias(proxyBeanMethods = false)
public class ProxyMethodsConfiguration {
    @Bean
    ProxyBeanA proxyBeanA() {
        return new ProxyBeanA();
    }

    @Bean
    ProxyBeanAA proxyBeanAAIncorrect() {
        return new ProxyBeanAA(proxyBeanA()); // -> <strong>incorrect call</strong>
    }

    @Bean
    ProxyBeanAA proxyBeanAACorrect(ProxyBeanA proxyBeanA) { // -> <strong>correct injected instance</strong>
        return new ProxyBeanAA(proxyBeanA);
    }
}

@Component
class ProxyMethodsComponent {
    @Bean
    public ProxyBeanA proxyBeanAFromComponent() {
        return new ProxyBeanA();
    }

    @Bean
    public ProxyBeanAA proxyBeanAA() {
        return new ProxyBeanAA(proxyBeanAFromComponent()); // -> <strong>incorrect call</strong>
    }
}
