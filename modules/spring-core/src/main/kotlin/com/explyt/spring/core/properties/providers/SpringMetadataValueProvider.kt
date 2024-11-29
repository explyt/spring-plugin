/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
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