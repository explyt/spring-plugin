package com.esprito.spring.core.runconfiguration.lifecycle

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
