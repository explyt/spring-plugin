/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core

object SpringCoreClasses {
    const val SPRING_BOOT_APPLICATION = "org.springframework.boot.autoconfigure.SpringBootApplication"
    const val SPRING_APPLICATION = "org.springframework.boot.SpringApplication"
    const val BOOT_AUTO_CONFIGURATION = "org.springframework.boot.autoconfigure.AutoConfiguration"

    const val PROFILE = "org.springframework.context.annotation.Profile"
    const val EVENT_LISTENER = "org.springframework.context.event.EventListener"
    const val APPLICATION_LISTENER = "org.springframework.context.ApplicationListener"
    const val EVENT_PUBLISHER = "org.springframework.context.ApplicationEventPublisher"
    const val APPLICATION_CONTEXT = "org.springframework.context.ApplicationContext"

    const val COMPONENT_SCAN = "org.springframework.context.annotation.ComponentScan"
    const val COMPONENT_SCANS = "org.springframework.context.annotation.ComponentScans"
    const val BEAN = "org.springframework.context.annotation.Bean"
    const val CONFIGURATION = "org.springframework.context.annotation.Configuration"
    const val DEPENDS_ON = "org.springframework.context.annotation.DependsOn"
    const val PRIMARY = "org.springframework.context.annotation.Primary"
    const val SCOPE = "org.springframework.context.annotation.Scope"
    const val IMPORT = "org.springframework.context.annotation.Import"
    const val BEAN_FACTORY = "org.springframework.beans.factory.BeanFactory"
    const val CONFIGURATION_PROPERTIES_SCAN = "org.springframework.boot.context.properties.ConfigurationPropertiesScan"
    const val ENABLE_CONFIGURATION_PROPERTIES =
        "org.springframework.boot.context.properties.EnableConfigurationProperties"

    const val VALUE = "org.springframework.beans.factory.annotation.Value"
    const val AUTOWIRED = "org.springframework.beans.factory.annotation.Autowired"
    const val QUALIFIER = "org.springframework.beans.factory.annotation.Qualifier"
    const val LOOKUP = "org.springframework.beans.factory.annotation.Lookup"
    const val ANNOTATION_CONFIG_CONTEXT = "org.springframework.context.annotation.AnnotationConfigApplicationContext"

    const val COMPONENT = "org.springframework.stereotype.Component"
    const val SERVICE = "org.springframework.stereotype.Service"
    const val CONTROLLER = "org.springframework.stereotype.Controller"
    const val REST_CONTROLLER = "org.springframework.web.bind.annotation.RestController"
    const val REPOSITORY = "org.springframework.stereotype.Repository"

    const val BOOTSTRAP_WITH = "org.springframework.test.context.BootstrapWith"
    const val DYNAMIC_PROPERTY_REGISTRY = "org.springframework.test.context.DynamicPropertyRegistry"

    const val IO_RESOURCE = "org.springframework.core.io.Resource"
    const val ALIAS_FOR = "org.springframework.core.annotation.AliasFor"

    const val MIME_TYPE = "org.springframework.util.MimeType"
    const val RETENTION = "java.lang.annotation.Retention"
    const val RETENTION_POLICY = "java.lang.annotation.RetentionPolicy"
    const val TARGET = "java.lang.annotation.Target"

    const val CACHEABLE = "org.springframework.cache.annotation.Cacheable"
    const val CACHING = "org.springframework.cache.annotation.Caching"
    const val CACHEEVICT = "org.springframework.cache.annotation.CacheEvict"
    const val CACHEPUT = "org.springframework.cache.annotation.CachePut"
    const val CACHECONFIG= "org.springframework.cache.annotation.CacheConfig"

    const val CACHE_RESOLVER = "org.springframework.cache.interceptor.CacheResolver"
    const val CACHE_KEY_GENERATOR = "org.springframework.cache.interceptor.KeyGenerator"
    const val CACHE_MANAGER = "org.springframework.cache.CacheManager"

    const val CONDITIONAL = "org.springframework.context.annotation.Conditional"
    const val CONDITIONAL_ON_BEAN = "org.springframework.boot.autoconfigure.condition.ConditionalOnBean"
    const val CONDITIONAL_ON_SINGLE_BEAN =
        "org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate"
    const val CONDITIONAL_ON_MISSING_BEAN = "org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean"
    const val CONDITIONAL_ON_CLASS = "org.springframework.boot.autoconfigure.condition.ConditionalOnClass"
    const val CONDITIONAL_ON_MISSING_CLASS = "org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass"
    const val CONDITIONAL_ON_PROPERTY = "org.springframework.boot.autoconfigure.condition.ConditionalOnProperty"
    const val CONDITIONAL_ON_WEB_APPLICATION =
        "org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication"

    const val CONFIGURATION_PROPERTIES = "org.springframework.boot.context.properties.ConfigurationProperties"
    const val NESTED_CONFIGURATION_PROPERTIES = "org.springframework.boot.context.properties.NestedConfigurationProperty"
    const val DEPRECATED_CONFIGURATION_PROPERTIES = "org.springframework.boot.context.properties.DeprecatedConfigurationProperty"
    const val CONSTRUCTOR_BINDING = "org.springframework.boot.context.properties.bind.ConstructorBinding"

    const val PROPERTY_SOURCES = "org.springframework.context.annotation.PropertySources"
    const val PROPERTY_SOURCE = "org.springframework.context.annotation.PropertySource"

    const val SCHEDULED = "org.springframework.scheduling.annotation.Scheduled"
    const val ASYNC = "org.springframework.scheduling.annotation.Async"
    const val FUTURE = "java.util.concurrent.Future"

    const val SPRING_BOOT_TEST = "org.springframework.boot.test.context.SpringBootTest"
    const val TEST_PROPERTY_SOURCE = "org.springframework.test.context.TestPropertySource"
    const val TEST_PROPERTY_VALUES = "org.springframework.boot.test.util.TestPropertyValues"

    const val CONTEXT_CONFIGURATION = "org.springframework.test.context.ContextConfiguration"
    const val IMPORT_RESOURCE = "org.springframework.context.annotation.ImportResource"
    const val CONTEXT_SQL = "org.springframework.test.context.jdbc.Sql"

    const val TRANSACTIONAL = "org.springframework.transaction.annotation.Transactional"

    const val RESOURCE_CLASS = "java.lang.Class"
    const val RESOURCE_CLASS_LOADER = "java.lang.ClassLoader"

    const val FILE_RESOURCE_RESOURCE = "org.springframework.core.io.AbstractFileResolvingResource"
    const val FILE_URL_RESOURCE = "org.springframework.core.io.FileUrlResource"
    const val URL_RESOURCE = "org.springframework.core.io.UrlResource"
    const val CLASS_PATH_RESOURCE = "org.springframework.core.io.ClassPathResource"

    const val RESOURCE_LOADER = "org.springframework.core.io.ResourceLoader"
    const val RESOURCE_LOADER_RESOLVER = "org.springframework.core.io.support.ResourcePatternResolver"
    const val RESOURCE_UTILS = "org.springframework.util.ResourceUtils"

    const val PROPERTY_RESOLVER = "org.springframework.core.env.PropertyResolver"
    const val ENVIRONMENT = "org.springframework.core.env.Environment"
    const val CONVERSION_SERVICE = "org.springframework.core.convert.ConversionService"
    const val APPLICATION_ARGUMENTS = "org.springframework.boot.ApplicationArguments"

    const val PARAMETERIZED_TYPE_REFERENCE = "org.springframework.core.ParameterizedTypeReference"
    const val ENTITY_SCAN = "org.springframework.boot.autoconfigure.domain.EntityScan"
    const val ENTITY = "jakarta.persistence.Entity"
    const val ENTITY_X = "javax.persistence.Entity"

    const val POST_CONSTRUCT_J = "jakarta.annotation.PostConstruct"
    const val POST_CONSTRUCT_X = "javax.annotation.PostConstruct"
    const val PRE_DESTROY_J = "jakarta.annotation.PreDestroy"
    const val PRE_DESTROY_X = "javax.annotation.PreDestroy"

    const val NETFLIX_FEIGN_CLIENT = "org.springframework.cloud.netflix.feign.FeignClient"
    const val OPEN_FEIGN_CLIENT = "org.springframework.cloud.openfeign.FeignClient"
    const val LOAD_BALANCED = "org.springframework.cloud.client.loadbalancer.LoadBalanced"

    const val MESSAGE_MAPPING = "org.springframework.messaging.handler.annotation.MessageMapping"
    const val MESSAGE_MAPPING_EE = "org.eclipse.microprofile.reactive.messaging.Incoming"

    const val MOCK_BEAN = "org.springframework.boot.test.mock.mockito.MockBean"
    const val SPY_BEAN = "org.springframework.boot.test.mock.mockito.SpyBean"

    const val HIBERNATE_CFG_AVAILABLE_SETTING = "org.hibernate.cfg.AvailableSettings"

    const val ASPECT = "org.aspectj.lang.annotation.Aspect"

    val ANNOTATIONS_WITH_FILE_REFERENCES_TO_PROPERTIES = setOf(PROPERTY_SOURCE, TEST_PROPERTY_SOURCE)
    val ANNOTATIONS_WITH_FILE_REFERENCES_TO_XML = setOf(CONTEXT_CONFIGURATION, IMPORT_RESOURCE)
    val ANNOTATIONS_WITH_FILE_REFERENCES_TO_SQL = setOf(CONTEXT_SQL)
    val ANNOTATIONS_CACHE = setOf(CACHEABLE, CACHEPUT, CACHEEVICT, CACHECONFIG, CACHING)

    val QUALIFIERS = listOf(QUALIFIER) + JavaEeClasses.QUALIFIER.allFqns
    val AOP_ANNOTATION = listOf(TRANSACTIONAL, ASYNC) + ANNOTATIONS_CACHE
}