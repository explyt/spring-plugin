package com.app;

import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.core.env.Environment;

public class MyBeanRegistrar implements BeanRegistrar {
    @Override
    public void register(BeanRegistry registry, Environment env) {
        registry.registerBean("foo", Foo.class);
        registry.registerBean(Bar.class);
        if (true) {
            registry.registerBean(Baz.class);
        }
    }
}
