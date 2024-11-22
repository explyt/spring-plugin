package com.explyt.spring.core.language.profiles.psi

import com.explyt.spring.core.language.profiles.ProfilesFileType
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil

object ProfilesElementFactory {

    @JvmStatic
    fun createValue(project: Project, name: String): PsiElement {
        val file = createFile(project, name)
        return PsiTreeUtil.findChildOfType(file, ProfilesProfile::class.java, false)
            ?.value ?: throw ProcessCanceledException()
    }

    @JvmStatic
    fun createFile(project: Project, text: String): ProfilesFile {
        val factory = PsiFileFactory.getInstance(project)
        return factory.createFileFromText("dummy.profiles", ProfilesFileType.INSTANCE, text) as ProfilesFile
    }

}