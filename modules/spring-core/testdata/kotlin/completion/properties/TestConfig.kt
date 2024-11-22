import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "mail")
open class TestConfig {
    var hostName: String = ""
    var port: Int = 0
    var from: String = ""
    var ert: Long = 0
    var newProperty: String = ""
    var externalSettings: ExternalSettings? = null
    var nestedSettings: NestedSettings = NestedSettings()

    fun setFormAndPort(formAndPort: String) {
        from = formAndPort
        port = 8081
    }

    inner class NestedSettings {
        var f1: String = ""
        var anotherNestedSettings: AnotherNestedSettings = AnotherNestedSettings("", "", "")
    }

    data class AnotherNestedSettings(
        val camelCaseLongPropertyVeryLongProperty: String,
        val property2: String,
        val property3: String
    )
}