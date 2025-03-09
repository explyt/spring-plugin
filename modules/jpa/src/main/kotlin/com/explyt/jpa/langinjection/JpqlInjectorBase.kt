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

package com.explyt.jpa.langinjection

import com.explyt.jpa.ql.JpqlLanguage
import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.injected.changesHandler.contentRange
import org.jetbrains.uast.*
import org.jetbrains.uast.expressions.UInjectionHost
import org.jetbrains.uast.expressions.UStringConcatenationsFacade

abstract class JpqlInjectorBase() : MultiHostInjector {
    protected abstract fun isValidPlace(uElement: UElement): Boolean

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        val uElement = context.toUElementOfExpectedTypes(
            UInjectionHost::class.java,
            UPolyadicExpression::class.java
        ) ?: return
        val language = getSqlLanguage(uElement.sourcePsi) ?: return

        if (!isValidPlace(uElement))
            return

        if (uElement.sourcePsi !== context) return
        @Suppress("UnstableApiUsage")
        if (isConcatenation(uElement.uastParent)) return

        val flattenExpression = uElement !is UInjectionHost
        val concatenationsFacade =
            UStringConcatenationsFacade.createFromUExpression(uElement, flattenExpression) ?: return
        val hosts = concatenationsFacade.psiLanguageInjectionHosts.toList()
        if (hosts.isEmpty()) {
            return
        }
        registrar.startInjecting(language)
        hosts.forEach { host ->
            registrar.addPlace(
                null,
                null,
                host,
                host.contentRange.shiftLeft(host.textOffset)
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

    open fun getSqlLanguage(sourcePsi: PsiElement?): Language? = JpqlLanguage.INSTANCE
}