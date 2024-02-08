package config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "main")
open class TestConfig {
    var isEnabled: Boolean = false
    var enabledPrimitive: Boolean = false

    var maxIntegerValue: Int? = null

    var maxDoubleValue: Double? = null

    var maxNumberValue: Number? = null

    @get:DeprecatedConfigurationProperty(reason = "Deprecated because that's all.", replacement = "not.main")
    @get:Deprecated("")
    @set:Deprecated("")
    var maxConnections: Int? = null

    var arrayInteger: Array<Int>? = null

    var contexts: Map<Int, Int>? = null

    var listNotInteger: List<Int> = ArrayList()

    var addresses: List<Int> = ArrayList()
}