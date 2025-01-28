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

package com.explyt.spring.core.externalsystem

import com.explyt.spring.core.action.EXPLYT_APPLICATION_RUN
import com.explyt.spring.core.externalsystem.setting.NativeExecutionSettings
import com.explyt.spring.core.externalsystem.setting.RunConfigurationType
import com.explyt.spring.core.runconfiguration.SpringBootConfigurationFactory
import com.explyt.spring.core.runconfiguration.SpringBootRunConfiguration
import com.intellij.execution.RunManager
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.module.ModuleUtilCore
import org.jetbrains.kotlin.idea.run.KotlinRunConfiguration
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.visitor.AbstractUastVisitor

object RunConfigurationExtractor {
    fun findRunConfiguration(projectPath: String, settings: NativeExecutionSettings?): RunConfigurationHolder? {
        settings ?: return null
        val allConfigurationsList = RunManager.getInstance(settings.project).allConfigurationsList
        if (settings.runConfigurationType == RunConfigurationType.KOTLIN) {
            val runConfig = allConfigurationsList
                .filterIsInstance<KotlinRunConfiguration>()
                .find { it.name == settings.runConfigurationName }
            if (runConfig != null) {
                val useOriginal = useOriginalRunConfiguration(runConfig)
                val runConfiguration = mapToSpringBootRunConfiguration(runConfig, settings) ?: return null
                return RunConfigurationHolder(useOriginal, runConfiguration)
            }
        }
        if (settings.runConfigurationType == RunConfigurationType.APPLICATION) {
            val runConfig = allConfigurationsList
                .filterIsInstance<ApplicationConfiguration>()
                .find { it !is SpringBootRunConfiguration && it.name == settings.runConfigurationName }
            if (runConfig != null) {
                val useOriginal = useOriginalRunConfiguration(runConfig)
                val runConfiguration = mapToSpringBootRunConfiguration(runConfig, settings) ?: return null
                return RunConfigurationHolder(useOriginal, runConfiguration)
            }
        }
        val runConfig = if (settings.runConfigurationName != null) {
            allConfigurationsList.find { it is SpringBootRunConfiguration && it.name == settings.runConfigurationName }
        } else {
            allConfigurationsList.find { checkRunConfiguration(it, projectPath) }
        }

        if (runConfig is SpringBootRunConfiguration) {
            val useOriginal = useOriginalRunConfiguration(runConfig)
            return RunConfigurationHolder(useOriginal, runConfig.clone() as SpringBootRunConfiguration)
        }
        return null
    }

    private fun checkRunConfiguration(runConfiguration: RunConfiguration, projectPath: String): Boolean {
        return if (runConfiguration is SpringBootRunConfiguration) {
            runConfiguration.mainClass?.containingFile?.virtualFile?.canonicalPath == projectPath
        } else false
    }

    private fun useOriginalRunConfiguration(kotlinRunConfiguration: KotlinRunConfiguration): Boolean {
        val uClass = getKotlinUClass(kotlinRunConfiguration) ?: return false
        return useOriginalRunConfiguration(uClass)
    }

    private fun getKotlinUClass(kotlinRunConfiguration: KotlinRunConfiguration): UClass? {
        val mainClassFile = kotlinRunConfiguration.findMainClassFile() ?: return null
        val runClassQualifiedName = kotlinRunConfiguration.runClass ?: return null
        val uFile = mainClassFile.toUElement() as? UFile ?: return null
        return uFile.classes
            .firstOrNull { it.javaPsi.name != null && runClassQualifiedName.endsWith(it.javaPsi.name!!) }
    }

    private fun useOriginalRunConfiguration(applicationConfiguration: ApplicationConfiguration): Boolean {
        val mainUClass = applicationConfiguration.mainClass?.toUElement() as? UClass ?: return false
        return useOriginalRunConfiguration(mainUClass)
    }

    private fun useOriginalRunConfiguration(uClass: UClass): Boolean {
        val uMethod = uClass.methods.firstOrNull { it.name == "main" } ?: return false
        val uastBody = uMethod.uastBody ?: return false
        val visitor = object : AbstractUastVisitor() {
            var isExplytApplicationRun = false
            override fun visitCallExpression(node: UCallExpression): Boolean {
                if (!isExplytApplicationRun && node.asSourceString().contains(EXPLYT_APPLICATION_RUN)) {
                    isExplytApplicationRun = true
                }
                return super.visitCallExpression(node)
            }
        }
        uastBody.accept(visitor)
        return visitor.isExplytApplicationRun
    }

    private fun mapToSpringBootRunConfiguration(
        configuration: KotlinRunConfiguration, settings: NativeExecutionSettings
    ): SpringBootRunConfiguration? {

        val psiClass = getKotlinUClass(configuration)?.javaPsi ?: return null
        val module = ModuleUtilCore.findModuleForPsiElement(psiClass) ?: return null

        val runConfiguration = SpringBootConfigurationFactory
            .createTemplateConfiguration(settings.project)
            .apply {
                setModule(module)
                setMainClass(psiClass)
                setGeneratedName()
            }
        runConfiguration.vmParameters = configuration.vmParameters
        runConfiguration.envs = configuration.envs
        runConfiguration.programParameters = configuration.programParameters
        runConfiguration.isPassParentEnvs = configuration.isPassParentEnvs
        runConfiguration.alternativeJrePath = configuration.alternativeJrePath
        runConfiguration.classpathModifications = configuration.classpathModifications
        return runConfiguration
    }

    private fun mapToSpringBootRunConfiguration(
        configuration: ApplicationConfiguration, settings: NativeExecutionSettings
    ): SpringBootRunConfiguration? {
        val mainClass = configuration.mainClass ?: return null
        val module = ModuleUtilCore.findModuleForPsiElement(mainClass) ?: return null

        val runConfiguration = SpringBootConfigurationFactory
            .createTemplateConfiguration(settings.project)
            .apply {
                setModule(module)
                setMainClass(mainClass)
                setGeneratedName()
            }
        runConfiguration.vmParameters = configuration.vmParameters
        runConfiguration.envs = configuration.envs
        runConfiguration.programParameters = configuration.programParameters
        runConfiguration.isPassParentEnvs = configuration.isPassParentEnvs
        runConfiguration.alternativeJrePath = configuration.alternativeJrePath
        runConfiguration.classpathModifications = configuration.classpathModifications
        return runConfiguration
    }
}


data class RunConfigurationHolder(
    val useOriginalMainMethod: Boolean = false,
    val runConfiguration: SpringBootRunConfiguration
)