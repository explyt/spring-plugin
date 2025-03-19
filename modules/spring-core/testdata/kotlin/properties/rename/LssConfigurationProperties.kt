package com

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "lss")
data class LssConfigurationProperties(
    var modeForStudio: String = "",
    var tokenForStudio: String = "",
)