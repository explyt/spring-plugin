package com.esprito.spring.core.runconfiguration.lifecycle

import com.esprito.spring.core.runconfiguration.SpringBootRunConfiguration
import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import com.jetbrains.rd.util.concurrentMapOf

@Service(Service.Level.PROJECT)
class SpringBootLifecycleManager(val project: Project) : Disposable {
    private val infos: MutableMap<ProcessHandler, SpringBootApplicationInfo> = concurrentMapOf()

    init {
        val disposableRoot = this

        project.messageBus.connect(this).subscribe(ExecutionManager.EXECUTION_TOPIC, object : ExecutionListener {
            override fun processStarted(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
                val settings = env.runnerAndConfigurationSettings ?: return
                val configuration = getConfiguration(settings) ?: return
                val info = SpringBootApplicationInfo(project, configuration, handler)
                Disposer.register(disposableRoot, info)
                infos[handler] = info
            }
        })
    }

    fun getInfo(handler: ProcessHandler): SpringBootApplicationInfo? {
        return infos[handler]
    }

    fun getDebugSession(handler: ProcessHandler): XDebugSession? {
        for (session in XDebuggerManager.getInstance(project).debugSessions) {
            if (session.debugProcess.processHandler == handler)
                return session
        }

        return null
    }

    companion object {
        val JMX_PORT: Key<Int> = Key.create("SPRING_BOOT_APPLICATION_JMX_PORT")
        val PROCESS_UID: Key<String> = Key.create("SPRING_BOOT_APPLICATION_PROCESS_UID")

        fun getInstance(project: Project): SpringBootLifecycleManager = project.service()
        fun getConfiguration(settings: RunnerAndConfigurationSettings): SpringBootRunConfiguration? {
            return settings.configuration as? SpringBootRunConfiguration
        }
    }

    override fun dispose() {
        infos.clear()
    }

}