package com.example.demo

import org.springframework.context.annotation.PropertySource
import org.springframework.context.annotation.Configuration

@Configuration
@PropertySource("/1/<caret>")
open class TestPropertySourceMultiResolve