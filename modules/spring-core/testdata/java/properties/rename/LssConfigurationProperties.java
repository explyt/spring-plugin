package com;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lss")
public class LssConfigurationProperties {
    private LssPlanConfiguration lssPlanConfiguration = new LssPlanConfiguration();

    public LssPlanConfiguration getLssPlanConfiguration() {
        return lssPlanConfiguration;
    }

    public void setLssPlanConfiguration(LssPlanConfiguration lssPlanConfiguration) {
        this.lssPlanConfiguration = lssPlanConfiguration;
    }

    static class LssPlanConfiguration {
        private Boolean isExact = false;

        public Boolean getExact() {
            return isExact;
        }

        public void setExact(Boolean exact) {
            isExact = exact;
        }
    }
}