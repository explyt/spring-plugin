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

package com.explyt.spring.core.providers.spi

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.SpringProperties
import com.explyt.util.ExplytPsiUtil.isAbstract
import com.explyt.util.ExplytPsiUtil.isPrivate
import com.explyt.util.ExplytPsiUtil.resolvedPsiClass
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement
import java.nio.file.Path

class SpiBeanLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val uMethod = element.toUElement() as? UMethod ?: return
        if (uMethod.isPrivate || uMethod.isStatic || uMethod.isAbstract) return
        ModuleUtilCore.findModuleForPsiElement(element) ?: return
        uMethod.findAnnotation(SpringCoreClasses.BEAN) ?: return
        val resolvedPsiClass = uMethod.returnType?.resolvedPsiClass ?: return
        resolvedPsiClass.takeIf { it.isInterface || it.isAbstract } ?: return
        val qualifiedName = resolvedPsiClass.qualifiedName ?: return

        val project = element.project
        val foundFiles = FilenameIndex.getVirtualFilesByName(qualifiedName, GlobalSearchScope.projectScope(project))

        for (virtualFile in foundFiles) {
            if (virtualFile.parent.name != "services") break
            if (virtualFile.parent.parent.name != SpringProperties.META_INF) break

            val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringSetting)
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTargets(NotNullLazyValue.lazy { getTarget(virtualFile.toNioPath(), project) })
                .setTooltipText(SpringCoreBundle.message("explyt.spring.gutter.spi.tooltip.title.usage"))
                .setPopupTitle(SpringCoreBundle.message("explyt.spring.gutter.spi.popup.title.usage"))
                .setEmptyPopupText(SpringCoreBundle.message("explyt.spring.gutter.notfound.title.choose.property.usage"))

            result += builder.createLineMarkerInfo(element)
        }
    }

    private fun getTarget(path: Path, project: Project): List<PsiElement> {
        val spiPsiFile = VfsUtil.findFile(path, false)?.toPsiFile(project) ?: return emptyList()
        val lines = spiPsiFile.text.split(System.lineSeparator())
        val result: MutableList<PsiElement> = lines.asSequence().filter { !it.startsWith("#") }
            .mapNotNull { JavaPsiFacade.getInstance(project).findClass(it, GlobalSearchScope.projectScope(project)) }
            .toMutableList()
        result.add(spiPsiFile)
        return result
    }
}