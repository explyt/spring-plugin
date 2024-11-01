package com.app;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

@ComponentScan("com.outer")
@Component
public class AppBean {
}
