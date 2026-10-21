/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.demo

import java.time.Clock
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service

@SpringBootApplication
class KotlinApp

@Service
class ClockConsumer(val clock: Clock = Clock.systemUTC())

@Configuration
class OneClockConfig {

    @Bean
    fun utcClock(): Clock = Clock.systemUTC()
}
