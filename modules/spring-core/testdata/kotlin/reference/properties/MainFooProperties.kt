package src

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import org.springframework.util.MimeType
import src.WeekEnum
import java.nio.charset.Charset
import java.util.Locale
import java.util.Map

@ConfigurationProperties(prefix = "main.local")
@Configuration
open class MainFooProperties {

    var maxSessionsPerConnection: Int = 765

    var eventListener: String? = null

    var codeMimeType: MimeType? = null

    var enumValue: WeekEnum? = null

    var codeLocale: java.util.Locale? = null

    var codeCharset: java.nio.charset.Charset? = null

    var codeResource: Resource? = null

    var contexts: Map<String, Int>? = null

}