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

package com.explyt.spring.core.statistic

enum class StatisticActionId(description: String) {
    DEVICE_ID("IDEA Id"),
    PLUGIN_VERSION("Explyt Spring plugin version"),
    LOCAL_DATE("Local date for statistic"),

    SPRING_BOOT_PANEL_ADD("Explyt Spring Boot panel add new project"),
    SPRING_BOOT_PANEL_REFRESH("Explyt Spring Boot panel - refresh projects"),
    SPRING_BOOT_PANEL_REMOVE("Explyt Spring Boot panel - remove projects"),
    SPRING_BOOT_PANEL_CHANGE_PROFILE("Explyt Spring Boot panel - change profile state"),
    SPRING_BOOT_PANEL_BEAN_NAVIGATION("Explyt Spring Boot panel - navigate to Spring Bean"),
    SPRING_BOOT_PANEL_BEAN_ANALYZER("Explyt Spring Boot panel - open dependency analyzer"),
    SPRING_BOOT_PANEL_BEAN_ANALYZER_GO_TO("Explyt Spring Boot dependency analyzer - go to class"),
    SPRING_BOOT_PANEL_FLOATING_BUTTON_REFRESH("Explyt Spring Boot panel - floating button refresh"),
    SPRING_BOOT_PANEL_FLOATING_BUTTON_HIDE("Explyt Spring Boot panel - floating button hide"),

    ENDPOINTS_TOOLWINDOW_REFRESH("Endpoints toolwindow - pressed refresh button"),
    ENDPOINTS_TOOLWINDOW_HTTP_TYPE_FILTER("Endpoints toolwindow - changed HttpType filter"),
    ENDPOINTS_TOOLWINDOW_SEARCH_TEXT("Endpoints toolwindow - changed text filter"),
    ENDPOINTS_TOOLWINDOW_ENDPOINT_TYPE_FILTER("Endpoints toolwindow - changed EndpointType filter"),

    GUTTER_BEAN_USAGE("Gutter line marker for Spring bean go to usage"),
    GUTTER_BEAN_DECLARATION("Gutter line marker for Spring bean go to declaration"),
    GUTTER_BEAN_FACTORY_GET_BEAN("Gutter line marker for getBean method of BeanFactory to go to mentioned bean"),

    GUTTER_BEAN_LIBRARY_USAGE("Gutter line marker for Spring bean library go to usage"),
    GUTTER_BEAN_LIBRARY_DECLARATION("Gutter line marker for Spring bean library go to declaration"),

    GUTTER_FACTORIES_METADATA_FIND("Gutter line marker for Spring factories find metadata"),
    GUTTER_ASPECTJ_USAGE("Gutter line marker for Spring aspectj expression go to usage"),
    GUTTER_ASPECTJ_DECLARATION("Gutter line marker for Spring aspectj point cut go to declaration"),

    GUTTER_TARGET_ENDPOINTS_ROUTER_FUNCTION("Gutter line marker for bean method for route and coRouter"),
    GUTTER_TARGET_PROPERTY("Gutter line marker Configuration Properties go to usages"),
    GUTTER_TARGET_ADDITIONAL_METADATA("Gutter line marker to additional-spring-configuration-metadata.json"),
    GUTTER_TARGET_PUBLISH_EVENT("Gutter line marker for Spring Event go to publisher"),
    GUTTER_TARGET_EVENT_LISTENER("Gutter line marker Spring Event go to listener"),

    GUTTER_OPENAPI_GENERATE_ENDPOINT("Gutter line marker for generating openapi description for endpoint"),
    GUTTER_OPENAPI_ENDPOINT_OPEN_IN_SWAGGER("Gutter line marker for openapi navigating to swagger preview"),
    GUTTER_OPENAPI_CONTROLLER_OPEN_IN_SWAGGER("Gutter line marker for controller/feignClient generating openapi preview"),
    GUTTER_CONTROLLER_ENDPOINT_USAGE("Gutter line marker for Controllers - navigation to endpoint usage"),

    ACTION_OPENAPI_PROJECT_GENERATE_SPEC("An action that generates OpenApi Specification for whole project"),

    COMPLETION_SPRING_DATA_METHOD("Completion Spring Data - method name"),
    COMPLETION_PROPERTY_KEY_CONFIGURATION("Completion for application properties for key"),
    COMPLETION_YAML_KEY_CONFIGURATION("Completion for application yaml for key"),
    COMPLETION_SPRING_ADDITIONAL_METADATA("Completion in additional-spring-configuration-metadata.json"),
    COMPLETION_CONDITIONAL_ON_CONFIGURATION_PREFIX("Completion in ConditionalOnConfiguration for argument named prefix"),
    COMPLETION_PROFILES("Completion in Profiles annotation"),
    COMPLETION_OPENAPI_YAML_ENDPOINT("Completion in yaml openapi file providing existing endpoints"),
    COMPLETION_OPENAPI_JSON_ENDPOINT("Completion in json openapi file providing existing endpoints"),
    COMPLETION_WEB_CLIENT_METHOD_CALL_WITH_TYPE("Completion for Web(Test)Client methods like bodyToX providing type from existing endpoint"),

    SPRING_INITIALIZR_OPEN_PROJECT("Explyt Spring Initializr - open project"),
    SPRING_INITIALIZR_MODIFY_SETTING("Explyt Spring Initializr - modify setting"),
    SPRING_INITIALIZR_WIZARD("Explyt Spring Initializr - shown page"),
    SPRING_INITIALIZR_TEXT_LOCATION_CHANGE("Explyt Spring Initializr - edit location"),
    SPRING_INITIALIZR_FILE_CHOSEN("Explyt Spring Initializr - changed folder location"),

    QUICK_FIX_ADD_QUALIFIER("Quick fixed - added Qualifier"),
    QUICK_FIX_DUPLICATE_PREFIX("Quick fixed - showed classes @ConfigurationProperties with duplicate prefix"),

    QUICK_FIX_REQUEST_MAPPING_ADD_PATH_VARIABLE("Quick fixed - added missing path variable to endpoint method"),
    PREVIEW_REQUEST_MAPPING_ADD_PATH_VARIABLE("Quick fix preview showed for adding missing path variable to endpoint method"),

    QUICK_FIX_PROXY_BEAN_METHODS("Quick fixed - used dependency injection in proxied bean methods"),
    PREVIEW_PROXY_BEAN_METHODS("Quick fix preview showed - for using dependency injection in proxied bean methods"),

    QUICK_FIX_YAML_SWITCH_KEY_TO_KEBAB_CASE("Quick fixed - yaml key changed to kebab-case"),
    PREVIEW_YAML_SWITCH_KEY_TO_KEBAB_CASE("Quick fix preview showed - yaml key changed to kebab-case"),

    QUICK_FIX_CREATE_META_INFO_DESCRIPTION("Quick fixed - created description in meta info file"),
    PREVIEW_CREATE_META_INFO_DESCRIPTION("Quick fix preview showed - creating description in meta info file"),

    QUICK_FIX_FACTORIES_MOVE_TO_IMPORT("Quick fixed Spring Factories - move property to AutoConfiguration.imports"),
    QUICK_FIX_FACTORIES_REMOVE_PROPERTY("Quick fixed Spring Factories - remove empty property"),

    SETTINGS_OPEN("Explyt Settings - open"),
    SETTINGS_CHANGED("Explyt Settings - changed"),

    RUN_CONFIGURATION_OPEN("Explyt Run Configuration - open"),
    RUN_CONFIGURATION_CHANGED("Explyt Run Configuration - changed"),

    GENERATE_POST_CONSTRUCT("Method generate - @PostConstruct"),
    GENERATE_PRE_DESTROY("Method generate - @PreDestroy"),
    GENERATE_WEB_METHOD("Method generate - Spring Web Method from generate menu"),
    GENERATE_HTTP_CLIENT_METHOD("Method generate - Curl to HttpClient method"),
}