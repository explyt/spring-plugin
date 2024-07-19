package com.esprito.spring.web

object SpringWebClasses {

    const val MODEL_ATTRIBUTE = "org.springframework.web.bind.annotation.ModelAttribute"

    const val WEB_INITIALIZER = "org.springframework.web.WebApplicationInitializer"


    const val CONTROLLER = "org.springframework.stereotype.Controller"
    const val RESPONSE_BODY = "org.springframework.web.bind.annotation.ResponseBody"
    const val REQUEST_MAPPING = "org.springframework.web.bind.annotation.RequestMapping"
    const val PATH_VARIABLE = "org.springframework.web.bind.annotation.PathVariable"
    const val RESPONSE_ENTITY = "org.springframework.http.ResponseEntity"

    const val MOCK_MVC_REQUEST_BUILDERS = "org.springframework.test.web.servlet.request.MockMvcRequestBuilders"
    const val WEB_TEST_CLIENT_URI_SPEC = "org.springframework.test.web.reactive.server.WebTestClient.UriSpec"
    const val WEB_CLIENT_URI_SPEC = "org.springframework.web.reactive.function.client.WebClient.UriSpec"
    const val WEB_CLIENT_RESPONSE_SPEC = "org.springframework.web.reactive.function.client.WebClient.ResponseSpec"
    const val WEB_TEST_CLIENT_RESPONSE_SPEC = "org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec"

    const val MONO = "reactor.core.publisher.Mono"
    const val FLUX = "reactor.core.publisher.Flux"
    const val FLOW = "kotlinx.coroutines.flow.Flow"

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

}