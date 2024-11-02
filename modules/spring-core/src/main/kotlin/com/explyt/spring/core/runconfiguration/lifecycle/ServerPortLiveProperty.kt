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

import com.intellij.openapi.Disposable

class ServerPortLiveProperty(
    moduleDescriptor: LiveProperty<SpringBootModuleDescriptor>,
    serviceUrl: LiveProperty<String?>,
    errorHandler: LifecycleErrorHandler,
    parent: Disposable?
) : AsyncApplicationLiveProperty<Int?>(
    moduleDescriptor, serviceUrl, errorHandler, parent, DEFAULT_VALUE
) {
    @Throws(LifecycleException::class)
    override fun compute(): Int {
        if (this.isDisposed)
            return DEFAULT_VALUE

        applicationConnector?.use { connector ->
            try {
                var lastThrownException: Exception? = null

                for (i in 0..<SERVER_PORT_RETRY_COUNT) {
                    if (this.isDisposed) {
                        return DEFAULT_VALUE
                    }

                    try {
                        val serverPort = connector.serverPort
                        if (serverPort > 0) {
                            return serverPort
                        }
                    } catch (ex: Exception) {
                        lastThrownException = ex
                    }

                    try {
                        Thread.sleep(SERVER_PORT_RETRY_INTERVAL)
                    } catch (_: InterruptedException) {
                        return DEFAULT_VALUE
                    }
                }

                if (lastThrownException != null) {
                    throw LifecycleException("Failed to retrieve application port", lastThrownException)
                }
            } catch (ex: Throwable) {
                try {
                    connector.close()
                } catch (suppressed: Throwable) {
                    ex.addSuppressed(suppressed)
                }

                throw ex
            }
        }
        return DEFAULT_VALUE
    }

    companion object {
        private const val SERVER_PORT_RETRY_INTERVAL = 200L
        private const val SERVER_PORT_RETRY_COUNT = 20
        private const val DEFAULT_VALUE = -1
    }

}
