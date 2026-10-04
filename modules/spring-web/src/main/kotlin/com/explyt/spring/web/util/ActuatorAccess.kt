/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import com.explyt.spring.core.completion.properties.DefinedConfigurationProperty
import com.explyt.spring.core.properties.FoldedPropertyValue
import com.explyt.spring.core.properties.references.ActuatorEndpoint
import com.explyt.spring.web.loader.EndpointAccess
import com.intellij.openapi.module.Module
import java.util.Locale

/**
 * The access Spring Boot grants an Actuator endpoint, decided the way its `PropertiesEndpointAccessResolver` does
 * (identical in Boot 3.4+ and 4.x):
 * - `management.endpoint.<id>.access`, else the legacy `management.endpoint.<id>.enabled`;
 * - else `management.endpoints.access.default`, else the legacy `management.endpoints.enabled-by-default`;
 * - else the endpoint's own `@Endpoint(defaultAccess)`, which `enableByDefault = false` turns into `NONE` in Boot 3.x;
 * - the result capped by `management.endpoints.access.max-permitted`, which defaults to `UNRESTRICTED`.
 *
 * A legacy `enabled` value reads `true` as `UNRESTRICTED` and `false` as `NONE`, as Boot maps it. Setting an access
 * key together with its legacy twin fails the application at startup, so that configuration answers `UNKNOWN`.
 *
 * Before Boot 3.4 there is no `Access` type and no access key: only the legacy keys decide, and nothing caps them.
 */
object ActuatorAccess {

    const val DEFAULT_ACCESS_KEY = "management.endpoints.access.default"
    const val ENABLED_BY_DEFAULT_KEY = "management.endpoints.enabled-by-default"
    const val MAX_PERMITTED_KEY = "management.endpoints.access.max-permitted"

    private const val ENDPOINT_KEY_PREFIX = "management.endpoint."
    private const val ACCESS_SUFFIX = ".access"
    private const val ENABLED_SUFFIX = ".enabled"

    private val TRUE_VALUES = setOf("true", "on", "yes", "1")
    private val FALSE_VALUES = setOf("false", "off", "no", "0")

    /** The rule [module]'s configuration states, read from [definitions] — every key the module defines, grouped by key. */
    fun of(module: Module, definitions: Map<String, List<DefinedConfigurationProperty>>): Rule {
        val valueOf = { key: String -> FoldedPropertyValue.choose(module, definitions[key].orEmpty())?.value }
        return Rule { key ->
            valueOf(key)?.let { MappingPathPlaceholders.resolve(it, valueOf) }?.trim()?.takeIf { it.isNotEmpty() }
        }
    }

    /**
     * The access one operation of an endpoint with [endpointAccess] gets. Under `READ_ONLY` Boot registers read
     * operations alone, so any other operation of that endpoint is not served at all.
     */
    fun ofOperation(endpointAccess: EndpointAccess, isRead: Boolean): EndpointAccess =
        if (endpointAccess == EndpointAccess.READ_ONLY && !isRead) EndpointAccess.NONE else endpointAccess

    class Rule internal constructor(private val read: (String) -> String?) {

        private val endpointsDefault: EndpointAccess? =
            exclusive(accessValue(DEFAULT_ACCESS_KEY), enabledValue(ENABLED_BY_DEFAULT_KEY))
        private val maxPermitted: EndpointAccess = accessValue(MAX_PERMITTED_KEY) ?: EndpointAccess.UNRESTRICTED
        private val legacyEndpointsDefault: EndpointAccess? = enabledValue(ENABLED_BY_DEFAULT_KEY)

        fun accessOf(endpoint: ActuatorEndpoint): EndpointAccess {
            val key = ENDPOINT_KEY_PREFIX + endpoint.id.lowercase(Locale.ROOT)
            if (endpoint.defaultAccess == null) {
                return enabledValue(key + ENABLED_SUFFIX) ?: legacyEndpointsDefault ?: declaredAccessOf(endpoint)
            }
            val own = exclusive(accessValue(key + ACCESS_SUFFIX), enabledValue(key + ENABLED_SUFFIX))
            return cap(own ?: endpointsDefault ?: declaredAccessOf(endpoint), maxPermitted)
        }

        private fun accessValue(key: String): EndpointAccess? = read(key)?.let(::parseAccess)

        private fun enabledValue(key: String): EndpointAccess? = read(key)?.let(::parseEnabled)
    }

    private fun declaredAccessOf(endpoint: ActuatorEndpoint): EndpointAccess {
        if (!endpoint.enabledByDefault) return EndpointAccess.NONE
        return endpoint.defaultAccess?.let(::parseAccess) ?: EndpointAccess.UNRESTRICTED
    }

    /** Boot binds an enum leniently: case and separators are ignored, so `read-only` is `READ_ONLY`. */
    private fun parseAccess(value: String): EndpointAccess {
        if (MappingPathPlaceholders.PLACEHOLDER_START in value) return EndpointAccess.UNKNOWN
        val normalized = value.filter { it.isLetterOrDigit() }.uppercase(Locale.ROOT)
        return ACCESS_BY_NORMALIZED_NAME[normalized] ?: EndpointAccess.UNKNOWN
    }

    private fun parseEnabled(value: String): EndpointAccess {
        if (MappingPathPlaceholders.PLACEHOLDER_START in value) return EndpointAccess.UNKNOWN
        val normalized = value.lowercase(Locale.ROOT)
        return when (normalized) {
            in TRUE_VALUES -> EndpointAccess.UNRESTRICTED
            in FALSE_VALUES -> EndpointAccess.NONE
            else -> EndpointAccess.UNKNOWN
        }
    }

    private fun exclusive(access: EndpointAccess?, legacy: EndpointAccess?): EndpointAccess? =
        if (access != null && legacy != null) EndpointAccess.UNKNOWN else access ?: legacy

    /** `Access.cap`: the lower of the two, where `NONE` is lowest whatever the other side reads. */
    private fun cap(access: EndpointAccess, maxPermitted: EndpointAccess): EndpointAccess = when {
        access == EndpointAccess.NONE || maxPermitted == EndpointAccess.NONE -> EndpointAccess.NONE
        access == EndpointAccess.UNKNOWN || maxPermitted == EndpointAccess.UNKNOWN -> EndpointAccess.UNKNOWN
        access == EndpointAccess.READ_ONLY || maxPermitted == EndpointAccess.READ_ONLY -> EndpointAccess.READ_ONLY
        else -> EndpointAccess.UNRESTRICTED
    }

    private val ACCESS_BY_NORMALIZED_NAME: Map<String, EndpointAccess> =
        listOf(EndpointAccess.UNRESTRICTED, EndpointAccess.READ_ONLY, EndpointAccess.NONE)
            .associateBy { it.name.replace("_", "") }
}
