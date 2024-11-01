package com.explyt.spring.core.autoconfigure

import com.explyt.spring.core.autoconfigure.language.AutoConfigurationImportsFileType
import com.explyt.spring.core.autoconfigure.language.FactoriesFileType
import com.intellij.lang.properties.codeInspection.unused.ImplicitPropertyUsageProvider
import com.intellij.lang.properties.psi.Property
import com.intellij.openapi.fileTypes.FileTypeRegistry

class AutoConfigureImplicitPropertyUsageProvider : ImplicitPropertyUsageProvider {
    override fun isUsed(property: Property) =
        FileTypeRegistry.getInstance().isFileOfType(property.propertiesFile.virtualFile, FactoriesFileType)
                || FileTypeRegistry.getInstance()
            .isFileOfType(property.propertiesFile.virtualFile, AutoConfigurationImportsFileType)
}