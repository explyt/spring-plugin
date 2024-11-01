package com.explyt.spring.core.runconfiguration.lifecycle

fun interface LifecycleErrorHandler {
    fun handleLifecycleError(message: String)
}