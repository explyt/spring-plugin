/*
 * Copyright © 2025 Explyt Ltd
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

package com.explyt.spring.core.debug

import com.explyt.spring.boot.bean.reader.Constants.DEBUG_PROGRAM_PARAM
import com.explyt.spring.core.externalsystem.utils.EXPLYT_AGENT_JAR
import com.explyt.spring.core.externalsystem.utils.NativeBootUtils
import com.explyt.spring.core.externalsystem.utils.NativeBootUtils.getConfigurationId
import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.debugger.impl.GenericDebuggerRunnerSettings
import com.intellij.execution.JavaRunConfigurationBase
import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.ModuleBasedConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.java.library.JavaLibraryUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import kotlin.io.path.Path


const val EXPLYT_SPRING_HOLDER_MARKER_CLASS = "com.explyt.spring.boot.bean.reader.ContextHolder"

class SpringDebuggerRunConfigurationExtension : RunConfigurationExtension() {
    override fun <T : RunConfigurationBase<*>?> updateJavaParameters(
        configuration: T & Any, javaParameters: JavaParameters, runnerSettings: RunnerSettings?
    ) {
        if (runnerSettings !is GenericDebuggerRunnerSettings) return

        val module = getModule(configuration)?.takeIf { isSpringModule(it) } ?: return
        if (javaParameters.vmParametersList.parametersString.contains(EXPLYT_AGENT_JAR)) return

        if (configuration is ExternalSystemRunConfiguration) {
            configuration.settings.env.put(DEBUG_PROGRAM_PARAM, getConfigurationId(configuration))
        } else {
            val javaAgentEscaping = NativeBootUtils.getJavaAgentParam()
            javaParameters.vmParametersList.add(javaAgentEscaping)
            javaParameters.vmParametersList.addProperty(DEBUG_PROGRAM_PARAM, getConfigurationId(configuration))
        }

        ApplicationManager.getApplication().executeOnPooledThread { addExplytContextLibrary(module) }
    }

    override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean {
        if (!SpringToolRunConfigurationsSettingsState.getInstance().isDebugMode) return false
        return configuration is ApplicationConfiguration
                || configuration is JavaRunConfigurationBase
                || configuration is ExternalSystemRunConfiguration
    }

    private fun getModule(runConfiguration: RunConfigurationBase<*>): ModuleHolder? {
        return when (runConfiguration) {
            is ApplicationConfiguration -> runConfiguration.modules.map { ModuleHolder(it) }.firstOrNull()
            is ModuleBasedConfiguration<*, *> -> runConfiguration.modules.map { ModuleHolder(it) }.firstOrNull()
            is ExternalSystemRunConfiguration -> findModuleForExternalSystem(runConfiguration)
            else -> null
        }
    }

    private fun findModuleForExternalSystem(configuration: ExternalSystemRunConfiguration): ModuleHolder? {
        val externalProjectPath = configuration.settings.externalProjectPath
        val file = VfsUtil.findFile(Path(externalProjectPath), false) ?: return null
        val module = ModuleUtilCore.findModuleForFile(file, configuration.project) ?: return null
        val modules = ModuleManager.getInstance(configuration.project).modules
        val subModules = modules.filter { it.name.startsWith(module.name + ".") }
            .takeIf { it.isNotEmpty() } ?: return ModuleHolder(module)
        val mainModule = subModules.find { it.name == module.name + ".main" } ?: return ModuleHolder(module)
        val testModule = subModules.find { it.name == module.name + ".test" }
        return ModuleHolder(mainModule, testModule)
    }

    private fun isSpringModule(holder: ModuleHolder): Boolean = SpringCoreUtil.isSpringModule(holder.mainModule)

    private fun addExplytContextLibrary(holder: ModuleHolder) {
        addExplytContextLibrary(holder.mainModule)
        holder.testModule?.let { addExplytContextLibrary(it) }
    }

    private fun addExplytContextLibrary(module: Module) {
        val libraryRoot = Path(NativeBootUtils.getContextLibPath())
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(libraryRoot.toFile())
        val urlForLibraryRoot = VfsUtil.getUrlForLibraryRoot(libraryRoot)

        val hasLibraryClass = runReadAction {
            JavaLibraryUtil.hasLibraryClass(module, EXPLYT_SPRING_HOLDER_MARKER_CLASS)
        }
        if (hasLibraryClass) return
        val librariesUrls = listOf(urlForLibraryRoot)
        ModuleRootModificationUtil.addModuleLibrary(
            module, "explyt-spring-context",
            librariesUrls, emptyList(),
            DependencyScope.PROVIDED
        )
    }
}

private data class ModuleHolder(val mainModule: Module, val testModule: Module? = null)