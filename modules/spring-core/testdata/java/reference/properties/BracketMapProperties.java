/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package src;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "app")
@Configuration
public class BracketMapProperties {
    private Map<String, PublisherProperties> publishers;

    public Map<String, PublisherProperties> getPublishers() {
        return publishers;
    }

    public void setPublishers(Map<String, PublisherProperties> publishers) {
        this.publishers = publishers;
    }

    public static class PublisherProperties {
        private String ownerApplication;
        private List<RouteProperties> routes;

        public String getOwnerApplication() {
            return ownerApplication;
        }

        public void setOwnerApplication(String ownerApplication) {
            this.ownerApplication = ownerApplication;
        }

        public List<RouteProperties> getRoutes() {
            return routes;
        }

        public void setRoutes(List<RouteProperties> routes) {
            this.routes = routes;
        }
    }

    public static class RouteProperties {
        private String payloadType;

        public String getPayloadType() {
            return payloadType;
        }

        public void setPayloadType(String payloadType) {
            this.payloadType = payloadType;
        }
    }
}
