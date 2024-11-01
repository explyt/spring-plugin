package com.explyt.jpa.ql.psi.impl

import com.explyt.jpa.ql.JpqlLanguage
import com.explyt.jpa.ql.psi.JpqlFile
import com.explyt.jpa.ql.psi.JpqlIdentifier
import com.explyt.jpa.ql.psi.JpqlInputParameterExpression
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil


@Service(Service.Level.PROJECT)
class JpqlElementFactory(
    private val project: Project
) {
    fun createIdentifier(name: String): JpqlIdentifier {
        val file: JpqlFile = createFile("FROM $name")
        return PsiTreeUtil.findChildOfType(file, JpqlIdentifier::class.java, false)
            ?: throw ProcessCanceledException()
    }

    fun createNamedInputParameter(newElementName: String): JpqlInputParameterExpression {
        val file: JpqlFile = createFile("FROM foo WHERE bar > :$newElementName")
        return PsiTreeUtil.findChildOfType(file, JpqlInputParameterExpression::class.java, false)
            ?: throw ProcessCanceledException()
    }

    fun createFile(text: String, eventSystemEnabled: Boolean = false): JpqlFile {
        val name = "dummy.jpql"
        return PsiFileFactory.getInstance(project)
            .createFileFromText(
                name,
                JpqlLanguage.INSTANCE,
                text,
                eventSystemEnabled,
                false
            ) as JpqlFile
    }

    companion object {
        fun getInstance(project: Project): JpqlElementFactory = project.service()
    }
}