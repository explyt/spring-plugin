/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.quickfix

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.inspections.utils.SystemEnvironmentAccessFixUtil.AUTOWIRED_ANNOTATION
import com.explyt.spring.core.inspections.utils.SystemEnvironmentAccessFixUtil.ENVIRONMENT_CLASS
import com.explyt.spring.core.inspections.utils.SystemEnvironmentAccessFixUtil.GET_PROPERTY
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
 * Replaces `System.getenv("X")` / `System.getProperty("X")` with `environment.getProperty("X")`, injecting an
 * `Environment` dependency into the bean when it does not have one yet.
 */
class ReplaceSystemAccessWithEnvironmentFix(callPsi: PsiElement, private val key: String) :
    LocalQuickFixAndIntentionActionOnPsiElement(callPsi) {

    override fun getFamilyName(): String = SpringCoreBundle.message("explyt.spring.inspection.system.env.fix.family")

    override fun getText(): String =
        SpringCoreBundle.message("explyt.spring.inspection.system.env.fix.environment", key)

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement
    ) {
        val uClass = startElement.toUElement()?.getContainingUClass() ?: return
        val psiClass = uClass.javaPsi
        when (val sourcePsi = uClass.sourcePsi) {
            is PsiClass -> {
                val name = environmentMemberName(psiClass) ?: addJavaEnvironment(project, sourcePsi, psiClass)
                replaceCall(project, startElement, name)
            }

            is KtClass -> {
                val name = environmentMemberName(psiClass) ?: addKotlinEnvironment(sourcePsi)
                    ?: return
                startElement.replace(KtPsiFactory(project).createExpression("$name.$GET_PROPERTY(\"$key\")"))
            }
        }
    }

    private fun replaceCall(project: Project, callPsi: PsiElement, environmentName: String) {
        val expression = JavaPsiFacade.getElementFactory(project)
            .createExpressionFromText("$environmentName.$GET_PROPERTY(\"$key\")", callPsi)
        callPsi.replace(expression)
    }

    private fun addJavaEnvironment(project: Project, psiClass: PsiClass, owner: PsiClass): String {
        val factory = JavaPsiFacade.getElementFactory(project)
        val name = uniqueMemberName(owner.fields.mapNotNull { it.name }.toSet(), DEFAULT_ENVIRONMENT_NAME)
        val field = factory.createFieldFromText("@$AUTOWIRED_ANNOTATION private $ENVIRONMENT_CLASS $name;", psiClass)
        val lastField = owner.fields.lastOrNull()
        val added = (if (lastField != null) owner.addAfter(field, lastField) else owner.add(field)) as? PsiField
            ?: return name

        val styleManager = JavaCodeStyleManager.getInstance(project)
        styleManager.shortenClassReferences(added)
        (owner.containingFile as? PsiJavaFile)?.let(styleManager::optimizeImports)
        return name
    }

    private fun addKotlinEnvironment(ktClass: KtClass): String? {
        val existingNames = ktClass.primaryConstructorParameters.mapNotNull { it.name }
            .plus(ktClass.body?.properties.orEmpty().mapNotNull { it.name })
            .toSet()
        val name = uniqueMemberName(existingNames, DEFAULT_ENVIRONMENT_NAME)
        val factory = KtPsiFactory(ktClass.project)

        val added: KtDeclaration = if (ktClass.primaryConstructor == null) {
            ktClass.addDeclaration(
                factory.createProperty("@$AUTOWIRED_ANNOTATION private lateinit var $name: $ENVIRONMENT_CLASS")
            )
        } else {
            val parameterList = ktClass.primaryConstructor?.valueParameterList ?: return null
            parameterList.addParameter(factory.createParameter("private val $name: $ENVIRONMENT_CLASS"))
                ?: return null
        }

        ShortenReferencesFacility.getInstance().shorten(added)
        return name
    }

    private companion object {
        const val DEFAULT_ENVIRONMENT_NAME = "environment"
    }
}
