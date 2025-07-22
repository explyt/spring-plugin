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

package com.explyt.spring.core.externalsystem.action

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringCoreClasses.SPRING_BOOT_APPLICATION
import com.explyt.spring.core.SpringIcons.SpringExplorer
import com.explyt.spring.core.externalsystem.process.SpringBootOpenProjectProvider
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.explyt.spring.core.externalsystem.utils.NativeBootUtils
import com.explyt.spring.core.runconfiguration.SpringBootRunConfiguration
import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isPublic
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.execution.RunManager
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiMethodUtil
import org.jetbrains.kotlin.idea.run.KotlinRunConfiguration
import org.jetbrains.uast.*
import java.awt.event.MouseEvent

class AttachSpringProjectLineMarkerContributor : LineMarkerProviderDescriptor() {

    override fun getName(): String? = null

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        return if (SpringToolRunConfigurationsSettingsState.getInstance().isJavaAgentMode) {
            springProject(element)
        } else {
            springBootProject(element)
        }
    }

    private fun springProject(element: PsiElement): LineMarkerInfo<PsiElement>? {
        LibraryClassCache.searchForLibraryClass(element.project, SpringCoreClasses.COMPONENT) ?: return null
        val uMethod = element.toUElement() as? UMethod ?: return null
        val javaPsi = uMethod.javaPsi
        if (!isMainMethod(javaPsi)) return null
        if (!isSpringMainMethod(uMethod)) return null
        val virtualFile = javaPsi.containingFile?.virtualFile ?: return null
        val canonicalPath = virtualFile.canonicalPath ?: return null
        if (ProjectRootManager.getInstance(element.project).fileIndex.isInTestSourceContent(virtualFile)) return null
        val containingClass = javaPsi.containingClass?.qualifiedName ?: return null
        val sourcePsi = uMethod.uastAnchor?.sourcePsi ?: return null
        return LineMarkerInfo(
            sourcePsi,
            sourcePsi.textRange,
            SpringExplorer,
            { getTooltipText(element.project, canonicalPath) },
            AttachProjectIconGutterHandler(canonicalPath, containingClass),
            GutterIconRenderer.Alignment.LEFT,
            { getTooltipText(element.project, canonicalPath) },
        )
    }

    private fun isMainMethod(javaPsi: PsiMethod): Boolean {
        val mainMethod = PsiMethodUtil.isMainMethod(javaPsi)
        if (!mainMethod) {
            return javaPsi.name == "main" && javaPsi.isPublic
                    && javaPsi.containingClass?.qualifiedName?.contains(".Companion") == true
        }
        return mainMethod
    }

    private fun springBootProject(element: PsiElement): LineMarkerInfo<PsiElement>? {
        val uAnnotation = element.toUElement() as? UAnnotation ?: return null
        val uClass = uAnnotation.uastParent as? UClass ?: return null
        if (!isSupport(uAnnotation, uClass)) return null
        val sourcePsi = uAnnotation.uastAnchor?.sourcePsi ?: return null
        val canonicalPath = uClass.javaPsi.containingFile?.virtualFile?.canonicalPath ?: return null
        return LineMarkerInfo(
            sourcePsi,
            sourcePsi.textRange,
            SpringExplorer,
            { getTooltipText(element.project, canonicalPath) },
            AttachProjectIconGutterHandler(canonicalPath, null),
            GutterIconRenderer.Alignment.LEFT,
            { getTooltipText(element.project, canonicalPath) },
        )
    }

    private fun isSupport(uAnnotation: UAnnotation, uClass: UClass): Boolean {
        if (uAnnotation.qualifiedName == SPRING_BOOT_APPLICATION) return true
        val project = uClass.javaPsi.project
        if (!uClass.javaPsi.isMetaAnnotatedBy(SPRING_BOOT_APPLICATION)) return false
        val module = ModuleUtilCore.findModuleForPsiElement(uClass.javaPsi) ?: return false
        if (JavaPsiFacade.getInstance(project)
                .findClass(SPRING_BOOT_APPLICATION, module.moduleWithLibrariesScope) == null
        ) return false
        val holder = SpringSearchService.getInstance(project).getMetaAnnotations(module, SPRING_BOOT_APPLICATION)
        return holder.contains(uAnnotation)
    }

    private fun isSpringMainMethod(uMethod: UMethod): Boolean {
        if (uMethod.getContainingUClass()?.javaPsi?.isMetaAnnotatedBy(SPRING_BOOT_APPLICATION) == true) return true

        val bodyText = uMethod.uastBody?.sourcePsi?.text ?: return false
        return bodyText.contains("runApplication") || bodyText.contains("run(")
                || bodyText.contains("ApplicationContext")
    }

    companion object {
        fun isExist(project: Project, canonicalPath: String) =
            ExternalSystemApiUtil.getSettings(project, SYSTEM_ID).getLinkedProjectSettings(canonicalPath) != null

        private fun getTooltipText(project: Project, canonicalPath: String): String {
            return if (isExist(project, canonicalPath)
            ) message("explyt.external.project.link.line.marker.refresh.text") else
                message("explyt.external.project.link.line.marker.text")
        }
    }
}

class AttachProjectIconGutterHandler(private val canonicalPath: String, private val qualifiedClassName: String?) :
    GutterIconNavigationHandler<PsiElement> {

    override fun navigate(e: MouseEvent?, elt: PsiElement?) {
        val project = elt?.project ?: return
        val exist = AttachSpringProjectLineMarkerContributor.isExist(project, canonicalPath)
        if (exist) {
            ExternalProjectsManagerImpl.getInstance(project).runWhenInitialized {
                ExternalSystemUtil.refreshProject(
                    canonicalPath, ImportSpecBuilder(project, SYSTEM_ID)
                )
            }
        } else {
            val runConfiguration = qualifiedClassName?.let { getRunConfigurationForClass(project, it) }
                ?: getRunConfiguration(project, canonicalPath)
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(canonicalPath) ?: return
            SpringBootOpenProjectProvider().linkToExistingProject(
                virtualFile,
                runConfiguration,
                qualifiedClassName,
                project
            )
        }
    }

    private fun getRunConfigurationForClass(project: Project, qualifiedClassName: String): RunConfiguration? {
        val currentRunConfiguration = RunManager.getInstance(project).selectedConfiguration?.configuration
        val selectedRunConfiguration = currentRunConfiguration
            ?.takeIf { checkRunConfigurationForClassName(currentRunConfiguration, qualifiedClassName) }
        if (selectedRunConfiguration != null) return selectedRunConfiguration
        return RunManager.getInstance(project).allConfigurationsList
            .filterIsInstance<SpringBootRunConfiguration>()
            .firstOrNull { checkRunConfigurationForClassName(it, qualifiedClassName) }
            ?: RunManager.getInstance(project).allConfigurationsList.firstOrNull {
                checkRunConfigurationForClassName(it, qualifiedClassName)
            }
    }

    private fun getRunConfiguration(project: Project, canonicalPath: String): RunConfiguration? {
        val currentRunConfiguration = RunManager.getInstance(project).selectedConfiguration?.configuration
        if (currentRunConfiguration != null) {
            if (checkRunConfigurationForRun(currentRunConfiguration, canonicalPath)) return currentRunConfiguration
        }
        for (runConfiguration in RunManager.getInstance(project).allConfigurationsList) {
            if (runConfiguration !is SpringBootRunConfiguration) continue
            if (checkRunConfigurationForRun(runConfiguration, canonicalPath)) return currentRunConfiguration
        }
        return null
    }

    private fun checkRunConfigurationForRun(
        runConfiguration: RunConfiguration, canonicalPath: String
    ): Boolean {
        val mainClass = NativeBootUtils.getMainClass(runConfiguration)
        if (mainClass?.containingFile?.virtualFile?.canonicalPath == canonicalPath) {
            return true
        }
        return false
    }

    private fun checkRunConfigurationForClassName(
        runConfiguration: RunConfiguration, qualifiedClassName: String
    ) = when (runConfiguration) {
        is KotlinRunConfiguration -> runConfiguration.runClass == qualifiedClassName
        is ApplicationConfiguration -> runConfiguration.mainClassName == qualifiedClassName
        else -> false
    }
}