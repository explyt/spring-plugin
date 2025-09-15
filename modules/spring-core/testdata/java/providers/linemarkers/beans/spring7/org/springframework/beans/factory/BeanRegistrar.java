package org.springframework.beans.factory;

import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.core.env.Environment;

public interface BeanRegistrar {
    void register(BeanRegistry registry, Environment env);
}
