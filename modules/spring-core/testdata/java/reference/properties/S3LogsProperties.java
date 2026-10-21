/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package src;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@ConfigurationProperties(prefix = "ingest")
@Configuration
public class S3LogsProperties {

    private S3Logs s3Logs = new S3Logs();

    private List<String> addresses;

    public S3Logs getS3Logs() {
        return s3Logs;
    }

    public void setS3Logs(S3Logs s3Logs) {
        this.s3Logs = s3Logs;
    }

    public List<String> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<String> addresses) {
        this.addresses = addresses;
    }

    public static class S3Logs {
        private List<S3Source> sources;

        public List<S3Source> getSources() {
            return sources;
        }

        public void setSources(List<S3Source> sources) {
            this.sources = sources;
        }
    }

    public static class S3Source {
        private String name;

        private Boolean enabled = true;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }
}
