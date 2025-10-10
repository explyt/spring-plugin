/*
 * Copyright © 2025 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.boot.bean.reader;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import tech.ytsaurus.spyt.patch.annotations.AddClass;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@AddClass
public class InternalHolderContext {
    private static AbstractApplicationContext context;

    public static AbstractApplicationContext getContext() {
        return context;
    }

    public static ConfigurableEnvironment getEnvironment() {
        return context.getEnvironment();
    }

    public static ConfigurableListableBeanFactory getBeanFactory() {
        return context.getBeanFactory();
    }

    public static String getRawBeanData() {
        if (context == null) return "";
        Class<?> aClass = context.getClass();
        Method[] declaredMethods = aClass.getMethods();
        Method method = Arrays.stream(declaredMethods)
                .filter(it -> "explytPrintBeans".equals(it.getName()))
                .findAny()
                .orElse(null);
        if (method == null) return "";
        try {
            ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
            List<String> result = new ArrayList<>(beanFactory.getBeanDefinitionCount());
            method.invoke(context, beanFactory, result);
            if (result.size() <= 1) return "";
            StringBuilder builder = new StringBuilder(1000);
            for (String rowBeanInfo : result) {
                builder.append(";").append(rowBeanInfo);
            }
            return builder.substring(1);
        } catch (Exception e) {
            return "error";
        }
    }
}
