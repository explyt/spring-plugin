package com.explyt.jpa.ql.psi

import com.explyt.jpa.ql.JpqlFileType
import com.explyt.jpa.ql.JpqlLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

class JpqlFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, JpqlLanguage.INSTANCE) {
    override fun getFileType(): FileType {
        return JpqlFileType.INSTANCE
    }

    override fun toString(): String {
        return "JPA QL File"
    }
}