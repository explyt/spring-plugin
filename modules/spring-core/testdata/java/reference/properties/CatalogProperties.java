/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package src;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "explyt.catalog")
public class CatalogProperties {
    private List<Provider> providers;
    private Map<String, Provider> regions;

    public List<Provider> getProviders() {
        return providers;
    }

    public void setProviders(List<Provider> providers) {
        this.providers = providers;
    }

    public Map<String, Provider> getRegions() {
        return regions;
    }

    public void setRegions(Map<String, Provider> regions) {
        this.regions = regions;
    }

    public static class Provider {
        private String url;
        private Map<String, Model> models;
        @NestedConfigurationProperty
        private Limits limits;

        public Limits getLimits() {
            return limits;
        }

        public void setLimits(Limits limits) {
            this.limits = limits;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Map<String, Model> getModels() {
            return models;
        }

        public void setModels(Map<String, Model> models) {
            this.models = models;
        }
    }

    public static class Model {
        private boolean availableForPersonal;
        private ModelInfo modelInfo;

        public boolean isAvailableForPersonal() {
            return availableForPersonal;
        }

        public void setAvailableForPersonal(boolean availableForPersonal) {
            this.availableForPersonal = availableForPersonal;
        }

        public ModelInfo getModelInfo() {
            return modelInfo;
        }

        public void setModelInfo(ModelInfo modelInfo) {
            this.modelInfo = modelInfo;
        }
    }

    public static class Limits {
        private int maxTokens;

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }
    }

    public static class ModelInfo {
        private String modelName;

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }
    }
}
