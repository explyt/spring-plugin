package com.esprito.spring.core.externalsystem.view

import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.externalsystem.setting.NativeSettings
import com.esprito.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.intellij.openapi.externalSystem.service.task.ui.AbstractExternalSystemToolWindowFactory
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings
import com.intellij.openapi.project.Project

class SpringBootToolWindowFactory : AbstractExternalSystemToolWindowFactory(SYSTEM_ID) {

    override val icon = SpringIcons.SpringBootToolWindow

    override fun getSettings(project: Project): AbstractExternalSystemSettings<*, *, *> =
        project.getService(NativeSettings::class.java)
}
