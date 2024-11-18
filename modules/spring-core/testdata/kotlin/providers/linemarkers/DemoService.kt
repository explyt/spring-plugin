package com.example.demo

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Component
class DemoService(demo1Service: Demo1Service) {
    @Autowired
    lateinit var demo2Service: Demo2Service
}

@Component
class Demo1Service


class Demo2Service

@Configuration
class DemoConfig {

    @Bean
    fun demo2Service(): Demo2Service {
        return Demo2Service()
    }
}
