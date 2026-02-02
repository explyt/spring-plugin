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