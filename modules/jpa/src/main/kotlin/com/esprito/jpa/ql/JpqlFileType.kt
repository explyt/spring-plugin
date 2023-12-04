package com.esprito.jpa.ql

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

class JpqlFileType private constructor() : LanguageFileType(JpqlLanguage.INSTANCE) {
    override fun getName(): String = "JPA QL"

    override fun getDescription(): String = "JPQL file"

    override fun getDefaultExtension(): String = "jpql"

    override fun getIcon(): Icon = AllIcons.FileTypes.Text

    companion object {
        @JvmField val INSTANCE = JpqlFileType()
    }
}