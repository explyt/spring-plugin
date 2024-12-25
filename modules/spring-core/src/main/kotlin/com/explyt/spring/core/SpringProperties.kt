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

    const val COLON = ":"
    const val PLACEHOLDER_PREFIX = "\${"
    const val PLACEHOLDER_SUFFIX = "}"

}
