package com.esprito.spring.core

object SpringProperties {
    const val PREFIX_CLASSPATH = "classpath:"
    const val PREFIX_CLASSPATH_STAR = "classpath*:"
    const val PREFIX_FILE = "file:"
    const val PREFIX_HTTP = "http:"

    const val PUBLISH_EVENT_METHOD = "publishEvent"
    const val ON_APPLICATION_EVENT = "onApplicationEvent"

    const val ANY = "any"
    const val CLASS_REFERENCE = "class-reference"
    const val HANDLE_AS = "handle-as"
    const val SPRING_BEAN_REFERENCE = "spring-bean-reference"

    const val SPRING_PROFILES_ACTIVE = "spring.profiles.active"

    const val SPRING_FACTORIES_FILE_NAME = "spring.factories"
    const val FACTORIES_ENABLE_AUTO_CONFIGURATION = "org.springframework.boot.autoconfigure.EnableAutoConfiguration"
    const val AUTOCONFIGURATION_IMPORTS = "org.springframework.boot.autoconfigure.AutoConfiguration.imports"

    const val META_INF = "META-INF"
    const val SPRING = "spring"

    const val SPRING_XML_TEMPLATE = "spring-beans.xml"

    const val PROPERTY_VALUE_DELIMITERS = ",\\ "

    const val COLON = ":"
    const val PLACEHOLDER_PREFIX = "\${"
    const val PLACEHOLDER_SUFFIX = "}"
}
