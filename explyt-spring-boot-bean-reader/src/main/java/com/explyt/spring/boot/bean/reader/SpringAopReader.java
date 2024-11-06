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

import org.springframework.aop.*;
import org.springframework.aop.aspectj.AspectJPrecedenceInformation;
import org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.stream.Collectors;

import static com.explyt.spring.boot.bean.reader.BeanPrinter.EXPLYT_AOP_INFO;


public class SpringAopReader {
    private static final String AOP_INFO_TEMPLATE = EXPLYT_AOP_INFO +
            "{\"aspectName\":\"%s\", \"aspectMethodName\":\"%s\", \"beanName\":\"%s\", \"methodName\":\"%s\", \"methodParams\":\"%s\"}";

    public static void printAopData(ConfigurableListableBeanFactory beanFactory, Map<String, String> beanClassByName) {
        try {
            Class.forName("org.aspectj.lang.annotation.Aspect");
        } catch (ClassNotFoundException e) {
            return;
        }
        List<Advisor> advisors = getAdvisors((AbstractBeanFactory) beanFactory);
        if (advisors.isEmpty()) return;
        String[] names = beanFactory.getBeanDefinitionNames();
        for (String beanName : names) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            Object source = beanDefinition.getSource();
            if (source == null || source.toString().startsWith("org.springframework.")) continue;
            Class<?> beanClass = getBeanClass(beanName, beanClassByName);
            if (beanClass == null) continue;
            checkMethods(beanClass, advisors, beanName);
        }
    }

    private static List<Advisor> getAdvisors(AbstractBeanFactory beanFactory) {
        List<BeanPostProcessor> beanPostProcessors = beanFactory.getBeanPostProcessors();
        for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
            if (beanPostProcessor instanceof AbstractAdvisorAutoProxyCreator) {
                try {
                    AbstractAdvisorAutoProxyCreator advisorCreator = (AbstractAdvisorAutoProxyCreator) beanPostProcessor;
                    Class<? extends AbstractAdvisorAutoProxyCreator> advisorClass = advisorCreator.getClass();
                    Field field = advisorClass.getDeclaredField("aspectJAdvisorsBuilder");
                    field.setAccessible(true);
                    Method findCandidateAdvisors = field.getType().getDeclaredMethod("buildAspectJAdvisors");
                    findCandidateAdvisors.setAccessible(true);
                    return (List<Advisor>) findCandidateAdvisors.invoke(field.get(advisorCreator));
                } catch (Exception e) {
                    return Collections.emptyList();
                }
            }
        }
        return Collections.emptyList();
    }

    private static List<Method> getApplyMethods(Pointcut pc, Class<?> targetClass) {
        if (pc == null || !pc.getClassFilter().matches(targetClass)) {
            return Collections.emptyList();
        }

        MethodMatcher methodMatcher = pc.getMethodMatcher();
        if (methodMatcher == MethodMatcher.TRUE) {
            // No need to iterate the methods if we're matching any method anyway...
            return Collections.emptyList();
        }

        IntroductionAwareMethodMatcher introductionAwareMethodMatcher = null;
        if (methodMatcher instanceof IntroductionAwareMethodMatcher) {
            introductionAwareMethodMatcher = (IntroductionAwareMethodMatcher) methodMatcher;
        }

        Set<Class<?>> classes = new LinkedHashSet<>();
        if (!Proxy.isProxyClass(targetClass)) {
            classes.add(ClassUtils.getUserClass(targetClass));
        }
        classes.addAll(ClassUtils.getAllInterfacesForClassAsSet(targetClass));

        List<Method> result = new ArrayList<>();
        for (Class<?> clazz : classes) {
            Method[] methods = ReflectionUtils.getAllDeclaredMethods(clazz);
            for (Method method : methods) {
                if (introductionAwareMethodMatcher != null ?
                        introductionAwareMethodMatcher.matches(method, targetClass, false) :
                        methodMatcher.matches(method, targetClass)) {
                    result.add(method);
                }
            }
        }

        return result;
    }

    private static Class<?> getBeanClass(String beanName, Map<String, String> beanClassByName) {
        return Optional.ofNullable(beanClassByName.get(beanName))
                .map(SpringAopReader::getBeanClass).orElse(null);
    }

    private static Class<?> getBeanClass(String beanClassName) {
        if (beanClassName == null) return null;
        try {
            return Class.forName(beanClassName);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static void checkMethods(Class<?> beanClass, List<Advisor> advisors, String beanName) {
        for (Advisor advisor : advisors) {
            if (advisor instanceof AspectJPrecedenceInformation) {
                String aspectName = ((AspectJPrecedenceInformation) advisor).getAspectName();
                if (aspectName.equals(beanName)) {
                    continue;
                }
                String aspectMethodName = getAspectMethodName(advisor);
                if (aspectMethodName == null) {
                    continue;
                }
                if (advisor instanceof PointcutAdvisor) {
                    List<Method> applyMethods = getApplyMethods(((PointcutAdvisor) advisor).getPointcut(), beanClass);
                    for (Method applyMethod : applyMethods) {
                        printAopMethodData(aspectName, aspectMethodName, beanName, applyMethod);
                    }
                }
            }
        }
    }

    private static String getAspectMethodName(Advisor advisor) {
        try {
            Field methodNameField = advisor.getClass().getDeclaredField("methodName");
            methodNameField.setAccessible(true);
            return (String) methodNameField.get(advisor);
        } catch (Exception e) {
            return null;
        }
    }

    private static void printAopMethodData(String aspectName, String aspectMethodName, String beanName, Method method) {
        String methodName = method.getName();
        String parametersString = Arrays.stream(method.getParameterTypes())
                .map(Class::getName)
                .collect(Collectors.joining(","));
        String formatted = String.format(
                AOP_INFO_TEMPLATE, aspectName, aspectMethodName, beanName, methodName, parametersString
        );
        System.out.println(formatted);
    }
}