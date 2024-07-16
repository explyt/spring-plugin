package com.esprito.spring.core.runconfiguration

import com.intellij.execution.CommonJavaRunConfigurationParameters
import com.intellij.execution.JavaTestConfigurationBase
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.idea.run.KotlinRunConfiguration

object RunConfigurationUtil {

    fun getRunPsiClass(runConfiguration: RunConfiguration?): List<PsiClass> {
        return when (runConfiguration) {
            is KotlinRunConfiguration -> {
                runConfiguration.findMainClassFile()?.classes?.toList() ?: emptyList()
            }

            is SpringBootRunConfiguration -> {
                val mainClass = runConfiguration.mainClass
                if (mainClass is KtLightClassForFacade) {
                    return mainClass.files.flatMap { it.classes.toList() }
                }
                mainClass?.let { listOf(it) } ?: emptyList()
            }

            else -> emptyList()
        }
    }

    fun getRunClassName(runConfiguration: RunConfiguration?): String? {
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
            is ApplicationConfiguration -> stringToProfile(runConfiguration.envs["spring.profiles.active"])
            is KotlinRunConfiguration -> stringToProfile(runConfiguration.envs["spring.profiles.active"])
            is ExternalSystemRunConfiguration -> stringToProfile(runConfiguration.settings.env["spring.profiles.active"])
            is JavaTestConfigurationBase -> stringToProfile(runConfiguration.envs["spring.profiles.active"])
            else -> emptySet()
        }
    }

    private fun stringToProfile(string: String?): Set<String> {
        return string?.split(',')
            ?.filterTo(mutableSetOf()) { it.isNotBlank() }
            ?: emptySet()
    }
}