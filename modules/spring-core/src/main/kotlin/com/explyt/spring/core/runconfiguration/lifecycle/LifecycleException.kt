package com.explyt.spring.core.runconfiguration.lifecycle

class LifecycleException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    constructor(cause: Throwable) : this(null, cause)
}
