/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package src

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class ConstructorBoundProperties(
    val publishers: Map<String, PublisherProperties> = emptyMap(),
    val routes: List<RouteProperties> = emptyList(),
) {
    data class PublisherProperties(
        val ownerApplication: String = "",
    )

    data class RouteProperties(
        val payloadType: String = "",
    )
}
