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