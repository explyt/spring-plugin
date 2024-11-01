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

package com.explyt.spring.core.language.profiles.injection

import com.explyt.spring.core.SpringCoreClasses.PROFILE
import com.explyt.spring.core.language.profiles.ProfilesLanguage
import com.explyt.spring.core.service.SpringSearchService
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.injected.changesHandler.contentRange
import org.jetbrains.uast.*
import org.jetbrains.uast.expressions.UInjectionHost
import org.jetbrains.uast.expressions.UStringConcatenationsFacade

class ProfilesToProfileAnnotationInjector : MultiHostInjector {

    private fun isValidPlace(uElement: UElement): Boolean {
        val module = uElement.sourcePsi?.let {
            ModuleUtilCore.findModuleForPsiElement(it)
        } ?: return false

        val uAnnotation = uElement.getParentOfType<UAnnotation>() ?: return false
        val uAnnotationQn = uAnnotation.qualifiedName ?: return false
        if (PROFILE != uAnnotationQn)
            return false
        val metaHolder = SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, PROFILE)

        val valueAttribute = uAnnotation.attributeValues.asSequence()
            .filter {
                metaHolder.isAttributeRelatedWith(
                    uAnnotationQn,
                    it.name ?: "value",
                    PROFILE,
                    setOf("value")
                )
            }
            .map { it.expression }
            .firstOrNull() ?: return false


        return valueAttribute.isInjectionHost() && uElement.isUastChildOf(uAnnotation)
    }

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        val uElement = context.toUElementOfExpectedTypes(
            UInjectionHost::class.java,
            UPolyadicExpression::class.java
        ) ?: return

        if (!isValidPlace(uElement))
            return

        if (uElement.sourcePsi !== context) return
        @Suppress("UnstableApiUsage")
        if (isConcatenation(uElement.uastParent)) return

        val flattenExpression = uElement !is UInjectionHost
        val concatenationsFacade =
            UStringConcatenationsFacade.createFromUExpression(uElement, flattenExpression) ?: return

        registrar.startInjecting(ProfilesLanguage.INSTANCE)

        concatenationsFacade.psiLanguageInjectionHosts
            .forEach { hostPsi ->
                registrar.addPlace(
                    null,
                    null,
                    hostPsi,
                    hostPsi.contentRange.shiftLeft(hostPsi.textOffset)
                )
            }


        registrar.doneInjecting()

    }

    override fun elementsToInjectIn(): List<Class<out PsiElement>> {
        return UastFacade.getPossiblePsiSourceTypes(
            UInjectionHost::class.java,
            UPolyadicExpression::class.java
        ).toList()

    }

}