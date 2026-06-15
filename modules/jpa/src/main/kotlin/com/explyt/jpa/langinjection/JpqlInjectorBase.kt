/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.jpa.langinjection

import com.explyt.jpa.ql.JpqlLanguage
import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.injected.changesHandler.contentRange
import org.jetbrains.uast.*
import org.jetbrains.uast.expressions.UInjectionHost
import org.jetbrains.uast.expressions.UStringConcatenationsFacade

abstract class JpqlInjectorBase() : MultiHostInjector {
    private val log = logger<JpqlInjectorBase>()

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
        val hosts = concatenationsFacade.psiLanguageInjectionHosts
            .distinct()
            .filter { it.isValid && it.isValidHost }
            .filter { host ->
                try {
                    val range = host.contentRange.shiftLeft(host.textOffset)
                    range.startOffset >= 0 && range.endOffset >= range.startOffset
                } catch (_: Exception) {
                    false
                }
            }
        if (hosts.none()) {
            return
        }
        registrar.startInjecting(language)
        hosts.forEach { host ->
            try {
                if (!host.isValid) {
                    log.warn("[JpqlInjectorBase] Skipping invalid host: $host")
                    return@forEach
                }
                val range = host.contentRange.shiftLeft(host.textOffset)
                val hostRange = host.textRange
                if (range.startOffset < 0 || range.endOffset > hostRange.length || range.endOffset < range.startOffset) {
                    log.warn("[JpqlInjectorBase] Skipping host with invalid range: $host, range: $range, hostRange: $hostRange")
                    return@forEach
                }
                registrar.addPlace(
                    null,
                    null,
                    host,
                    range
                )
            } catch (e: ProcessCanceledException) {
                throw e
            } catch (e: Exception) {
                log.warn("[JpqlInjectorBase] Exception in addPlace", e)
            }
        }
        try {
            registrar.doneInjecting()
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (_: Exception) {
        }
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement>> {
        return UastFacade.getPossiblePsiSourceTypes(
            UInjectionHost::class.java,
            UPolyadicExpression::class.java
        ).toList()
    }

    open fun getSqlLanguage(sourcePsi: PsiElement?): Language? = JpqlLanguage.INSTANCE
}