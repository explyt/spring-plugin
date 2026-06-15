/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

import org.springframework.beans.factory.annotation.Value

class UserHandler {
    @Value("\${server.timing_new.minutes-to-next-claim}")
    private val minutesToNextClaim: Long = 470L
}