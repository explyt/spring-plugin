package com.outerimport;


import com.outer.OuterComponent;
import com.outer2.Outer2;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan("com.outer3")
class OuterImport {

    @Bean
    fun outer2(): Outer2 {
        return Outer2()
    }

    @Bean
    fun outerImportBean(): OuterImportBean {
        return OuterImportBean()
    }
}
