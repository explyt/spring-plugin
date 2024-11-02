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

package com.explyt.spring.core.externalsystem.process

import com.explyt.spring.boot.bean.reader.BeanPrinter
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
    var classNotFoundError = false

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
            if (event.text.contains("NoClassDefFoundError")) {
                classNotFoundError = true
            }
            listener.onTaskOutput(id, event.text, true)
        }
    }

    fun await() {
        latch.await()
    }

    fun getSpringContextInfo(): SpringContextInfo {
        latch.await()
        val beanInfos = mutableListOf<BeanInfo>()
        val aspectInfos = mutableListOf<AspectInfo>()
        for (line in output.stdoutLines) {
            if (line.startsWith(BeanPrinter.EXPLYT_BEAN_INFO)) {
                val jsonString = line.substringAfter(BeanPrinter.EXPLYT_BEAN_INFO)
                getBeanInfo(jsonString)?.let { beanInfos.add(it) }
            } else if (line.startsWith(BeanPrinter.EXPLYT_AOP_INFO)) {
                val jsonString = line.substringAfter(BeanPrinter.EXPLYT_AOP_INFO)
                getAopInfo(jsonString)?.let { aspectInfos.add(it) }
            }
        }
        return SpringContextInfo(beanInfos, aspectInfos)
    }

    private fun getBeanInfo(it: String) = try {
        gson.fromJson(it, BeanInfo::class.java)
    } catch (e: Exception) {
        null
    }

    private fun getAopInfo(it: String) = try {
        gson.fromJson(it, AspectInfo::class.java)
    } catch (e: Exception) {
        null
    }

    private fun checkExplytLogStart(text: String?) {
        if (!explytLog && text?.contains(BeanPrinter.EXPLYT_BEAN_INFO_START) == true) {
            explytLog = true
        }
    }
}

data class SpringContextInfo(val beans: List<BeanInfo>, val aspects: List<AspectInfo>)

data class BeanInfo(
    var beanName: String,
    var className: String,
    var methodName: String?,
    var methodType: String?,
    var scope: String,
    var primary: Boolean,
    var rootBean: Boolean
)

data class AspectInfo(
    var aspectName: String,
    var aspectMethodName: String,
    var beanName: String,
    var methodName: String,
    var methodParams: String,
)