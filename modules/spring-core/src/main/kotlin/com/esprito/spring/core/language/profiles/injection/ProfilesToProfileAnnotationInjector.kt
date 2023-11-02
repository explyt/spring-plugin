package com.esprito.spring.core.language.profiles.injection

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.language.profiles.ProfilesLanguage
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.injected.changesHandler.contentRange
import org.jetbrains.uast.*
import org.jetbrains.uast.expressions.UInjectionHost
import org.jetbrains.uast.expressions.UStringConcatenationsFacade

class ProfilesToProfileAnnotationInjector : MultiHostInjector {

    private fun isValidPlace(uElement: UElement): Boolean {
        val uAnnotation = uElement.getParentOfType<UAnnotation>() ?: return false

        if (SpringCoreClasses.PROFILE != uAnnotation.qualifiedName)
            return false

        val valueAttribute = uAnnotation.findAttributeValue("value")

        return uElement.isUastChildOf(valueAttribute)
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

        val concatenationsFacade = if (uElement is UInjectionHost) {
            UStringConcatenationsFacade.createFromUExpression(uElement, false) ?: return
        } else {
            UStringConcatenationsFacade.createFromUExpression(uElement, true) ?: return
        }

        registrar.startInjecting(ProfilesLanguage.INSTANCE)

        concatenationsFacade.uastOperands
            .forEach {
                val host = (it as? UInjectionHost)?.psiLanguageInjectionHost ?: return@forEach

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