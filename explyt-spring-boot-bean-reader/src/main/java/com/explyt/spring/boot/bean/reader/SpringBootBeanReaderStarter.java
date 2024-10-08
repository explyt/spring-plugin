package com.explyt.spring.boot.bean.reader;


import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.MethodMetadata;

import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Supplier;

public class SpringBootBeanReaderStarter {

    public static final String SPRING_EXPLYT_ERROR_MESSAGE = "I am Spring Explyt";
    public static final String EXPLYT_BEAN_INFO_START = "ExplytBeanInfoStart";
    public static final String EXPLYT_BEAN_INFO_END = "ExplytBeanInfoEnd";
    public static final String EXPLYT_BEAN_INFO = "ExplytBeanInfo:";

    public static void main(String[] args) {
        Class<?> applicationClass = getApplicationClass();
        ExplytApplicationStartup applicationStartup = new ExplytApplicationStartup();
        SpringApplication springApplication = new SpringApplication(applicationClass) {
            @Override
            protected ConfigurableApplicationContext createApplicationContext() {
                ConfigurableApplicationContext context = super.createApplicationContext();
                applicationStartup.context = context;
                return context;
            }
        };
        springApplication.setApplicationStartup(applicationStartup);
        springApplication.run(args);
    }

    private static Class<?> getApplicationClass() {
        String className = System.getenv("explyt.spring.appClassName");
        try {
            return Class.forName(className);
        } catch (Exception e) {
            throw new RuntimeException("Class not found for: " + className);
        }
    }

    private static class ExplytApplicationStartup implements ApplicationStartup {
        public ConfigurableApplicationContext context;

        @Override
        public ExplytDefaultStartupStep start(String name) {
            return new ExplytDefaultStartupStep(name);
        }

        class ExplytDefaultStartupStep implements StartupStep {

            private final ExplytDefaultStartupStep.DefaultTags TAGS = new DefaultTags();
            private String stepName;

            public ExplytDefaultStartupStep(String name) {
                this.stepName = name;
            }

            @Override
            public String getName() {
                return "default";
            }

            @Override
            public long getId() {
                return 0L;
            }

            @Override
            public Long getParentId() {
                return null;
            }

            @Override
            public Tags getTags() {
                return this.TAGS;
            }

            @Override
            public StartupStep tag(String key, String value) {
                return this;
            }

            @Override
            public StartupStep tag(String key, Supplier<String> value) {
                return this;
            }

            @Override
            public void end() {
                if ("spring.context.beans.post-process".equalsIgnoreCase(stepName)) {
                    printBeans(context);
                    throw new RuntimeException(SPRING_EXPLYT_ERROR_MESSAGE);
                }
            }

            private static void printBeans(ConfigurableApplicationContext context) {
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

            static class DefaultTags implements StartupStep.Tags {

                @Override
                public Iterator<Tag> iterator() {
                    return Collections.emptyIterator();
                }
            }
        }
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
