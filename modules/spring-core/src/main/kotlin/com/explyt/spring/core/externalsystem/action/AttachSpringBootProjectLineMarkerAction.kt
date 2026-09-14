/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.action

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringCoreClasses.SPRING_BOOT_APPLICATION
import com.explyt.spring.core.SpringIcons.SpringExplorer
import com.explyt.spring.core.externalsystem.process.SpringBootOpenProjectProvider
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.explyt.spring.core.runconfiguration.RunConfigurationUtil
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
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiMethodUtil
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.kotlin.idea.run.KotlinRunConfiguration
import org.jetbrains.uast.*
import java.awt.event.MouseEvent

//disable after spring debugger
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

    /**
     * Finds the run configuration that launches the clicked main-class file.
     *
     * The file is resolved to PSI **once**, and each run configuration is then compared by its *stored* main class
     * name. Asking every configuration for its main class instead resolved PSI per configuration, and for the
     * JetBrains Spring Boot configuration that means searching for a main-class candidate through the indexes and
     * jar attributes — repeated for the whole list, on the event dispatch thread that handles the gutter click.
     */
    private fun getRunConfiguration(project: Project, canonicalPath: String): RunConfiguration? {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(canonicalPath) ?: return null
        return findRunConfiguration(project, virtualFile)
    }

    @VisibleForTesting
    fun findRunConfiguration(project: Project, mainClassFile: VirtualFile): RunConfiguration? {
        val mainClassNames = mainClassNamesOf(project, mainClassFile)
        if (mainClassNames.isEmpty()) return null

        val runManager = RunManager.getInstance(project)
        val currentRunConfiguration = runManager.selectedConfiguration?.configuration
        if (currentRunConfiguration != null && launchesAnyOf(currentRunConfiguration, mainClassNames)) {
            return currentRunConfiguration
        }
        return runManager.allConfigurationsList
            .filterIsInstance<SpringBootRunConfiguration>()
            .firstOrNull { launchesAnyOf(it, mainClassNames) }
    }

    /**
     * Every class the file declares, by qualified name. A Kotlin file reports its `...Kt` facade here as well, which
     * is the identity a run configuration stores for a top-level `main()` — the `@SpringBootApplication` class alone
     * would never match one.
     */
    private fun mainClassNamesOf(project: Project, mainClassFile: VirtualFile): Set<String> {
        return runReadActionBlocking {
            val classOwner = PsiManager.getInstance(project).findFile(mainClassFile) as? PsiClassOwner
                ?: return@runReadActionBlocking emptySet()
            classOwner.classes.mapNotNullTo(mutableSetOf()) { it.qualifiedName }
        }
    }

    private fun launchesAnyOf(runConfiguration: RunConfiguration, mainClassNames: Set<String>): Boolean =
        RunConfigurationUtil.getRunClassNameInner(runConfiguration) in mainClassNames

    private fun checkRunConfigurationForClassName(
        runConfiguration: RunConfiguration, qualifiedClassName: String
    ) = when (runConfiguration) {
        is KotlinRunConfiguration -> runConfiguration.runClass == qualifiedClassName
        is ApplicationConfiguration -> runConfiguration.mainClassName == qualifiedClassName
        else -> false
    }
}