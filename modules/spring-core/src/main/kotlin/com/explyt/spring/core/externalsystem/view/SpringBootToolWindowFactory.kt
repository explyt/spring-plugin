/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.view

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.externalsystem.setting.NativeSettings
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.intellij.openapi.externalSystem.service.task.ui.AbstractExternalSystemToolWindowFactory
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings
import com.intellij.openapi.project.Project

class SpringBootToolWindowFactory : AbstractExternalSystemToolWindowFactory(SYSTEM_ID) {

    override fun getSettings(project: Project): AbstractExternalSystemSettings<*, *, *> =
        project.getService(NativeSettings::class.java)
}
