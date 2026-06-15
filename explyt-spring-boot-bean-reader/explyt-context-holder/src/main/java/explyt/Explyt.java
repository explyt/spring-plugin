/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package explyt;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import tech.ytsaurus.spyt.patch.annotations.AddClass;

import java.util.HashMap;
import java.util.Map;

/**
 * it should be equals to Explyt in explyt-java-agent module.
 * Need to added to project classpath with provided scope for evaluation.
 */
@AddClass
public class Explyt {
    public static AbstractApplicationContext context;

    public static Map<String, AbstractApplicationContext> contexts = new HashMap<>();

    public static AbstractApplicationContext getContext() {
        return context;
    }

    public static ConfigurableEnvironment getEnvironment() {
        return context.getEnvironment();
    }

    public static ConfigurableListableBeanFactory getBeanFactory() {
        return context.getBeanFactory();
    }

    public static void addContext(AbstractApplicationContext context) {
        Explyt.context = context;
        Explyt.contexts.put(context.getId(), context);
    }

    public static <T> T getBean(Class<T> tClass) {
        for (AbstractApplicationContext ctx : contexts.values()) {
            try {
                T bean = ctx.getBean(tClass);
                if (bean != null) {
                    return bean;
                }
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    public static String getProperty(String propertyName) {
        for (AbstractApplicationContext ctx : contexts.values()) {
            try {
                String property = ctx.getEnvironment().getProperty(propertyName);
                if (property != null) {
                    return property;
                }
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    public static BeanDefinition getBeanDefinition(String beanName) {
        for (AbstractApplicationContext ctx : contexts.values()) {
            try {
                if (ctx instanceof GenericApplicationContext) {
                    BeanDefinition definition = ((GenericApplicationContext) ctx).getBeanDefinition(beanName);
                    if (definition != null) {
                        return definition;
                    }
                }
            } catch (Exception ignore) {
            }
        }
        return null;
    }
}
