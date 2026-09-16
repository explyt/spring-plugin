/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package src;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "app")
public record ConstructorBoundRecordProperties(
        Map<String, PublisherProperties> publishers,
        List<RouteProperties> routes
) {
    public record PublisherProperties(String ownerApplication, List<RouteProperties> routes) {
    }

    public record RouteProperties(String payloadType) {
    }
}
