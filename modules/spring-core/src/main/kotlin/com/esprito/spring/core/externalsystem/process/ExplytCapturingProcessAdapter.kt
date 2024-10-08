package com.esprito.spring.core.externalsystem.process

import com.explyt.spring.boot.bean.reader.SpringBootBeanReaderStarter
import com.google.gson.Gson
import com.intellij.execution.process.CapturingProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.registry.Registry
import java.util.concurrent.CountDownLatch

class ExplytCapturingProcessAdapter(
    private val id: ExternalSystemTaskId,
    private val listener: ExternalSystemTaskNotificationListener
) : CapturingProcessAdapter() {
    private val latch = CountDownLatch(1)
    private var explytLog = false
    private val gson = Gson()

    override fun processTerminated(event: ProcessEvent) {
        super.processTerminated(event)
        latch.countDown()
    }

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        checkExplytLogStart(event.text)
        if (explytLog) {
            super.onTextAvailable(event, outputType)
            if (Registry.`is`("explyt.spring.process.log.all")) {
                listener.onTaskOutput(id, event.text, true)
            }
        } else {
            listener.onTaskOutput(id, event.text, true)
        }
    }

    fun await() {
        latch.await()
    }

    fun getBeans(): List<BeanInfo> {
        latch.await()
        return output.stdoutLines.asSequence()
            .filter { it.startsWith(SpringBootBeanReaderStarter.EXPLYT_BEAN_INFO) }
            .map { it.substringAfter(SpringBootBeanReaderStarter.EXPLYT_BEAN_INFO) }
            .mapNotNull { getBeanInfo(it) }
            .toList()
    }

    private fun getBeanInfo(it: String) = try {
        gson.fromJson(it, BeanInfo::class.java)
    } catch (e: Exception) {
        null
    }

    private fun checkExplytLogStart(text: String?) {
        if (!explytLog && text?.contains(SpringBootBeanReaderStarter.EXPLYT_BEAN_INFO_START) == true) {
            explytLog = true
        }
    }
}

data class BeanInfo(
    var beanName: String,
    var className: String,
    var methodName: String?,
    var methodType: String?,
    var type: String?,
    var scope: String,
    var primary: Boolean,
    var rootBean: Boolean
)