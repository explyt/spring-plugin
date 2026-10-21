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
    const val JSON_TYPE_INFO = "com.fasterxml.jackson.annotation.JsonTypeInfo"
    const val JSON_SUB_TYPES = "com.fasterxml.jackson.annotation.JsonSubTypes"
    const val JSON_TYPE_NAME = "com.fasterxml.jackson.annotation.JsonTypeName"

    /** `@JsonNaming` lives in databind, whose package differs between Jackson 2 and Jackson 3. */
    val JSON_NAMING_ANNOTATIONS = setOf(
        "com.fasterxml.jackson.databind.annotation.JsonNaming",
        "tools.jackson.databind.annotation.JsonNaming",
    )

    /**
     * The mapper Spring Boot auto-configures under `@ConditionalOnMissingBean`: a project bean of this type, or of a
     * subtype such as `JsonMapper`, replaces Boot's and the `spring.jackson.*` properties no longer reach it.
     */
    val OBJECT_MAPPERS = setOf(
        "com.fasterxml.jackson.databind.ObjectMapper",
        "tools.jackson.databind.ObjectMapper",
    )

    /** The builder Boot's mapper is built from, also auto-configured under `@ConditionalOnMissingBean`. */
    val MAPPER_BUILDERS = setOf(
        "org.springframework.http.converter.json.Jackson2ObjectMapperBuilder",
        "tools.jackson.databind.json.JsonMapper.Builder",
    )

    /**
     * The callbacks Boot applies to its builder. Boot's own, which applies the properties, runs at order 0; a
     * project customizer without an order runs after it and can override what the properties set.
     */
    val MAPPER_BUILDER_CUSTOMIZERS = setOf(
        "org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer",
        "org.springframework.boot.jackson2.autoconfigure.Jackson2ObjectMapperBuilderCustomizer",
        "org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer",
    )
}
