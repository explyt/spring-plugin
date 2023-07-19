package com.esprito.spring.core

object SpringCoreClasses {
    const val INJECT = "javax.inject.Inject"

    const val AUTOWIRED = "org.springframework.beans.factory.annotation.Autowired"

    const val VALUE = "org.springframework.beans.factory.annotation.Value"

    const val EVENT_LISTENER = "org.springframework.context.event.EventListener"
    const val BEAN = "org.springframework.context.annotation.Bean"
    const val CONFIGURATION = "org.springframework.context.annotation.Configuration"

    const val COMPONENT = "org.springframework.stereotype.Component"
    const val CONTROLLER = "org.springframework.stereotype.Controller"

    const val REQUEST_MAPPING = "org.springframework.web.bind.annotation.RequestMapping"
    const val INIT_BINDER = "org.springframework.web.bind.annotation.InitBinder"
    const val MODEL_ATTRIBUTE = "org.springframework.web.bind.annotation.ModelAttribute"

    val FIELD_ANNOTATIONS = setOf(INJECT, AUTOWIRED, VALUE)
    val CONFIGURATION_METHOD_ANNOTATIONS = setOf(BEAN)
    val TYPE_ANNOTATIONS = setOf(COMPONENT)
    val CONTROLLER_METHOD_ANNOTATIONS = setOf(REQUEST_MAPPING, INIT_BINDER, MODEL_ATTRIBUTE)
}