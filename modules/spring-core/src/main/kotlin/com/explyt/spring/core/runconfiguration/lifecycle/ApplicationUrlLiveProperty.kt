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
import com.intellij.util.net.NetUtils

open class ApplicationUrlLiveProperty(
    private val serverPort: ServerPortLiveProperty,
    private val serverConfiguration: ServerConfigurationLiveProperty,
    errorHandler: LifecycleErrorHandler,
    parent: Disposable?
) : AsyncLiveProperty<String?>(errorHandler, parent) {

    init {
        serverPort.addListener(object : LiveProperty.PropertyListener {
            override fun onFinish() {
                retrieve()
            }
        })
        serverConfiguration.addListener(object : LiveProperty.PropertyListener {
            override fun onFinish() {
                retrieve()
            }
        })
    }

    override fun compute(): String? {
        val serverPortValue = serverPort.value?.takeIf { it > 0 } ?: return null

        var path = ""
        var scheme = "http"
        var address = NetUtils.getLocalHostString()

        val serverConfigurationValue = serverConfiguration.value
        if (serverConfigurationValue != null) {
            if (serverConfigurationValue.isSslEnabled) {
                scheme = "https"
            }

            val contextPath = serverConfigurationValue.contextPath
            if (contextPath != null) {
                path += contextPath
            }

            val serverAddress = serverConfigurationValue.address
            if (serverAddress?.isNotBlank() == true) {
                address = serverAddress
            }
        }

        return "$scheme://$address:$serverPortValue$path"
    }
}