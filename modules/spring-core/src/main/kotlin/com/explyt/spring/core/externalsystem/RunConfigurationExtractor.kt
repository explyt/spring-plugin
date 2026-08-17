/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem

import com.explyt.spring.core.externalsystem.setting.NativeExecutionSettings
import com.explyt.spring.core.externalsystem.setting.RunConfigurationType
import com.explyt.spring.core.runconfiguration.SpringBootConfigurationFactory
import com.explyt.spring.core.runconfiguration.SpringBootRunConfiguration
import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.intellij.execution.RunManager
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiMethodUtil
import org.jetbrains.kotlin.idea.run.KotlinRunConfiguration
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElement
import kotlin.io.path.Path

object RunConfigurationExtractor {

    fun findRunConfiguration(projectPath: String, settings: NativeExecutionSettings?): RunConfigurationHolder? {
        settings ?: return null
        val allConfigurationsList = RunManager.getInstance(settings.project).allConfigurationsList
        val isJavaAgent = SpringToolRunConfigurationsSettingsState.getInstance().isJavaAgentMode
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
        val runConfigByName = settings.runConfigurationName?.let { name ->
            allConfigurationsList.find { it is SpringBootRunConfiguration && it.name == name }
        }
        // If the stored name doesn't match (e.g. user renamed/replaced the run config),
        // fall back to matching a SpringBootRunConfiguration by main-class file path.
        // Configurations sharing a main class can still differ in profiles, VM args or environment, so when the
        // path match is ambiguous the user's current selection is the only honest tie-breaker.
        val matchingByPath = allConfigurationsList.filter { checkRunConfiguration(it, projectPath) }
        val runConfig = runConfigByName
            ?: matchingByPath.singleOrNull()
            ?: RunManager.getInstance(settings.project).selectedConfiguration?.configuration
                ?.takeIf { selected -> matchingByPath.any { it === selected } }
        if (runConfig is SpringBootRunConfiguration) {
            if (isJavaAgent) {
                return RunConfigurationHolder(agentRunConfiguration = runConfig.clone())
            }
            return RunConfigurationHolder(runConfig.clone() as SpringBootRunConfiguration)
        }
        // A stored-but-missing name means a dangling link: fabricating a configuration would silently sync with
        // default profiles/env and hide the real cause, so report it instead (SpringBeanNativeResolver.nothingException).
        if (settings.runConfigurationName != null) return null
        return createDefaultRunConfiguration(settings)
            ?.let { if (isJavaAgent) RunConfigurationHolder(agentRunConfiguration = it) else RunConfigurationHolder(it) }
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
        val externalProjectMainFilePath = settings.externalProjectMainFilePath ?: return null
        val virtualFile = VfsUtil.findFile(Path(externalProjectMainFilePath), false) ?: return null
        val module = ModuleUtilCore.findModuleForFile(virtualFile, settings.project) ?: return null
        // The stored qualified name is the `@SpringBootApplication` class, which for a Kotlin top-level `main()` is
        // not the launchable class: running it fails with "Main method not found in class ...". Resolve the class that
        // actually declares `main` from the linked file and keep the stored name only as a last resort.
        val mainClassName = findRunnableMainClassName(virtualFile, settings.project)
            ?: settings.qualifiedMainClassName
            ?: return null

        val runConfiguration = SpringBootConfigurationFactory
            .createTemplateConfiguration(settings.project)
            .apply {
                setModule(module)
                this.mainClassName = mainClassName
                setGeneratedName()
            }
        return runConfiguration
    }

    /**
     * `KtFile` is a [PsiClassOwner] too, so a single branch covers Java and Kotlin: for Kotlin the reported classes
     * include the `...Kt` file facade that actually holds a top-level `main()`.
     */
    private fun findRunnableMainClassName(virtualFile: VirtualFile, project: Project): String? {
        val classOwner = PsiManager.getInstance(project).findFile(virtualFile) as? PsiClassOwner ?: return null
        return classOwner.classes.firstOrNull { PsiMethodUtil.hasMainMethod(it) }?.qualifiedName
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