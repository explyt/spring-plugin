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

package com.explyt.spring.web.langinjection

import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.web.SpringWebClasses.REQUEST_MAPPING
import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UastFacade
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.toUElementOfExpectedTypes

class PathRegexpInjector : MultiHostInjector {
    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        val uElement = context.toUElementOfExpectedTypes(UAnnotation::class.java) ?: return
        val sourcePsi = uElement.sourcePsi ?: return
        val module = ModuleUtil.findModuleForPsiElement(sourcePsi) ?: return
        if (JavaPsiFacade.getInstance(module.project)
                .findClass(REQUEST_MAPPING, module.moduleWithLibrariesScope) == null
        ) return

        val mahRequestMapping = SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, REQUEST_MAPPING)
        val urlPaths = mahRequestMapping.getAnnotationMemberValues(uElement, setOf("value", "path"))
        for (memberValue in urlPaths) {
            val urlPath = memberValue.evaluateString() ?: continue
            val namesWithRanges = SpringWebUtil.NameInBracketsRx.findAll(urlPath)
                .mapNotNull { it.groups["name"] }
                .mapTo(mutableListOf()) {
                    val range = if (sourcePsi.language == JavaLanguage.INSTANCE) {
                        TextRange(it.range.first + 1, it.range.last + 2)
                    } else {
                        TextRange(it.range.first, it.range.last + 1)
                    }
                    val pathParameterName = it.value.substringBefore(":")
                    (pathParameterName to range)
                }
            namesWithRanges.toString()
        }
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement>> {
        return UastFacade.getPossiblePsiSourceTypes(UAnnotation::class.java).toList()
    }
}