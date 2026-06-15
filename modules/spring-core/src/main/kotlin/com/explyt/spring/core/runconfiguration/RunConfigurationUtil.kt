/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.runconfiguration

import com.explyt.spring.core.SpringProperties.SPRING_PROFILES_ACTIVE
import com.explyt.spring.core.externalsystem.RunConfigurationExtractor
import com.intellij.execution.CommonJavaRunConfigurationParameters
import com.intellij.execution.JavaTestConfigurationBase
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.idea.run.KotlinRunConfiguration
import org.jetbrains.kotlin.psi.KtFile


object RunConfigurationUtil {

    fun getRunPsiClass(runConfiguration: RunConfiguration?): List<PsiClass> {
        return if (ApplicationManager.getApplication().isReadAccessAllowed) {
            getRunPsiClassInner(runConfiguration)
        } else {
            ApplicationManager.getApplication()
                .runReadAction(Computable { getRunPsiClassInner(runConfiguration) })
        }
    }

    private fun getRunPsiClassInner(runConfiguration: RunConfiguration?): List<PsiClass> {
        return when (runConfiguration) {
            is KotlinRunConfiguration -> {
                RunConfigurationExtractor.findKotlinMainClassFile(runConfiguration)?.classes?.toList() ?: emptyList()
            }

            is ApplicationConfiguration -> {
                val mainClass = runConfiguration.mainClass
                if (mainClass is KtLightClassForFacade) {
                    return mainClass.files.flatMap { it.classes.toList() }
                }
                if (mainClass?.containingFile is KtFile) {
                    return (mainClass.containingFile as KtFile).classes.toList()
                }
                mainClass?.let { listOf(it) } ?: emptyList()
            }

            else -> emptyList()
        }
    }

    fun getRunClassName(runConfiguration: RunConfiguration?): String? {
        return if (ApplicationManager.getApplication().isReadAccessAllowed) {
            getRunClassNameInner(runConfiguration)
        } else {
            ApplicationManager.getApplication()
                .runReadAction(Computable { getRunClassNameInner(runConfiguration) })
        }
    }

    fun getRunClassNameInner(runConfiguration: RunConfiguration?): String? {
        return when (runConfiguration) {
            is SpringBootRunConfiguration -> runConfiguration.mainClassName
            is CommonJavaRunConfigurationParameters -> runConfiguration.runClass
            else -> null
        }
    }

    fun getProfiles(settings: RunnerAndConfigurationSettings?): Set<String> {
        val runConfiguration = settings?.configuration ?: return emptySet()
        return when (runConfiguration) {
            is SpringBootRunConfiguration -> stringToProfile(runConfiguration.springProfiles)
            is ApplicationConfiguration -> stringToProfile(runConfiguration.envs[SPRING_PROFILES_ACTIVE])
            is KotlinRunConfiguration -> stringToProfile(runConfiguration.envs[SPRING_PROFILES_ACTIVE])
            is ExternalSystemRunConfiguration -> stringToProfile(runConfiguration.settings.env[SPRING_PROFILES_ACTIVE])
            is JavaTestConfigurationBase -> stringToProfile(runConfiguration.envs[SPRING_PROFILES_ACTIVE])
            else -> emptySet()
        }
    }

    fun stringToProfile(string: String?): Set<String> {
        return string?.split(',')?.map { it.trim() }
            ?.filterTo(mutableSetOf()) { it.isNotBlank() }
            ?: emptySet()
    }
}