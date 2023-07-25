package com.esprito.spring.core.providers

import com.esprito.spring.core.util.SpringCoreUtil
import com.intellij.lang.properties.codeInspection.unused.ImplicitPropertyUsageProvider
import com.intellij.lang.properties.psi.Property

class ConfigurationPropertyImplicitUsageProvider : ImplicitPropertyUsageProvider {
    override fun isUsed(property: Property): Boolean {
        return SpringCoreUtil.isConfigurationPropertyFile(property.propertiesFile.containingFile)
    }
}
