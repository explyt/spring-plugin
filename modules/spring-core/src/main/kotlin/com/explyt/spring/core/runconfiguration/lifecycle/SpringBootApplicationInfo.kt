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