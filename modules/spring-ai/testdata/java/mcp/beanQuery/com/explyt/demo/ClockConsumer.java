/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.demo;

import java.time.Clock;
import org.springframework.stereotype.Service;

@Service
public class ClockConsumer {

    private final Clock clock;

    public ClockConsumer(Clock clock) {
        this.clock = clock;
    }

    public Clock clock() {
        return clock;
    }
}
