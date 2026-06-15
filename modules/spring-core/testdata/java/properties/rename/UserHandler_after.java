/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

import org.springframework.beans.factory.annotation.Value;

public class UserHandler {
    @Value("${server.timing_new.minutes-to-next-claim:3}")
    private Integer fooFromProperties;
}