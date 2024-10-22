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

import java.util.Optional;

public class BeanPrinter {

    public static final String EXPLYT_BEAN_INFO_START = "ExplytBeanInfoStart";
    public static final String EXPLYT_BEAN_INFO_END = "ExplytBeanInfoEnd";
    public static final String EXPLYT_BEAN_INFO = "ExplytBeanInfo:";

    public static void printBeans(ConfigurableApplicationContext context) {
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        String[] definitionNames = beanFactory.getBeanDefinitionNames();

        System.out.println(EXPLYT_BEAN_INFO_START);
        for (String beanName : definitionNames) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            BeanInfo beanInfo = getBeanInfo(beanDefinition, beanName);
            if (beanInfo.className == null) continue;
            System.out.println(beanInfo);
        }
        System.out.println(EXPLYT_BEAN_INFO_END);
    }

    private static BeanInfo getBeanInfo(BeanDefinition definition, String definitionName) {
        BeanInfo beanInfo = new BeanInfo();
        beanInfo.primary = definition.isPrimary();
        beanInfo.className = definition.getBeanClassName();
        beanInfo.beanName = definitionName;
        beanInfo.scope = Optional.ofNullable(definition.getScope()).filter(it -> !it.isEmpty()).orElse("singleton");

        String beanClassName = definition.getBeanClassName();
        if (beanClassName != null && definition instanceof AnnotatedBeanDefinition) {
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
                    beanInfo.type = "DATA";
                    return true;
                }
            } catch (Exception ignore) {
            }
        }
        return false;
    }

    static class BeanInfo {
        public String beanName;
        public String className;
        public String methodName;
        public String methodType;
        public String scope;
        public String type;
        public boolean primary;
        public boolean rootBean;

        @Override
        public String toString() {
            return EXPLYT_BEAN_INFO +
                    "{\"className\": \"" + className + "\"," +
                    "\"beanName\": \"" + beanName + "\"," +
                    "\"methodName\": " + getToStringValue(methodName) + "," +
                    "\"methodType\": " + getToStringValue(methodType) + "," +
                    "\"type\": " + getToStringValue(type) + "," +
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
