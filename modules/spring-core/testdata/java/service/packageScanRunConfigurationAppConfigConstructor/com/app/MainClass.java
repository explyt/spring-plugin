package com.app;

import com.outer.AppTestConfiguration;
import com.outer.OuterComponent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class MainClass {
    public static void main(String[] args) {
        try (var context = new AnnotationConfigApplicationContext(AppTestConfiguration.class)) {
            var bean = context.getBean(OuterComponent.class);
        }
    }
}
