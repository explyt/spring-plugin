/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package src

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "ingest")
@Configuration
open class S3LogsProperties {

    var s3Logs: S3Logs = S3Logs()

    var addresses: List<String> = emptyList()

    class S3Logs {
        var sources: List<S3Source> = emptyList()
    }

    class S3Source {
        var name: String = ""

        var enabled: Boolean = true
    }
}
