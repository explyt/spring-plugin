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

import com.explyt.spring.core.externalsystem.setting.NativeExecutionSettings
import com.explyt.spring.core.externalsystem.setting.RunConfigurationType
import com.explyt.spring.core.runconfiguration.SpringBootConfigurationFactory
import com.explyt.spring.core.runconfiguration.SpringBootRunConfiguration
import com.intellij.execution.RunManager
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.kotlin.idea.run.KotlinRunConfiguration
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElement
import kotlin.io.path.Path

object RunConfigurationExtractor {

    fun findRunConfiguration(projectPath: String, settings: NativeExecutionSettings?): RunConfigurationHolder? {
        settings ?: return null
        val allConfigurationsList = RunManager.getInstance(settings.project).allConfigurationsList
        val isJavaAgent = Registry.`is`("explyt.spring.native.javaagent")
        if (settings.runConfigurationType == RunConfigurationType.KOTLIN) {
            val runConfig = allConfigurationsList
                .filterIsInstance<KotlinRunConfiguration>()
                .find { it.name == settings.runConfigurationName }
            if (runConfig != null) {
                if (isJavaAgent) {
                    return RunConfigurationHolder(agentRunConfiguration = runConfig.clone())
                }
                val runConfiguration = mapToSpringBootRunConfiguration(runConfig, settings) ?: return null
                return RunConfigurationHolder(runConfiguration)
            }
        }
        if (settings.runConfigurationType == RunConfigurationType.APPLICATION) {
            val runConfig = allConfigurationsList
                .filterIsInstance<ApplicationConfiguration>()
                .find { it !is SpringBootRunConfiguration && it.name == settings.runConfigurationName }
            if (runConfig != null) {
                if (isJavaAgent) {
                    return RunConfigurationHolder(agentRunConfiguration = runConfig.clone())
                }
                val runConfiguration = mapToSpringBootRunConfiguration(runConfig, settings) ?: return null
                return RunConfigurationHolder(runConfiguration)
            }
        }
        val runConfig = if (settings.runConfigurationName != null) {
            allConfigurationsList.find { it is SpringBootRunConfiguration && it.name == settings.runConfigurationName }
        } else {
            allConfigurationsList.find { checkRunConfiguration(it, projectPath) }
        }
        if (runConfig is SpringBootRunConfiguration) {
            if (isJavaAgent) {
                return RunConfigurationHolder(agentRunConfiguration = runConfig.clone())
            }
            return RunConfigurationHolder(runConfig.clone() as SpringBootRunConfiguration)
        }
        return createDefaultRunConfiguration(settings)?.let { RunConfigurationHolder(agentRunConfiguration = it) }
    }

    private fun checkRunConfiguration(runConfiguration: RunConfiguration, projectPath: String): Boolean {
        return if (runConfiguration is SpringBootRunConfiguration) {
            runConfiguration.mainClass?.containingFile?.virtualFile?.canonicalPath == projectPath
        } else false
    }

    private fun getKotlinUClass(kotlinRunConfiguration: KotlinRunConfiguration): UClass? {
        val mainClassFile = kotlinRunConfiguration.findMainClassFile() ?: return null
        val runClassQualifiedName = kotlinRunConfiguration.runClass ?: return null
        val uFile = mainClassFile.toUElement() as? UFile ?: return null
        return uFile.classes
            .firstOrNull { it.javaPsi.name != null && runClassQualifiedName.endsWith(it.javaPsi.name!!) }
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

    private fun createDefaultRunConfiguration(settings: NativeExecutionSettings): SpringBootRunConfiguration? {
        val qualifiedMainClassName = settings.qualifiedMainClassName ?: return null
        val externalProjectMainFilePath = settings.externalProjectMainFilePath ?: return null
        val virtualFile = VfsUtil.findFile(Path(externalProjectMainFilePath), false) ?: return null
        val module = ModuleUtilCore.findModuleForFile(virtualFile, settings.project) ?: return null

        val runConfiguration = SpringBootConfigurationFactory
            .createTemplateConfiguration(settings.project)
            .apply {
                setModule(module)
                mainClassName = qualifiedMainClassName
                setGeneratedName()
            }
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
    val runConfiguration: SpringBootRunConfiguration? = null,
    val agentRunConfiguration: RunConfiguration? = null
) {
    fun isEmpty() = runConfiguration == null && agentRunConfiguration == null
}