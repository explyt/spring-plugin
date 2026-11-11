/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package src

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "explyt")
@Configuration
class EnumMapProperties {
    var modes: Map<String, Mode> = emptyMap()
    var labels: Map<String, String> = emptyMap()
    var entries: Map<String, Entry> = emptyMap()
    var groups: Map<String, Map<String, Mode>> = emptyMap()
    var objects: Map<String, Any> = emptyMap()
    var enumKeys: Map<Mode, String> = emptyMap()

    enum class Mode(val label: String) {
        ENABLED("enabled"), DISABLED("disabled")
    }

    class Entry {
        var name: String = ""
        var namespace: String = ""
        var active: Boolean = false
    }
}
