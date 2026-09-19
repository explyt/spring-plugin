/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web

object SpringWebClasses {

    const val MODEL_ATTRIBUTE = "org.springframework.web.bind.annotation.ModelAttribute"

    const val WEB_INITIALIZER = "org.springframework.web.WebApplicationInitializer"


    const val CONTROLLER = "org.springframework.stereotype.Controller"
    const val REST_CONTROLLER = "org.springframework.web.bind.annotation.RestController"
    const val RESPONSE_BODY = "org.springframework.web.bind.annotation.ResponseBody"
    const val REQUEST_MAPPING = "org.springframework.web.bind.annotation.RequestMapping"
    const val PATH_VARIABLE = "org.springframework.web.bind.annotation.PathVariable"
    const val REQUEST_PARAM = "org.springframework.web.bind.annotation.RequestParam"
    const val REQUEST_HEADER = "org.springframework.web.bind.annotation.RequestHeader"
    const val COOKIE_VALUE = "org.springframework.web.bind.annotation.CookieValue"
    const val REQUEST_BODY = "org.springframework.web.bind.annotation.RequestBody"
    const val RESPONSE_ENTITY = "org.springframework.http.ResponseEntity"
    const val FEIGN_CLIENT = "org.springframework.cloud.openfeign.FeignClient"
    const val JAVA_HTTP_CLIENT = "java.net.http.HttpClient"
    const val HTTP_EXCHANGE = "org.springframework.web.service.annotation.HttpExchange"

    const val RETROFIT_PATH_PARAM = "retrofit2.http.Path"
    const val RETROFIT_QUERY_PARAM = "retrofit2.http.Query"
    const val RETROFIT_HEADER_PARAM = "retrofit2.http.Header"
    const val RETROFIT_HTTP = "retrofit2.http.HTTP"

    const val MOCK_MVC_REQUEST_BUILDERS = "org.springframework.test.web.servlet.request.MockMvcRequestBuilders"
    const val MOCK_MVC = "org.springframework.test.web.servlet.MockMvc"
    private const val WEB_CLIENT = "org.springframework.web.reactive.function.client.WebClient"
    const val WEB_TEST_CLIENT = "org.springframework.test.web.reactive.server.WebTestClient"
    const val WEB_TEST_CLIENT_URI_SPEC = "$WEB_TEST_CLIENT.UriSpec"
    const val WEB_CLIENT_URI_SPEC = "$WEB_CLIENT.UriSpec"
    const val WEB_CLIENT_RESPONSE_SPEC = "$WEB_CLIENT.ResponseSpec"
    const val WEB_TEST_CLIENT_RESPONSE_SPEC = "$WEB_TEST_CLIENT.ResponseSpec"

    const val MONO = "reactor.core.publisher.Mono"
    const val FLUX = "reactor.core.publisher.Flux"
    const val FLOW = "kotlinx.coroutines.flow.Flow"

    const val SWAGGER_API = "io.swagger.annotations.Api"

    const val URL_BASED_VIEW_RESOLVER = "org.springframework.web.servlet.view.UrlBasedViewResolver"
    const val ABSTRACT_CONFIGURABLE_TEMPLATE_RESOLVER =
        "org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver"

    const val JAVAX_SERVLET_CONTEXT: String = "javax.servlet.ServletContext"
    const val JAKARTA_SERVLET_CONTEXT: String = "jakarta.servlet.ServletContext"

    const val JAVAX_SERVLET_CONFIG: String = "javax.servlet.ServletConfig"
    const val JAKARTA_SERVLET_CONFIG: String = "jakarta.servlet.ServletConfig"

    const val JAVAX_SERVLET_REQUEST: String = "javax.servlet.ServletRequest"
    const val JAKARTA_SERVLET_REQUEST: String = "jakarta.servlet.ServletRequest"

    const val JAVAX_HTTP_SERVLET_REQUEST: String = "javax.servlet.http.HttpServletRequest"
    const val JAKARTA_HTTP_SERVLET_REQUEST: String = "jakarta.servlet.http.HttpServletRequest"

    const val JAVAX_HTTP_SERVLET_RESPONSE: String = "javax.servlet.http.HttpServletResponse"
    const val JAKARTA_HTTP_SERVLET_RESPONSE: String = "jakarta.servlet.http.HttpServletResponse"

    const val JAVAX_HTTP_SESSION: String = "javax.servlet.http.HttpSession"
    const val JAKARTA_HTTP_SESSION: String = "jakarta.servlet.http.HttpSession"

    const val WEB_APPLICATION_CONTEXT: String = "org.springframework.web.context.WebApplicationContext"

    const val INIT_BINDER: String = "org.springframework.web.bind.annotation.InitBinder"

    const val ROUTE_FUNCTION = "org.springframework.web.reactive.function.server.RouterFunction"
    const val ROUTE_FUNCTION_BUILDER = "org.springframework.web.reactive.function.server.RouterFunctions.Builder"

    const val SERVLET_ROUTE_FUNCTION = "org.springframework.web.servlet.function.RouterFunction"
    const val SERVLET_ROUTE_FUNCTION_BUILDER = "org.springframework.web.servlet.function.RouterFunctions.Builder"

    val ROUTE_FUNCTION_BUILDERS = listOf(ROUTE_FUNCTION_BUILDER, SERVLET_ROUTE_FUNCTION_BUILDER)

    /**
     * Entry points of the Kotlin router DSL. `router` names both the reactive and the servlet DSL, so the stack is
     * told apart by the enclosing bean's type rather than by this name.
     */
    val ROUTER_DSL_ENTRY_POINTS = listOf("coRouter", "router")

    const val OPEN_FEIGN_CLIENT_CONFIG = "spring.cloud.openfeign.client.config"

    const val ROUTER_DSL_GENERIC_METHOD = "method"

    /**
     * Route methods of the Kotlin router DSL. `method` carries its verb in the argument instead of the name, so a
     * caller matching on this list must read the verb rather than reuse the called name.
     */
    val ROUTER_DSL_ROUTE_METHODS = listOf(
        "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", ROUTER_DSL_GENERIC_METHOD
    )

    /** HTTP verbs offered as tool window filter entries, kept in step with the icons rendered per verb. */
    val HTTP_METHOD_FILTER = listOf(
        "CONNECT", "DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT", "TRACE"
    )

}
