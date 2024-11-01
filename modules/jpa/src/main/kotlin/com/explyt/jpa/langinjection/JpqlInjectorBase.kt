package com.explyt.jpa.langinjection

import com.explyt.jpa.ql.JpqlLanguage
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.injected.changesHandler.contentRange
import org.jetbrains.uast.*
import org.jetbrains.uast.expressions.UInjectionHost
import org.jetbrains.uast.expressions.UStringConcatenationsFacade

abstract class JpqlInjectorBase : MultiHostInjector {
    protected abstract fun isValidPlace(uElement: UElement): Boolean

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

        registrar.startInjecting(JpqlLanguage.INSTANCE)

        concatenationsFacade.psiLanguageInjectionHosts
            .forEach { host ->
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
}