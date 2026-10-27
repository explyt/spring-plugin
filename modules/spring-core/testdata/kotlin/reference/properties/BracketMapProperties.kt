/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package src

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "app")
@Configuration
class BracketMapProperties {
    var publishers: Map<String, PublisherProperties> = emptyMap()

    class PublisherProperties {
        var ownerApplication: String = ""
        var routes: List<RouteProperties> = emptyList()
    }

    class RouteProperties {
        var payloadType: String = ""
    }
}
