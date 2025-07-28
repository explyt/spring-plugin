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

package com.explyt.spring.core.externalsystem.utils

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.SpringCoreClasses.SPRING_BOOT_APPLICATION
import com.explyt.spring.core.externalsystem.model.SpringBeanData
import com.explyt.spring.core.runconfiguration.RunConfigurationUtil
import com.explyt.spring.core.runconfiguration.SpringBootRunConfiguration
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.wsl.WslPath
import com.intellij.openapi.externalSystem.dependency.analyzer.DAArtifact
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.idea.base.util.projectScope
import org.jetbrains.kotlin.idea.run.KotlinRunConfiguration
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

const val EXPLYT_AGENT_JAR = "explyt-java-agent"

object NativeBootUtils {

    fun getPsiClassLocation(project: Project, beanData: SpringBeanData): PsiClass? {
        return psiClass(beanData.className, project)
    }

    fun getPsiClassLocation(project: Project, className: String): PsiClass? {
        return psiClass(className, project)
    }

    fun getBeanTypePsiClass(project: Project, beanData: SpringBeanData): PsiClass? {
        val beanClassQualifiedName = beanData.methodType ?: beanData.className
        return psiClass(beanClassQualifiedName, project)
    }

    fun toQualifiedClassName(className: String) = className.replace("$", ".")

    fun findProjectClass(classQualifiedName: String, project: Project): PsiClass? {
        return JavaPsiFacade.getInstance(project).findClass(classQualifiedName, project.projectScope())
    }

    fun psiClass(daArtifact: DAArtifact, project: Project) =
        psiClass(daArtifact.groupId + "." + daArtifact.artifactId, project)

    private fun psiClass(className: String, project: Project): PsiClass? {
        val classNameInner = toQualifiedClassName(className)
        return LibraryClassCache.searchForLibraryClass(project, classNameInner)
            ?: JavaPsiFacade.getInstance(project).findClass(classNameInner, project.projectScope())
    }

    fun getVirtualFile(filePath: String): VirtualFile {
        return LocalFileSystem.getInstance().refreshAndFindFileByPath(filePath)
            ?: throw RuntimeException("Virtual file not found $filePath")
    }

    fun getMainRootFiles(project: Project): Set<VirtualFile> {
        val allConfigurationsList = RunManager.getInstance(project).allConfigurationsList
        val result = mutableSetOf<VirtualFile>()
        for (runConfiguration in allConfigurationsList) {
            if (runConfiguration is SpringBootRunConfiguration) {
                runConfiguration.mainClass?.containingFile?.virtualFile?.let { result += it }
            } else if (runConfiguration is KotlinRunConfiguration) {
                runConfiguration.findMainClassFile()?.containingFile?.virtualFile?.let { result += it }
            }
        }
        return result
    }

    fun isSupportRunConfiguration(runConfiguration: RunConfiguration?): Boolean {
        return runConfiguration?.let { getMainClass(it) != null } ?: false
    }

    fun getMainClass(runConfiguration: RunConfiguration): PsiClass? {
        val psiClassList = RunConfigurationUtil.getRunPsiClass(runConfiguration)
        if (psiClassList.size == 1) return psiClassList.first()
        return psiClassList.find { it.isMetaAnnotatedBy(SPRING_BOOT_APPLICATION) }
    }

    fun getAgentPath(): String {
        val customAgentPath = Registry.stringValue("explyt.spring.agent.path")
        if (customAgentPath.isNotEmpty()) return customAgentPath
        val agentJarPath = PathUtil.getJarPathForClass(com.explyt.spring.boot.bean.reader.Constants::class.java)
        if (SystemInfo.isWindows) {
            val winPath = Path(agentJarPath)
            val windowsAbsolutePath = winPath.absolutePathString()
            val wslDistribution = WslPath.getDistributionByWindowsUncPath(windowsAbsolutePath) ?: return agentJarPath
            return wslDistribution.getWslPath(winPath) ?: agentJarPath
        }
        return agentJarPath
    }

    fun getContextLibPath(): String {
        val agentJarPath = PathUtil.getJarPathForClass(com.explyt.spring.boot.bean.reader.ContextHolder::class.java)
        if (SystemInfo.isWindows) {
            val winPath = Path(agentJarPath)
            val windowsAbsolutePath = winPath.absolutePathString()
            val wslDistribution = WslPath.getDistributionByWindowsUncPath(windowsAbsolutePath) ?: return agentJarPath
            return wslDistribution.getWslPath(winPath) ?: agentJarPath
        }
        return agentJarPath
    }

    fun getConfigurationId(configuration: RunConfigurationBase<*>): String {
        return configuration.type.displayName + ":" + configuration.name
    }
}