package com.outer;

import org.springframework.stereotype.Component;
import org.springframework.context.annotation.ComponentScan;

@Component
@ComponentScan("com.outer")
public class OuterComponent {}
