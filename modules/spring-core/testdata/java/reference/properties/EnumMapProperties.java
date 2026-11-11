/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package src;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "explyt")
@Configuration
public class EnumMapProperties {
    private Map<String, Mode> modes;
    private Map<String, String> labels;
    private Map<String, Entry> entries;
    private Map<String, Map<String, Mode>> groups;
    private Map<String, Object> objects;
    private Map<Mode, String> enumKeys;

    public Map<String, Mode> getModes() {
        return modes;
    }

    public void setModes(Map<String, Mode> modes) {
        this.modes = modes;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public Map<String, Entry> getEntries() {
        return entries;
    }

    public void setEntries(Map<String, Entry> entries) {
        this.entries = entries;
    }

    public Map<String, Map<String, Mode>> getGroups() {
        return groups;
    }

    public void setGroups(Map<String, Map<String, Mode>> groups) {
        this.groups = groups;
    }

    public Map<String, Object> getObjects() {
        return objects;
    }

    public void setObjects(Map<String, Object> objects) {
        this.objects = objects;
    }

    public Map<Mode, String> getEnumKeys() {
        return enumKeys;
    }

    public void setEnumKeys(Map<Mode, String> enumKeys) {
        this.enumKeys = enumKeys;
    }

    public enum Mode {
        ENABLED("enabled"), DISABLED("disabled");

        private final String label;

        Mode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public static class Entry {
        private String name;
        private String namespace;
        private boolean active;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }
}
