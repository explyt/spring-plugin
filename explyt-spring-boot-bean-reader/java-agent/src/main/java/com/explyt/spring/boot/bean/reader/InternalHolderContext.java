/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.boot.bean.reader;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import tech.ytsaurus.spyt.patch.annotations.AddClass;

import java.lang.reflect.Method;
import java.util.*;

@AddClass
public class InternalHolderContext {
    private static AbstractApplicationContext context;

    public static Map<String, AbstractApplicationContext> contexts = new HashMap<>();

    public static AbstractApplicationContext getContext() {
        return context;
    }

    public static Map<String, AbstractApplicationContext> getContexts() {
        return contexts;
    }

    public static ConfigurableEnvironment getEnvironment() {
        return context.getEnvironment();
    }

    public static ConfigurableListableBeanFactory getBeanFactory() {
        return context.getBeanFactory();
    }

    public static void addContext(AbstractApplicationContext context) {
        InternalHolderContext.context = context;
        contexts.put(context.getId(), context);
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
            List<String> result = new ArrayList<>(context.getBeanFactory().getBeanDefinitionCount());
            for (AbstractApplicationContext ctx : contexts.values()) {
                method.invoke(ctx, ctx.getBeanFactory(), result);
            }
            if (result.size() <= 1) return "";

            result = prepareResult(result, contexts);
            StringBuilder builder = new StringBuilder(1000);
            for (String rowBeanInfo : result) {
                builder.append(";").append(rowBeanInfo);
            }
            return builder.substring(1);
        } catch (Exception e) {
            return "error";
        }
    }

    private static List<String> prepareResult(List<String> result, Map<String, AbstractApplicationContext> contexts) {
        if (contexts.size() <= 1) return result;
        LinkedHashSet<String> beans = new LinkedHashSet<>(result.size() / 2);
        LinkedHashSet<String> aspects = new LinkedHashSet<>();
        for (String rowBeanData : result) {
            if (rowBeanData.startsWith(Constants.EXPLYT_BEAN_INFO)) {
                beans.add(rowBeanData);
            } else {
                aspects.add(rowBeanData);
            }
        }
        List<String> totalResult = new ArrayList<>(beans.size() + aspects.size());
        totalResult.addAll(beans);
        totalResult.addAll(aspects);
        return totalResult;
    }
}
