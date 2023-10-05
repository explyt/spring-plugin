package com.esprito.spring.core

object SpringCoreClasses {
    const val SPRING_BOOT_APPLICATION = "org.springframework.boot.autoconfigure.SpringBootApplication"
    const val SPRING_APPLICATION = "org.springframework.boot.SpringApplication"

    const val PROFILE = "org.springframework.context.annotation.Profile"
    const val ALIAS_FOR = "org.springframework.core.annotation.AliasFor"
    const val ANNOTATION = "java.lang.annotation.Annotation"

    const val EVENT_LISTENER = "org.springframework.context.event.EventListener"
    const val BEAN = "org.springframework.context.annotation.Bean"
    const val CONFIGURATION = "org.springframework.context.annotation.Configuration"
    const val DEPENDS_ON = "org.springframework.context.annotation.DependsOn"
    const val PRIMARY = "org.springframework.context.annotation.Primary"

    const val VALUE = "org.springframework.beans.factory.annotation.Value"
    const val AUTOWIRED = "org.springframework.beans.factory.annotation.Autowired"
    const val QUALIFIER = "org.springframework.beans.factory.annotation.Qualifier"
    const val LOOKUP = "org.springframework.beans.factory.annotation.Lookup"

    const val COMPONENT = "org.springframework.stereotype.Component"

    const val CACHEABLE = "org.springframework.cache.annotation.Cacheable"

    const val CONDITIONAL_ON_BEAN = "org.springframework.boot.autoconfigure.condition.ConditionalOnBean"
    const val CONDITIONAL_ON_MISSING_BEAN = "org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean"

    const val CONFIGURATION_PROPERTIES = "org.springframework.boot.context.properties.ConfigurationProperties"
    const val PROPERTY_SOURCES = "org.springframework.context.annotation.PropertySources"
    const val PROPERTY_SOURCE = "org.springframework.context.annotation.PropertySource"

    const val SPRING_BOOT_TEST = "org.springframework.boot.test.context.SpringBootTest"
    const val TEST_PROPERTY_SOURCE = "org.springframework.test.context.TestPropertySource"
    const val TEST_PROPERTY_VALUES = "org.springframework.boot.test.util.TestPropertyValues"

    const val CONTEXT_CONFIGURATION = "org.springframework.test.context.ContextConfiguration"
    const val IMPORT_RESOURCE = "org.springframework.context.annotation.ImportResource"
    const val CONTEXT_SQL = "org.springframework.test.context.jdbc.SQL"

    val ANNOTATIONS_WITH_FILE_REFERENCES_TO_PROPERTIES = setOf(PROPERTY_SOURCE, TEST_PROPERTY_SOURCE)
    val ANNOTATIONS_WITH_FILE_REFERENCES_TO_XML = setOf(CONTEXT_CONFIGURATION, IMPORT_RESOURCE)
    val ANNOTATIONS_WITH_FILE_REFERENCES_TO_SQL = setOf(CONTEXT_SQL)

    const val COMPONENT_SCAN = "org.springframework.context.annotation.ComponentScan"

    val ANNOTATIONS_WITH_PACKAGE_ANT_REFERENCES = setOf(COMPONENT_SCAN, SPRING_BOOT_APPLICATION)

    const val CLASSPATH_PREFIX = "classpath:"

    const val FILE_PREFIX = "file:"

}