package com.explyt.spring.web.inspections.quickfix

import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.SpringWebClasses
import com.intellij.codeInsight.intention.AddAnnotationFix
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope


/**
 * This class provides a solution to inspection
 */
class AddPathVariableQuickFix(psiMethod: PsiMethod, private val methodName: String) :
    LocalQuickFixAndIntentionActionOnPsiElement(psiMethod) {

    override fun getFamilyName(): String =
        SpringWebBundle.message("explyt.spring.web.inspection.pathVariable.omittedParameter.fix")

    override fun getText(): String = familyName

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement
    ) {
        val psiMethod = startElement as? PsiMethod ?: return
        val psiManager = PsiManager.getInstance(project)
        val elementFactory = JavaPsiFacade.getInstance(project).elementFactory

        val stringType = PsiType.getJavaLangString(psiManager, GlobalSearchScope.projectScope(project))
        val newArgument = elementFactory.createParameter(methodName, stringType)

        AddAnnotationFix(SpringWebClasses.PATH_VARIABLE, newArgument)
            .invoke(project, editor, file)

        psiMethod.parameterList.add(newArgument)
    }

}