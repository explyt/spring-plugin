package com.esprito.spring.core.language

import com.esprito.spring.core.SpringIcons
import com.intellij.lang.properties.PropertiesLanguage
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon


class FactoriesFileType : LanguageFileType(PropertiesLanguage.INSTANCE) {
    override fun getName(): String {
        return "Factories Configuration"
    }

    override fun getDescription(): String {
        return "Factories configuration properties"
    }

    override fun getDefaultExtension(): String {
        return "factories;imports"
    }

    override fun getIcon(): Icon {
        return SpringIcons.SpringFactories
    }
}