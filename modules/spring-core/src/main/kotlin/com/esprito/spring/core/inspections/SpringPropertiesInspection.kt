package com.esprito.spring.core.inspections

import com.esprito.spring.core.completion.properties.DefinedConfigurationProperty
import com.esprito.spring.core.completion.properties.PropertiesPropertySource
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.psi.PsiFile

class SpringPropertiesInspection : SpringBasePropertyInspection() {
    override fun loadFileProperties(file: PsiFile): List<DefinedConfigurationProperty> {
        if (file is PropertiesFile) {
            return PropertiesPropertySource(file).properties
        }
        return emptyList()
    }
}