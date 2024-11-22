package com.outer;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

public class OuterImport {
    @Bean
    fun outerBean(): OuterBean {
        return OuterBean()
    }
}
