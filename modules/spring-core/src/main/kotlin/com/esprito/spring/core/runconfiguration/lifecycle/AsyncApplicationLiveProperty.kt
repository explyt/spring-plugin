package com.esprito.spring.core.runconfiguration.lifecycle

import com.intellij.openapi.Disposable

abstract class AsyncApplicationLiveProperty<T> protected constructor(
    private val moduleDescriptor: LiveProperty<SpringBootModuleDescriptor>,
    private val serviceUrl: LiveProperty<String?>,
    errorHandler: LifecycleErrorHandler,
    parent: Disposable?,
    defaultValue: T? = null
) : AsyncLiveProperty<T>(errorHandler, parent, defaultValue) {

    protected val applicationConnector: SpringBootApplicationConnector?
        get() {
            val url = serviceUrl.value ?: return null
            val descriptor = moduleDescriptor.value ?: return null

            return SpringBootApplicationConnector(url, descriptor)
        }

}
