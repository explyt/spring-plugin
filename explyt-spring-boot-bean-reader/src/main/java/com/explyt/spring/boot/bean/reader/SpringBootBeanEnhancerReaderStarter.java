package com.explyt.spring.boot.bean.reader;


import org.springframework.boot.SpringApplication;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.reflect.Method;

public class SpringBootBeanEnhancerReaderStarter {

    private static final String SPRING_EXPLYT_ERROR_MESSAGE = "I am Spring Explyt";

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
