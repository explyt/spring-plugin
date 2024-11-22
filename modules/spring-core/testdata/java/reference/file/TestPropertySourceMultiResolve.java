package com.example.demo;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("/1/<caret>")
public class TestPropertySourceMultiResolve {
}