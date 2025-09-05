package org.springframework.beans.factory;

import org.springframework.core.env.Environment;

public interface BeanRegistrar {
    void register(BeanRegistry registry, Environment env);
}
