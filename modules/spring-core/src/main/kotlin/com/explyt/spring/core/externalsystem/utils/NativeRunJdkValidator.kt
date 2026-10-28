/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.utils

import com.explyt.spring.core.SpringCoreBundle
import com.intellij.execution.CantRunException
import com.intellij.execution.CommonJavaRunConfigurationParameters
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.ModuleBasedConfiguration
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.model.ExternalSystemException
import com.intellij.openapi.util.Computable

/**
 * A sync started while the build tool is still importing the project can meet a module whose JDK is not assigned
 * yet. Launching it anyway fails deep inside the platform launcher, which logs the failure as an unhandled coroutine
 * error blamed on this plugin, and the sync then waits for an application that never starts.
 */
object NativeRunJdkValidator {

    fun requireJdkToRun(configuration: RunConfiguration) {

        if ((configuration as? CommonJavaRunConfigurationParameters)?.isAlternativeJrePathEnabled == true) return
        val module = (configuration as? ModuleBasedConfiguration<*, *>)?.configurationModule?.module ?: return
        try {
            ApplicationManager.getApplication().runReadAction(Computable {
                JavaParameters.getValidJdkToRunModule(module, true)
            })
        } catch (e: CantRunException) {
            throw ExternalSystemException(
                SpringCoreBundle.message("explyt.external.project.sync.no.jdk.error", e.message.orEmpty())
            )
        }
    }
}
