/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core

/** Jackson annotations that decide how a value is serialised; the package is the same in Jackson 2 and Jackson 3. */
object JacksonClasses {
    const val JSON_VALUE = "com.fasterxml.jackson.annotation.JsonValue"
    const val JSON_PROPERTY = "com.fasterxml.jackson.annotation.JsonProperty"
    const val JSON_IGNORE = "com.fasterxml.jackson.annotation.JsonIgnore"
}
