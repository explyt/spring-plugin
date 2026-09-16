/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.runconfiguration

import com.explyt.spring.core.SpringCoreClasses.SPRING_BOOT_APPLICATION
import com.explyt.spring.core.SpringProperties.SPRING_PROFILES_ACTIVE
import com.intellij.execution.CommonJavaRunConfigurationParameters
import com.intellij.execution.JavaTestConfigurationBase
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.util.Computable
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.PsiMethodUtil
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
                runConfiguration.findMainClassFile()
                runConfiguration.findMainClassFile()?.classes?.toList() ?: emptyList()
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

            is ExternalSystemRunConfiguration -> getExternalSystemRunPsiClasses(runConfiguration)

            else -> emptyList()
        }
    }

    /**
     * A Gradle run configuration never names a main class: the owning module is encoded in the task prefix
     * (`billing:bootRun` runs in Gradle project `:billing`), matched against the external project id every linked
     * module carries. A module-per-source-set layout yields several ids for one Gradle project (`:billing`,
     * `:billing:main`, `:billing:test`), so the `:main` source set wins; any other ambiguity resolves to nothing
     * rather than guessing a module.
     */
    fun findModuleForExternalSystemRunConfiguration(runConfiguration: ExternalSystemRunConfiguration): Module? {
        val project = runConfiguration.project
        val taskName = runConfiguration.settings.taskNames.firstOrNull { it.isNotBlank() } ?: return null
        val modules = ModuleManager.getInstance(project).modules
        if (':' !in taskName) {
            // A configuration created from the Gradle tool window can carry a bare task name with the module
            // directory as the external project path. Source-set modules of one Gradle project share that
            // directory, so only a unique match resolves.
            val externalPath = runConfiguration.settings.externalProjectPath ?: return null
            return modules.singleOrNull { ExternalSystemApiUtil.getExternalProjectPath(it) == externalPath }
        }
        val taskProjectPath = taskName.substringBeforeLast(':')
        val normalizedPath = if (taskProjectPath.startsWith(":")) taskProjectPath else ":$taskProjectPath"
        val matching = modules.filter { module ->
            val projectId = ExternalSystemApiUtil.getExternalProjectId(module) ?: return@filter false
            projectId == normalizedPath || projectId.startsWith("$normalizedPath:")
        }
        return matching.singleOrNull { ExternalSystemApiUtil.getExternalProjectId(it) == "$normalizedPath:main" }
            ?: matching.singleOrNull()
    }

    private fun getExternalSystemRunPsiClasses(runConfiguration: ExternalSystemRunConfiguration): List<PsiClass> {
        val project = runConfiguration.project
        val module = findModuleForExternalSystemRunConfiguration(runConfiguration) ?: return emptyList()
        val mainFiles = PsiShortNamesCache.getInstance(project)
            .getMethodsByName("main", GlobalSearchScope.moduleScope(module))
            .asSequence()
            .filter { PsiMethodUtil.isMainMethod(it) }
            .mapNotNullTo(mutableSetOf()) { it.containingClass?.containingFile?.virtualFile }
        if (mainFiles.isEmpty()) return emptyList()
        val annotationClass = JavaPsiFacade.getInstance(project)
            .findClass(SPRING_BOOT_APPLICATION, GlobalSearchScope.moduleWithLibrariesScope(module))
            ?: return emptyList()
        // Kotlin puts the annotation on the declared class while `main` lives on the file facade, so the entry
        // point is an annotated class whose file declares a `main` — searching classes by the annotation covers
        // both languages without unwrapping light classes.
        val bootClasses = AnnotatedElementsSearch
            .searchPsiClasses(annotationClass, GlobalSearchScope.moduleScope(module))
            .filter { it.containingFile?.virtualFile in mainFiles }
        // A task name identifies a module, not an entry point: several boot applications in one module cannot
        // be told apart, so the link must not guess one.
        return bootClasses.singleOrNull()?.let { listOf(it) } ?: emptyList()
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
            // This platform line has no `mainClassName`; the entry point is `runClass`, which is the raw
            // options accessor here and resolves no PSI. KotlinRunConfiguration is itself a
            // CommonJavaRunConfigurationParameters, so this branch reads exactly what the generic branch
            // below would. It is kept only to mirror the newer lines, where the two differ and the
            // distinction is load-bearing.
            is KotlinRunConfiguration -> runConfiguration.runClass
            is ApplicationConfiguration -> runConfiguration.mainClassName
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