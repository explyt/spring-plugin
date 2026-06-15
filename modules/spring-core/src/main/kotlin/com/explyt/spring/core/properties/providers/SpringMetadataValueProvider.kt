/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.providers


class SpringMetadataValueProvider private constructor(
    val value: String,
    val description: String,
    val required: List<String>
) {

    companion object {

        private val providers = listOf(
            SpringMetadataValueProvider(
                value = "any",
                description = "Permits any additional value to be provided.",
                required = emptyList()
            ),
            SpringMetadataValueProvider(
                value = "class-reference",
                description = "Auto-completes the classes available in the project.",
                required = listOf("target")
            ),
            SpringMetadataValueProvider(
                value = "handle-as",
                description = "Handles the property as if it were defined by the type defined by the mandatory target parameter.",
                required = listOf("target")
            ),
            SpringMetadataValueProvider(
                value = "logger-name",
                description = "Auto-completes valid logger names and logger groups.",
                required = emptyList()
            ),
            SpringMetadataValueProvider(
                value = "spring-bean-reference",
                description = "Auto-completes the available bean names in the current project.",
                required = listOf("target")
            ),
            SpringMetadataValueProvider(
                value = "spring-profile-name",
                description = "Auto-completes the available Spring profile names in the project.",
                required = emptyList()
            )
        )

        fun findByName(name: String): SpringMetadataValueProvider? {
            return providers.find { it.value == name }
        }

        val entries: List<SpringMetadataValueProvider> get() = providers
    }
}