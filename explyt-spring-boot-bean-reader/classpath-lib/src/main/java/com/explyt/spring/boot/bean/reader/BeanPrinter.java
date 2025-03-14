/*
 * Copyright © 2024 Explyt Ltd
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

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.MethodMetadata;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class BeanPrinter {

    public static final String EXPLYT_BEAN_INFO_START = "ExplytBeanInfoStart";
    public static final String EXPLYT_BEAN_INFO_END = "ExplytBeanInfoEnd";
    public static final String EXPLYT_BEAN_INFO = "ExplytBeanInfo:";
    public static final String EXPLYT_AOP_INFO = "ExplytBeanAopInfo:";

    public static void printBeans(ConfigurableApplicationContext context) {
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        String[] definitionNames = beanFactory.getBeanDefinitionNames();

        System.out.println(EXPLYT_BEAN_INFO_START);
        Map<String, String> map = new TreeMap<>();
        for (String beanName : definitionNames) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            BeanInfo beanInfo = getBeanInfo(beanDefinition, beanName);
            if (beanInfo.className == null) continue;
            System.out.println(beanInfo);
            map.put(beanName, beanInfo.methodType != null ? beanInfo.methodType : beanInfo.className);
        }
        printAopData(beanFactory, map);
        System.out.println(EXPLYT_BEAN_INFO_END);
    }

    private static void printAopData(ConfigurableListableBeanFactory beanFactory, Map<String, String> map) {
        try {
            Class<?> aopReaderClass = Class.forName("com.explyt.spring.boot.bean.reader.SpringAopReader");
            Method printAopData = Arrays.stream(aopReaderClass.getMethods())
                    .filter(it -> it.getName().equals("printAopData"))
                    .findFirst().orElse(null);
            printAopData.invoke(null, beanFactory, map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static BeanInfo getBeanInfo(BeanDefinition definition, String definitionName) {
        BeanInfo beanInfo = new BeanInfo();
        beanInfo.primary = definition.isPrimary();
        beanInfo.className = definition.getBeanClassName();
        beanInfo.beanName = definitionName;
        beanInfo.scope = Optional.ofNullable(definition.getScope()).filter(it -> !it.isEmpty()).orElse("singleton");

        String beanClassName = definition.getBeanClassName();
        String factoryBeanObjectType = getFactoryBeanObjectType(definition);

        if (factoryBeanObjectType != null) {
            beanInfo.className = factoryBeanObjectType;
        } else if (beanClassName != null && definition instanceof AnnotatedBeanDefinition) {
            Optional<AnnotationMetadata> metadata = Optional.of(((AnnotatedBeanDefinition) definition).getMetadata());
            beanInfo.className = metadata.map(ClassMetadata::getClassName).orElse(beanClassName);
        } else if (definition instanceof AnnotatedBeanDefinition
                && ((AnnotatedBeanDefinition) definition).getFactoryMethodMetadata() != null) {
            MethodMetadata methodMetadata = ((AnnotatedBeanDefinition) definition).getFactoryMethodMetadata();
            beanInfo.className = methodMetadata.getDeclaringClassName();
            beanInfo.methodName = methodMetadata.getMethodName();
            beanInfo.methodType = methodMetadata.getReturnTypeName();
        } else if (definition instanceof RootBeanDefinition) {
            boolean isSpringData = fillSpringDataType((RootBeanDefinition) definition, beanInfo);
            beanInfo.rootBean = !isSpringData;
        }
        return beanInfo;
    }

    private static boolean fillSpringDataType(RootBeanDefinition definition, BeanInfo beanInfo) {
        Class<?> targetType = definition.getTargetType();
        if (targetType != null && targetType.getName().contains(".data")) {
            ResolvableType resolvableType = definition.getResolvableType();
            Class<?> baseDataRepoFactoryClass;
            try {
                baseDataRepoFactoryClass = Class.forName(
                        "org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport"
                );
                if (baseDataRepoFactoryClass.isAssignableFrom(targetType)) {
                    ResolvableType generic = resolvableType.getGeneric(0);
                    beanInfo.className = generic.toClass().getName();
                    return true;
                }
            } catch (Exception ignore) {
            }
        }
        return false;
    }

    private static String getFactoryBeanObjectType(BeanDefinition definition) {
        Object factoryBeanObjectType = definition.getAttribute("factoryBeanObjectType");
        if (factoryBeanObjectType instanceof String) {
            return (String) factoryBeanObjectType;
        } else if (factoryBeanObjectType instanceof Class) {
            return ((Class<?>) factoryBeanObjectType).getName();
        }
        return null;
    }

    static class BeanInfo {
        public String beanName;
        public String className;
        public String methodName;
        public String methodType;
        public String scope;
        public boolean primary;
        public boolean rootBean;

        @Override
        public String toString() {
            return EXPLYT_BEAN_INFO +
                    "{\"className\": \"" + className + "\"," +
                    "\"beanName\": \"" + beanName + "\"," +
                    "\"methodName\": " + getToStringValue(methodName) + "," +
                    "\"methodType\": " + getToStringValue(methodType) + "," +
                    "\"scope\": \"" + scope + "\"," +
                    "\"primary\": " + primary + "," +
                    "\"rootBean\": " + rootBean + "}";
        }
    }

    private static String getToStringValue(String value) {
        if (value == null) return "null";
        return "\"" + value + "\"";
    }
}
