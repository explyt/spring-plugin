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

import com.intellij.debugger.impl.attach.JavaDebuggerAttachUtil
import com.intellij.execution.process.BaseProcessHandler
import com.intellij.execution.process.OSProcessUtil
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.text.StringUtil
import com.intellij.remote.RemoteProcess
import com.intellij.util.net.NetUtils
import com.sun.tools.attach.AttachNotSupportedException
import com.sun.tools.attach.VirtualMachine
import java.io.IOException

class JmxServiceUrlLiveProperty(
    errorHandler: LifecycleErrorHandler,
    parent: Disposable?,
    private val processHandler: ProcessHandler,
    private val className: String?
) : AsyncLiveProperty<String?>(errorHandler, parent, "") {
    @Throws(LifecycleException::class)
    override fun compute(): String? {
        val vm: VirtualMachine?
        val url: String?

        try {
            vm = this.attachVirtualMachine() ?: throw AttachNotSupportedException("Process not found")
        } catch (ex: AttachNotSupportedException) {
            return connectionUrlByPort ?: throw LifecycleException(ex)
        } catch (ex: IOException) {
            return connectionUrlByPort ?: throw LifecycleException(ex)
        } catch (ex: Throwable) {
            url = this.connectionUrlByPort
            if (url != null) {
                return url
            }

            throw LifecycleException("Failed to attach to VM", ex)
        }

        try {
            url = vm.agentProperties.getProperty(JMX_CONNECTION_ADDRESS_PROPERTY)
            if (StringUtil.isEmpty(url)) {
                throw LifecycleException("JMX agent not loaded")
            }
        } catch (ex: Throwable) {
            return this.connectionUrlByPort ?: throw LifecycleException("Failed to retrieve JMX service url", ex)
        } finally {
            try {
                vm.detach()
            } catch (_: IOException) {
            }
        }

        return url
    }

    private val connectionUrlByPort: String?
        get() {
            val jmxPort = processHandler.getUserData(SpringBootLifecycleManager.JMX_PORT)
            return if (jmxPort != null && jmxPort > 0) {
                "service:jmx:rmi:///jndi/rmi://${NetUtils.getLocalHostString()}:$jmxPort/jmxrmi"
            } else {
                null
            }
        }

    @Throws(IOException::class, AttachNotSupportedException::class)
    private fun attachVirtualMachine(): VirtualMachine? {
        if (processHandler is BaseProcessHandler<*>) {
            return attachByProcess(processHandler.process)
        } else {
            val processUID = processHandler.getUserData(SpringBootLifecycleManager.PROCESS_UID) ?: return null
            return className?.let { attachByEnv(it, processUID) }
        }
    }

    companion object {
        private const val JMX_CONNECTION_ADDRESS_PROPERTY = "com.sun.management.jmxremote.localConnectorAddress"

        @Throws(IOException::class, AttachNotSupportedException::class)
        private fun attachByProcess(process: Process): VirtualMachine? {
            if (process is RemoteProcess) {
                return null
            } else {
                val pid: String
                try {
                    pid = OSProcessUtil.getProcessID(process).toString()
                } catch (var3: IllegalStateException) {
                    return null
                }

                return JavaDebuggerAttachUtil.attachVirtualMachine(pid)
            }
        }

        private fun attachByEnv(className: String, processUID: String): VirtualMachine? {
            return VirtualMachine.list().asSequence()
                .filter {
                    val displayName = it.displayName()?.split(" ")?.firstOrNull() ?: return@filter false

                    className == displayName
                }
                .mapNotNull {
                    try {
                        JavaDebuggerAttachUtil.attachVirtualMachine(it.id())
                    } catch (_: AttachNotSupportedException) {
                        null
                    } catch (_: IOException) {
                        null
                    }
                }
                .firstOrNull {
                    processUID == it.systemProperties.getProperty("ij.spring.boot.application.process")
                }
        }
    }
}
