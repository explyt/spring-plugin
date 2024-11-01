package com.app;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

@Component
@ComponentScan(basePackageClasses = com.outer.OuterScan.class)
public class InnerScan {
}
