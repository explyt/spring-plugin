package com.outerimport;


import com.outer.OuterComponent;
import com.outer2.Outer2;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan("com.outer3")
public class OuterImport {

    @Bean public Outer2 outer2() { return new Outer2();}

    @Bean public OuterImportBean outerImportBean() { return new OuterImportBean();}
}
