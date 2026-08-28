/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.langinjection

import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.web.SpringWebClasses.REQUEST_MAPPING
import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.impl.source.tree.injected.changesHandler.contentRange
import org.intellij.lang.regexp.RegExpLanguage
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UastFacade
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.expressions.UInjectionHost
import org.jetbrains.uast.expressions.UStringConcatenationsFacade
import org.jetbrains.uast.toUElementOfExpectedTypes

class PathRegexpInjector : MultiHostInjector {
    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        val uElement = context.toUElementOfExpectedTypes(UAnnotation::class.java) ?: return
        val sourcePsi = uElement.sourcePsi ?: return
        uElement.javaPsi ?: return
        val module = ModuleUtil.findModuleForPsiElement(sourcePsi) ?: return
        if (JavaPsiFacade.getInstance(module.project)
                .findClass(REQUEST_MAPPING, module.moduleWithLibrariesScope) == null
        ) return

        val mahRequestMapping = SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, REQUEST_MAPPING)
        val urlPaths = mahRequestMapping.getAnnotationMemberValues(uElement, setOf("value", "path"))
        for (memberValue in urlPaths) {
            memberValue.evaluateString() ?: continue
            val valueSourcePsi = memberValue.sourcePsi ?: continue
            val valueRange = valueSourcePsi.textRange ?: continue
            // offsets below are relative to the source text of the whole member value expression,
            // which may be a concatenation of several injection hosts
            val urlPath = valueSourcePsi.text ?: continue

            val regexpRanges = SpringWebUtil.NameInBracketsRx.findAll(urlPath)
                .mapNotNull { it.groups["name"] }
                .filter { it.value.contains(":") }
                .mapTo(mutableListOf()) {
                    val regexpStart = it.range.first + it.value.indexOf(":") + 1
                    TextRange(regexpStart, it.range.last + 1)
                }
            if (regexpRanges.isEmpty()) continue

            val flattenExpression = memberValue !is UInjectionHost
            val concatenationsFacade =
                UStringConcatenationsFacade.createFromUExpression(memberValue, flattenExpression) ?: return
            val hostContentRanges = concatenationsFacade.psiLanguageInjectionHosts
                .distinct()
                .mapNotNull { host -> host.absoluteContentRange()?.let { host to it } }
                .toList()
            if (hostContentRanges.isEmpty()) return

            val places = regexpRanges.mapNotNull { regexpRange ->
                val absoluteRange = regexpRange.shiftRight(valueRange.startOffset)
                val (host, _) = hostContentRanges.firstOrNull { (_, content) -> content.contains(absoluteRange) }
                    ?: return@mapNotNull null
                host to absoluteRange.shiftLeft(host.textRange.startOffset)
            }
            if (places.isEmpty()) continue

            registrar.startInjecting(RegExpLanguage.INSTANCE)
            places.forEach { (host, rangeInsideHost) -> registrar.addPlace(null, null, host, rangeInsideHost) }
            registrar.doneInjecting()
            return
        }
    }

    /** Content range of the host in file coordinates, or null if the host cannot be injected into. */
    private fun PsiLanguageInjectionHost.absoluteContentRange(): TextRange? {
        if (!isValid || !isValidHost) return null
        val hostRange = textRange ?: return null
        val valueRange = try {
            contentRange
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (_: Exception) {
            return null
        }
        return valueRange.takeIf { hostRange.contains(it) }
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement>> {
        return UastFacade.getPossiblePsiSourceTypes(UAnnotation::class.java).toList()
    }
}
