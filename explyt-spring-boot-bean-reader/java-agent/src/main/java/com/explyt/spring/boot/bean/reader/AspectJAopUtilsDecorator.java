/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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
import tech.ytsaurus.spyt.patch.annotations.AddMethod;
import tech.ytsaurus.spyt.patch.annotations.Decorate;
import tech.ytsaurus.spyt.patch.annotations.OriginClass;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

@Decorate
@OriginClass("org.springframework.aop.aspectj.AspectJAopUtils")
public class AspectJAopUtilsDecorator {
    private static final String AOP_INFO_TEMPLATE = Constants.EXPLYT_AOP_INFO +
            "{\"aspectName\":\"%s\", \"aspectMethodName\":\"%s\", \"beanName\":\"%s\", \"methodName\":\"%s\", \"methodParams\":\"%s\"}";

    @AddMethod
    public static void explytPrintAopData(
            ConfigurableListableBeanFactory beanFactory, Map<String, String> beanClassByName, List<String> result
    ) {
        try {
            Class.forName("org.aspectj.lang.annotation.Aspect");
        } catch (ClassNotFoundException e) {
            return;
        }

        List<Advisor> advisors = explytGetAdvisors((AbstractBeanFactory) beanFactory);
        if (advisors.isEmpty()) return;
        String[] names = beanFactory.getBeanDefinitionNames();
        for (String beanName : names) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            Object source = beanDefinition.getSource();
            if (source == null || source.toString().startsWith("org.springframework.")) continue;
            Class<?> beanClass = explytGetBeanClass(beanName, beanClassByName);
            if (beanClass == null) continue;
            explytCheckMethods(beanClass, advisors, beanName, result);
        }
    }

    @AddMethod
    private static List<Advisor> explytGetAdvisors(AbstractBeanFactory beanFactory) {
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

    @AddMethod
    private static Class<?> explytGetBeanClass(String beanName, Map<String, String> beanClassByName) {
        String beanClassName = beanClassByName.get(beanName);
        if (beanClassName == null) return null;
        try {
            return Class.forName(beanClassName);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @AddMethod
    private static void explytCheckMethods(
            Class<?> beanClass, List<Advisor> advisors, String beanName, List<String> result
    ) {
        for (Advisor advisor : advisors) {
            if (advisor instanceof AspectJPrecedenceInformation) {
                String aspectName = ((AspectJPrecedenceInformation) advisor).getAspectName();
                if (aspectName.equals(beanName)) {
                    continue;
                }
                String aspectMethodName = explytGetAspectMethodName(advisor);
                if (aspectMethodName == null) {
                    continue;
                }
                if (advisor instanceof PointcutAdvisor) {
                    List<Method> applyMethods = explytGetApplyMethods(((PointcutAdvisor) advisor).getPointcut(), beanClass);
                    for (Method applyMethod : applyMethods) {
                        explytPrintAopMethodData(aspectName, aspectMethodName, beanName, applyMethod, result);
                    }
                }
            }
        }
    }

    @AddMethod
    private static String explytGetAspectMethodName(Advisor advisor) {
        try {
            Field methodNameField = advisor.getClass().getDeclaredField("methodName");
            methodNameField.setAccessible(true);
            return (String) methodNameField.get(advisor);
        } catch (Exception e) {
            return null;
        }
    }

    @AddMethod
    private static List<Method> explytGetApplyMethods(Pointcut pc, Class<?> targetClass) {
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

    @AddMethod
    private static void explytPrintAopMethodData(
            String aspectName, String aspectMethodName, String beanName, Method method, List<String> result) {
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        StringBuilder parametersString = new StringBuilder();
        if (parameterTypes != null && parameterTypes.length > 0) {
            for (Class<?> parameterType : parameterTypes) {
                parametersString.append(",").append(parameterType.getName());
            }
            parametersString = new StringBuilder(parametersString.substring(1));
        }
        String aspectRawInfo = String.format(
                AOP_INFO_TEMPLATE, aspectName, aspectMethodName, beanName, methodName, parametersString
        );
        if (result != null) {
            result.add(aspectRawInfo);
        } else {
            System.out.println(aspectRawInfo);
        }
    }

}
