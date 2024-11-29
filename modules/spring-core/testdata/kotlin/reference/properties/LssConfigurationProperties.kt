package com;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lss")
data class LssConfigurationProperties(
    var lssPlanConfiguration: LssPlanConfiguration = LssPlanConfiguration()
}

data class LssPlanConfiguration(
    var isExact: Boolean = false
)




