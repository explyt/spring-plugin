package com.boot

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "main")
@Configuration
open class MainFooProperties {
    /**
     * max Sessions Per Connection
     */
    var maxSessionsPerConnection: Int = 765

    /**
     * Event listener
     */
    var eventListener: String? = null
}