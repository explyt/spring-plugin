package org.springframework.beans.factory;

import java.util.function.Supplier;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;

public interface BeanRegistry {
    <T> void registerBean(String name, Class<T> beanClass, BeanDefinitionCustomizer... customizers);
    <T> void registerBean(Class<T> beanClass, BeanDefinitionCustomizer... customizers);
    <T> void registerBean(String name, Class<T> beanClass, Supplier<T> supplier, BeanDefinitionCustomizer... customizers);
    <T> void registerBean(Class<T> beanClass, Supplier<T> supplier, BeanDefinitionCustomizer... customizers);
}
