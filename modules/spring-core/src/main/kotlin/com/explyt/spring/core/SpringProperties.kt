/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core

object SpringProperties {
    const val CONFIGURATION_METADATA_FILE_NAME = "spring-configuration-metadata.json"
    const val ADDITIONAL_CONFIGURATION_METADATA_FILE_NAME = "additional-spring-configuration-metadata.json"

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

    const val GET_RESOURCE = "getResource"
    const val GET_RESOURCES = "getResources"

    const val SPRING_FACTORIES_FILE_NAME = "spring.factories"
    const val FACTORIES_ENABLE_AUTO_CONFIGURATION = "org.springframework.boot.autoconfigure.EnableAutoConfiguration"
    const val AUTOCONFIGURATION_IMPORTS = "org.springframework.boot.autoconfigure.AutoConfiguration.imports"
    const val BASE_SPRING_PACKAGE = "org.springframework."

    const val META_INF = "META-INF"
    const val SPRING = "spring"
    const val VALUE = "value"
    const val PROPERTIES = "properties"
    const val HINTS = "hints"
    const val NAME = "name"
    const val PROVIDERS = "providers"
    const val VALUES = "values"
    const val PARAMETERS = "parameters"
    const val TARGET = "target"
    const val DESCRIPTION = "description"
    const val DEPRECATED = "deprecated"
    const val REASON = "reason"
    const val TYPE = "type"
    const val SOURCE_TYPE = "sourceType"
    const val REPLACEMENT = "replacement"
    const val DEPRECATION = "deprecation"

    const val SPRING_XML_TEMPLATE = "spring-beans.xml"

    const val PROPERTY_VALUE_DELIMITERS = ",\\ "

    const val POSTFIX_KEYS = ".keys"
    const val POSTFIX_VALUES = ".values"
    const val LOGGING_LEVEL = "logging.level"
    const val MAP_KEY = "mapKey"

    const val SPRING_JPA_PROPERTIES = "spring.jpa.properties"

    const val COLON = ":"
    const val PROPERTY_COMMENT = "#"
    const val PLACEHOLDER_PREFIX = "\${"
    const val PLACEHOLDER_SUFFIX = "}"

}
