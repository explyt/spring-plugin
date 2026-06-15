/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.runconfiguration

enum class ExplytLicenseState(val state: Int) {
    Unknown(-1),
    NotValid(0),
    Valid(1),
    Expired(2),
    Empty(3),
    NotConnect(4)
}