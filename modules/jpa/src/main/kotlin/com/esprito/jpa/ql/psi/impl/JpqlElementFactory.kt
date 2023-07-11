package com.esprito.jpa.ql.psi.impl

import com.esprito.jpa.ql.JpqlFileType
import com.esprito.jpa.ql.psi.JpqlFile
import com.esprito.jpa.ql.psi.JpqlIdentifier
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil


@Service(Service.Level.PROJECT)
class JpqlElementFactory(
    private val project: Project
) {
    fun createIdentifier(name: String): JpqlIdentifier {
        val file: JpqlFile = createFile("FROM $name")
        return PsiTreeUtil.findChildOfType(file, JpqlIdentifier::class.java, false)!!
    }

    fun createFile(text: String): JpqlFile {
        val name = "dummy.jpql"
        return PsiFileFactory.getInstance(project)
            .createFileFromText(name, JpqlFileType.INSTANCE, text) as JpqlFile
    }
}