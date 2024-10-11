package com

import org.springframework.boot.context.properties.bind.ConstructorBinding
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
data class SomeProperties2(
    var first: String?,
    var notAProblem: String
)

@Component
@ConfigurationProperties(prefix = "some.prefix")
data class SomeProperties3(
    var first: String?,
    var notAProblemA: String = "A",
    val notAProblemB: String = "B"
)

@Component
@ConfigurationProperties(prefix = "some.prefix")
data class SomeProperties4 @ConstructorBinding constructor(
    var first: String,
    var second: Long
)
