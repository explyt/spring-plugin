package com.esprito.spring.core.externalsystem

import com.esprito.spring.core.externalsystem.setting.NativeExecutionSettings
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.task.ExternalSystemTaskManager

class SpringBootNativeTaskManager : ExternalSystemTaskManager<NativeExecutionSettings> {
    override fun cancelTask(id: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener) = false
}