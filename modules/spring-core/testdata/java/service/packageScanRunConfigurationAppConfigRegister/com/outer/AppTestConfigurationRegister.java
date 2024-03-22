package com.outer;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(OuterComponent.class)
public class AppTestConfigurationRegister {

}
