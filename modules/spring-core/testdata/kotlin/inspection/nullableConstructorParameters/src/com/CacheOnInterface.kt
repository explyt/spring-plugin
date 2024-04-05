package com

import org.springframework.stereotype.Component
import org.springframework.boot.context.properties.ConfigurationProperties;

@Component
@ConfigurationProperties(prefix = "some.prefix")
data class SomeProperties(
    var first: String?,
    var mustBeNullable: String
)

@Component
//@ConfigurationProperties(prefix = "some.prefix")
data class SomeProperties(
    var first: String?,
    var notAProblem: String
)
