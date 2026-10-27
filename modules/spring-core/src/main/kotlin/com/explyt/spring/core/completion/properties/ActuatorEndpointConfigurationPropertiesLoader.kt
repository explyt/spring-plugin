/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.completion.properties

import com.explyt.spring.core.JavaCoreClasses
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.properties.references.ActuatorEndpoint
import com.explyt.spring.core.properties.references.ActuatorEndpointKeys
import com.explyt.spring.core.properties.references.ActuatorEndpointKeys.MANAGEMENT_ENDPOINT
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.util.runReadNonBlocking
import com.intellij.openapi.module.Module

/**
 * The `management.endpoint.<id>.*` keys of the endpoints the project itself declares.
 *
 * Spring ships metadata for its built-in ids only, because a key of this family is formatted from the endpoint id at
 * runtime rather than declared. Feeding the synthesized keys through the loader serves the three paths a key has -
 * resolution, inspection and completion - from one place.
 */
class ActuatorEndpointConfigurationPropertiesLoader : ConfigurationPropertiesLoader {

    override fun loadProperties(module: Module): List<ConfigurationProperty> = runReadNonBlocking {
        ActuatorEndpointKeys.endpointsById(module).values.asSequence()
            .mapNotNull { it.firstOrNull() }
            .flatMap { propertiesOf(it) }
            .toList()
    }

    override fun loadPropertyHints(module: Module): List<PropertyHint> = emptyList()

    override fun loadPropertyMetadataElements(module: Module): List<ElementHint> = emptyList()

    override fun loadMetadataElements(module: Module): List<ElementHint> = emptyList()

    override fun findMetadataValueElement(module: Module, propertyName: String, propertyValue: String): ElementHint? =
        null

    private fun propertiesOf(endpoint: ActuatorEndpoint): Sequence<ConfigurationProperty> = sequence {
        val id = endpoint.id
        endpoint.defaultAccess?.let { defaultAccess ->
            val recommendedAccess = PropertyUtil.recommendedValueSpelling(defaultAccess)
            yield(
                property(
                    "$MANAGEMENT_ENDPOINT.$id.access",
                    SpringCoreClasses.ACTUATOR_ENDPOINT_ACCESS,
                    endpoint,
                    SpringCoreBundle.message("explyt.spring.property.actuator.endpoint.access", id, recommendedAccess),
                    recommendedAccess
                )
            )
        }
        yield(
            property(
                "$MANAGEMENT_ENDPOINT.$id.enabled",
                JavaCoreClasses.BOOLEAN,
                endpoint,
                SpringCoreBundle.message("explyt.spring.property.actuator.endpoint.enabled", id),
                null
            )
        )
        yield(
            property(
                "$MANAGEMENT_ENDPOINT.$id.cache.time-to-live",
                ActuatorEndpointKeys.KEY_TYPES.getValue("cache.time-to-live"),
                endpoint,
                SpringCoreBundle.message("explyt.spring.property.actuator.endpoint.cache.time.to.live", id),
                null
            )
        )
    }

    private fun property(
        name: String,
        type: String,
        endpoint: ActuatorEndpoint,
        description: String,
        defaultValue: Any?
    ) = ConfigurationProperty(
        name = name,
        propertyType = null,
        type = type,
        sourceType = endpoint.psiClass.qualifiedName,
        description = description,
        defaultValue = defaultValue,
        deprecation = null
    )
}
