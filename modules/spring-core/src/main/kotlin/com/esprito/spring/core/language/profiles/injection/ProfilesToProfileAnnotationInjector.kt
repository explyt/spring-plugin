package com.esprito.spring.core.language.profiles.injection

import com.esprito.spring.core.SpringCoreClasses.PROFILE
import com.esprito.spring.core.language.profiles.ProfilesLanguage
import com.esprito.spring.core.service.SpringSearchService
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