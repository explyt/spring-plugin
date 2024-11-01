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

import com.explyt.spring.core.runconfiguration.SpringBootRunConfiguration
import com.intellij.execution.dashboard.RunDashboardManager
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import kotlin.concurrent.Volatile

class SpringBootApplicationInfo(
    private val project: Project, private val configuration: SpringBootRunConfiguration, handler: ProcessHandler
) : Disposable {

    @Volatile
    private var isDisposed = false

    private val module = configuration.configurationModule.module
    var readyState: ReadyStateLiveProperty
    var serverPort: ServerPortLiveProperty
    var applicationUrl: ApplicationUrlLiveProperty
    var serverConfiguration: ServerConfigurationLiveProperty
    private var moduleDescriptor: LiveProperty<SpringBootModuleDescriptor>

    init {
        val info = this

        val errorHandler = LifecycleErrorHandler { LOG.debug(it) }
        val mainClass = configuration.mainClassName

        moduleDescriptor = object : AsyncLiveProperty<SpringBootModuleDescriptor>(
            errorHandler, info, SpringBootModuleDescriptor.DEFAULT_DESCRIPTOR
        ) {
            override fun compute(): SpringBootModuleDescriptor {
                return DumbService.getInstance(project).runReadActionInSmartMode(Computable {
                    SpringBootModuleDescriptor.of(module)
                })
            }
        }

        val serviceUrl = JmxServiceUrlLiveProperty(errorHandler, this, handler, mainClass)
        readyState = ReadyStateLiveProperty(moduleDescriptor, serviceUrl, errorHandler, this)
        serverPort = ServerPortLiveProperty(moduleDescriptor, serviceUrl, errorHandler, this)
        serverConfiguration = ServerConfigurationLiveProperty(moduleDescriptor, serviceUrl, errorHandler, this)
        applicationUrl = ApplicationUrlLiveProperty(serverPort, serverConfiguration, errorHandler, this)

        moduleDescriptor.addListener(object : LiveProperty.PropertyListener {
            override fun onFinish() {
                readyState.retrieve()
            }
        })
        readyState.addListener(object : LiveProperty.PropertyListener {
            override fun changed() {
                emitConfigurationChanged()
                serverPort.retrieve()
            }

            override fun onFail(ex: Exception) {
                super.onFail(ex)
                emitConfigurationChanged()
            }
        })
        serverPort.addListener(object : LiveProperty.PropertyListener {
            override fun changed() {
                serverConfiguration.retrieve()
                emitConfigurationChanged()
            }
        })

        moduleDescriptor.retrieve()
    }

    private fun emitConfigurationChanged() {
        project.messageBus.syncPublisher(RunDashboardManager.DASHBOARD_TOPIC)
            .configurationChanged(configuration, false)
    }

    override fun dispose() {
        isDisposed = true
    }

    companion object {
        private val LOG = Logger.getInstance(SpringBootApplicationInfo::class.java)
    }

}