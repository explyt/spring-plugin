package com.esprito.spring.core.inspections

import com.esprito.spring.core.completion.properties.DefinedConfigurationProperty
import com.esprito.spring.core.completion.properties.YamlPropertySource
import com.intellij.psi.PsiFile
import org.jetbrains.yaml.psi.YAMLFile

class SpringYamlInspection : SpringBasePropertyInspection() {
    override fun loadFileProperties(file: PsiFile): List<DefinedConfigurationProperty> {
        if (file is YAMLFile) {
            return YamlPropertySource(file).properties
        }
        return emptyList()
    }
}