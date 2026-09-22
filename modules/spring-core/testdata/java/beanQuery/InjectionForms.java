/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.demo;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

@Service
class SingleConsumer {
    private final Clock clock;

    SingleConsumer(Clock clock) {
        this.clock = clock;
    }
}

@Service
class ShapeConsumer {
    @Autowired
    private Optional<Clock> optionalClock;
    @Autowired
    private List<Clock> allClocks;
    @Autowired
    private Clock[] clockArray;
    @Autowired
    private Map<String, Clock> clocksByName;
    @Autowired
    private Map<Integer, Clock> clocksByNumber;
    @Autowired
    private ObjectProvider<Clock> clockProvider;
    @Autowired
    private Optional rawOptional;

    @Autowired(required = false)
    private Clock notRequired;

    @Autowired
    void setClock(Clock injected, Clock second) {
    }
}

@Configuration
class ClockConfiguration {
    @Bean
    Clock clock(Clock delegate) {
        return delegate;
    }
}

@Service
class TwoConstructors {
    TwoConstructors(Clock clock) {
    }

    TwoConstructors(Clock clock, String name) {
    }
}

@Service
class LocalVariableHolder {
    void run() {
        Clock local = null;
    }
}
