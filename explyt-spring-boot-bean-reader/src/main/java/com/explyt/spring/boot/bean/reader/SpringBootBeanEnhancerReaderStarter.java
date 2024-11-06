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

import org.springframework.boot.SpringApplication;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.reflect.Method;

public class SpringBootBeanEnhancerReaderStarter {

    private static final String SPRING_EXPLYT_ERROR_MESSAGE = "I am Explyt Spring";

    public static void main(String[] args) {
        Class<?> applicationClass = getApplicationClass();

        SpringApplication springApplication = new SpringApplication(applicationClass) {
            @Override
            protected ConfigurableApplicationContext createApplicationContext() {
                ConfigurableApplicationContext applicationContext = super.createApplicationContext();
                Enhancer enhancer = new Enhancer();
                enhancer.setSuperclass(applicationContext.getClass());
                enhancer.setCallback(new MethodInterceptorCglib());
                return (ConfigurableApplicationContext) enhancer.create();
            }
        };
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

    static class MethodInterceptorCglib implements MethodInterceptor {

        @Override
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
            if (method.getName().equals("onRefresh")) {
                ConfigurableApplicationContext context = (ConfigurableApplicationContext) obj;
                BeanPrinter.printBeans(context);
                throw new RuntimeException(SPRING_EXPLYT_ERROR_MESSAGE);
            } else {
                return proxy.invokeSuper(obj, args);
            }
        }
    }
}
