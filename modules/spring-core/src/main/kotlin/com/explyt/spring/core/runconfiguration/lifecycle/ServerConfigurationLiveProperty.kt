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

package com.explyt.spring.core.runconfiguration.lifecycle

import com.explyt.spring.core.runconfiguration.lifecycle.ServerConfigurationLiveProperty.ApplicationServerConfiguration
import com.explyt.spring.core.runconfiguration.lifecycle.SpringBootApplicationConnector.Companion.SERVER_ADDRESS
import com.explyt.spring.core.runconfiguration.lifecycle.SpringBootApplicationConnector.Companion.SERVER_CONTEXT_PATH_PROPERTY
import com.explyt.spring.core.runconfiguration.lifecycle.SpringBootApplicationConnector.Companion.SERVER_SERVLET_PATH_PROPERTY
import com.explyt.spring.core.runconfiguration.lifecycle.SpringBootApplicationConnector.Companion.SERVER_SSL_ENABLED
import com.explyt.spring.core.runconfiguration.lifecycle.SpringBootApplicationConnector.Companion.SERVER_SSL_KEY_STORE
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
