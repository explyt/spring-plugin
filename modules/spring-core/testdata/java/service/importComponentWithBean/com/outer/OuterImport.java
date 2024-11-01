package com.outer;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

public class OuterImport {
    @Bean public OuterBean outerBean() { return new OuterBean();}
}
