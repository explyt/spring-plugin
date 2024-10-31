package com.explyt.spring.boot.bean.reader;


import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Supplier;

public class SpringBootBeanReaderStarter {

    private static final String SPRING_EXPLYT_ERROR_MESSAGE = "I am Explyt Spring";

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
                    BeanPrinter.printBeans(context);
                    throw new RuntimeException(SPRING_EXPLYT_ERROR_MESSAGE);
                }
            }

            class DefaultTags implements StartupStep.Tags {

                @Override
                public Iterator<Tag> iterator() {
                    return Collections.emptyIterator();
                }
            }
        }
    }
}
