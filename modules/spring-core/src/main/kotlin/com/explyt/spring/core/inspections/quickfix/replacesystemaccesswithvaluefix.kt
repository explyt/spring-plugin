/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.quickfix

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.inspections.utils.SystemEnvironmentAccessFixUtil.deriveMemberName
import com.explyt.spring.core.inspections.utils.SystemEnvironmentAccessFixUtil.environmentMemberName
import com.explyt.spring.core.inspections.utils.SystemEnvironmentAccessFixUtil.uniqueMemberName
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import org.jetbrains.kotlin.idea.base.codeInsight.ShortenReferencesFacility
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.toUElement

/**
 * Replaces `System.getenv("X")` / `System.getProperty("X")` with a `@Value("${X}")` dependency.
 *
 * Java gets a field, Kotlin a primary-constructor parameter when there is a primary constructor and a `@field:Value`
 * property otherwise, which is how the value is injected in each language.
 */
class ReplaceSystemAccessWithValueFix(callPsi: PsiElement, private val key: String) :
    LocalQuickFixAndIntentionActionOnPsiElement(callPsi) {

    override fun getFamilyName(): String = SpringCoreBundle.message("explyt.spring.inspection.system.env.fix.family")

    override fun getText(): String = SpringCoreBundle.message("explyt.spring.inspection.system.env.fix.value", key)

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement
    ) {
        val uClass = startElement.toUElement()?.getContainingUClass() ?: return
        when (val sourcePsi = uClass.sourcePsi) {
            is PsiClass -> replaceInJava(project, sourcePsi, startElement)
            is KtClass -> replaceInKotlin(project, sourcePsi, startElement)
        }
    }

    private fun replaceInJava(project: Project, psiClass: PsiClass, callPsi: PsiElement) {
        val factory = JavaPsiFacade.getElementFactory(project)
        val name = uniqueMemberName(psiClass.fields.mapNotNull { it.name }.toSet(), deriveMemberName(key))
        val field = factory.createFieldFromText("@$VALUE_ANNOTATION(\"\${$key}\") private String $name;", psiClass)
        val lastField = psiClass.fields.lastOrNull()
        val added = (if (lastField != null) psiClass.addAfter(field, lastField) else psiClass.add(field)) as? PsiField
            ?: return

        callPsi.replace(factory.createExpressionFromText(name, callPsi))

        val styleManager = JavaCodeStyleManager.getInstance(project)
        styleManager.shortenClassReferences(added)
        (psiClass.containingFile as? PsiJavaFile)?.let(styleManager::optimizeImports)
    }

    private fun replaceInKotlin(project: Project, ktClass: KtClass, callPsi: PsiElement) {
        val factory = KtPsiFactory(project)
        val existingNames = ktClass.primaryConstructorParameters.mapNotNull { it.name }
            .plus(ktClass.body?.properties.orEmpty().mapNotNull { it.name })
            .toSet()
        val name = uniqueMemberName(existingNames, deriveMemberName(key))

        val added: KtDeclaration = if (ktClass.primaryConstructor == null) {
            ktClass.addDeclaration(
                factory.createProperty("@field:$VALUE_ANNOTATION(\"\${$key}\") private val $name: String = \"\"")
            )
        } else {
            val parameterList = ktClass.primaryConstructor?.valueParameterList ?: return
            parameterList.addParameter(
                factory.createParameter("@$VALUE_ANNOTATION(\"\${$key}\") private val $name: String")
            ) ?: return
        }

        callPsi.replace(factory.createExpression(name))
        ShortenReferencesFacility.getInstance().shorten(added)
    }

    private companion object {
        const val VALUE_ANNOTATION = "org.springframework.beans.factory.annotation.Value"
    }
}
