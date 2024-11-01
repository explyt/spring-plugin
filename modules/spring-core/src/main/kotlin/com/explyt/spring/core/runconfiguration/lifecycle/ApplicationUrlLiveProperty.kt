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