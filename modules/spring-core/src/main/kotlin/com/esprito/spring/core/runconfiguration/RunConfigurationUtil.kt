package com.esprito.spring.core.runconfiguration

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.idea.run.KotlinRunConfiguration

object RunConfigurationUtil {

    fun getRunPsiClass(runConfiguration: RunConfiguration?): PsiClass? {
        return when (runConfiguration) {
            is KotlinRunConfiguration -> runConfiguration.configurationModule.findClass(runConfiguration.runClass)
            is SpringBootRunConfiguration -> runConfiguration.mainClass
            else -> null
        }
    }

    fun getRunClassName(runConfiguration: RunConfiguration?): String? {
        return when (runConfiguration) {
            is KotlinRunConfiguration -> runConfiguration.runClass
            is SpringBootRunConfiguration -> runConfiguration.mainClassName
            else -> null
        }
    }
}