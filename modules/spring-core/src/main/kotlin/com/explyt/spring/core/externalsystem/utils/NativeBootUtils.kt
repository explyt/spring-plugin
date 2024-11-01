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
import com.explyt.spring.core.service.SpringSearchService
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.externalSystem.dependency.analyzer.DAArtifact
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.base.util.projectScope
import org.jetbrains.kotlin.idea.run.KotlinRunConfiguration
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElement

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

    fun getMainClass(psiClass: PsiClass?): PsiClass? {
        psiClass ?: return null
        return when (psiClass.language) {
            JavaLanguage.INSTANCE -> psiClass

            KotlinLanguage.INSTANCE -> {
                if (psiClass is KtLightClassForFacade) {
                    return getKotlinMainBootClass(psiClass)
                }
                return null
            }

            else -> null
        }
    }

    fun getMainClass(runConfiguration: RunConfiguration): PsiClass? {
        val psiClassList = RunConfigurationUtil.getRunPsiClass(runConfiguration)
        if (psiClassList.size == 1) return psiClassList.first()
        return psiClassList.find { AnnotationUtil.isAnnotated(it, SPRING_BOOT_APPLICATION, 0) }
    }

    private fun getKotlinMainBootClass(psiClass: KtLightClassForFacade): PsiClass? {
        val psiClasses = psiClass.files.flatMap { it.classes.toList() }
        return if (psiClasses.size == 1) {
            psiClasses[0]
        } else {
            findMainSpringBootClass(psiClasses)
        }
    }

    private fun findMainSpringBootClass(psiClasses: List<PsiClass>): PsiClass? {
        if (psiClasses.isEmpty()) return null
        val module = ModuleUtilCore.findModuleForPsiElement(psiClasses.first()) ?: return null
        val metaHolder = SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, SPRING_BOOT_APPLICATION)

        for (psiClass in psiClasses) {
            val uClass = psiClass.toUElement() as? UClass ?: continue
            val isSpringBootApp = uClass.uAnnotations.asSequence()
                .mapNotNull { it.javaPsi }
                .any { metaHolder.contains(it) }
            if (isSpringBootApp) return psiClass
        }
        return null
    }

}