package com.app;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.stereotype.Component;

@Component
@ComponentScans({@ComponentScan("com.inner.scan1"), @ComponentScan("com.inner.scan2")})
public class InnerScan {
}
