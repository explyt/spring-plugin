package org.springframework.beans.factory;

import java.util.function.Consumer;

public interface BeanRegistry {
    <T> void registerBean(Class<T> beanClass);
    <T> void registerBean(String name, Class<T> beanClass);
    <T> void registerBean(String name, Class<T> beanClass, Consumer<Object> spec);
}
