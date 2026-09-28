/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.demo

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class ClockConsumer(val clock: Clock = Clock.systemUTC())

@Service
class PlainConsumer(val clock: Clock)

@Service
class NullableConsumer(val clock: Clock?)

@Service
class FieldConsumer {
    @Autowired
    lateinit var clock: Clock
}

@Service
class TwoOnOneLine(val clock: Clock = Clock.systemUTC(), val other: Clock)

@Service
class KotlinCollectionConsumer(val kotlinClocks: List<Clock>)

@Service
class KotlinProviderConsumer(val kotlinProvider: org.springframework.beans.factory.ObjectProvider<Clock>)

@Service
class NullableFieldConsumer {
    @Autowired
    var nullableField: Clock? = null
}
