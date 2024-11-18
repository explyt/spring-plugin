package com.example.demo

import org.springframework.context.annotation.PropertySource
import org.springframework.context.annotation.Configuration

@Configuration

//positive cases
@PropertySource("/1/application2.properties")
@PropertySource("classpath:./1/application3.properties")
@PropertySource("classpath:/2/abc/application4.properties")
@PropertySource("/2/def/application5.properties")
@PropertySource("file:3/application6.properties")
@PropertySource("file:#base_dir/src/3/application7.properties")
@PropertySource("file:./4/application8.properties")
@PropertySource("4/application9.properties")

//negative cases
@PropertySource("file:/4/application10.properties")
@PropertySource("classpath:/3/application10.properties")
// create reference to package 3
@PropertySource("./application10.properties")
@PropertySource("/*/application11.properties")
open class TestPropertySourceReferences {
}