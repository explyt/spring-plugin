package com.app;

import com.outer.AppTestConfigurationRegister;
import com.outer.OuterComponent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class MainClass {
    public static void main(String[] args) {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(AppTestConfigurationRegister.class)
            var bean = context.getBean(OuterComponent.class);
        }
    }
}
