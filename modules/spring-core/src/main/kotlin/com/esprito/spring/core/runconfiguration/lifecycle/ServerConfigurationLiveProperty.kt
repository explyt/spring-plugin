package com.esprito.spring.core.runconfiguration.lifecycle

import com.esprito.spring.core.runconfiguration.lifecycle.ServerConfigurationLiveProperty.ApplicationServerConfiguration
import com.esprito.spring.core.runconfiguration.lifecycle.SpringBootApplicationConnector.Companion.SERVER_ADDRESS
import com.esprito.spring.core.runconfiguration.lifecycle.SpringBootApplicationConnector.Companion.SERVER_CONTEXT_PATH_PROPERTY
import com.esprito.spring.core.runconfiguration.lifecycle.SpringBootApplicationConnector.Companion.SERVER_SERVLET_PATH_PROPERTY
import com.esprito.spring.core.runconfiguration.lifecycle.SpringBootApplicationConnector.Companion.SERVER_SSL_ENABLED
import com.esprito.spring.core.runconfiguration.lifecycle.SpringBootApplicationConnector.Companion.SERVER_SSL_KEY_STORE
import com.intellij.openapi.Disposable

class ServerConfigurationLiveProperty(
    moduleDescriptor: LiveProperty<SpringBootModuleDescriptor>,
    serviceUrl: LiveProperty<String?>,
    errorHandler: LifecycleErrorHandler,
    parent: Disposable?
) : AsyncApplicationLiveProperty<ApplicationServerConfiguration?>(
    moduleDescriptor, serviceUrl, errorHandler, parent
) {
    override fun compute(): ApplicationServerConfiguration? {
        applicationConnector?.use { connector ->
            val serverConfiguration: ApplicationServerConfiguration
            var sslEnabled = false
            val keyStore = connector.getProperty(SERVER_SSL_KEY_STORE)
            if (keyStore != null) {
                sslEnabled = connector.getBooleanProperty(SERVER_SSL_ENABLED, true)
            }

            val contextPath = connector.getProperty(SERVER_CONTEXT_PATH_PROPERTY)
            val servletPath = connector.getProperty(SERVER_SERVLET_PATH_PROPERTY)
            val address = connector.getProperty(SERVER_ADDRESS)
            serverConfiguration = ApplicationServerConfiguration(
                sslEnabled, contextPath?.toString(), servletPath?.toString(), address?.toString()
            )

            return serverConfiguration
        } ?: return null
    }

    data class ApplicationServerConfiguration(
        val isSslEnabled: Boolean, val contextPath: String?, val servletPath: String?, val address: String?
    )
}
