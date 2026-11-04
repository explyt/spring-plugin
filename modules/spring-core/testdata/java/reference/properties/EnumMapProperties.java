/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package src;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties("app")
public class EnumMapProperties {
    private Map<WeekEnum, String> values;

    public Map<WeekEnum, String> getValues() {
        return values;
    }

    public void setValues(Map<WeekEnum, String> values) {
        this.values = values;
    }
}
