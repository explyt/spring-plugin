package com.app;

import com.outer.AppTestConfiguration;
import com.outer.OuterComponent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class MainClass

fun main() {
    AnnotationConfigApplicationContext(AppTestConfiguration::class)
        .use { context ->
            val bean = context.getBean(OuterComponent::class.java)
        }
}
