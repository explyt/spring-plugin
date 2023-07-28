package com.esprito.spring.core.completion.yaml

import com.esprito.spring.core.properties.ConfigurationPropertyKeyReference
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLKeyValue

class SpringYamlConfigurationPropertiesReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(YAMLKeyValue::class.java), SpringYamlConfigurationPropertiesKeyReferenceProvider())
    }
}

class SpringYamlConfigurationPropertiesKeyReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val yamlKeyValue = element as? YAMLKeyValue ?: return PsiReference.EMPTY_ARRAY
        val propertyKey = YAMLUtil.getConfigFullName(yamlKeyValue)
        return arrayOf(object : ConfigurationPropertyKeyReference(yamlKeyValue, propertyKey) {
            init {
                rangeInElement = TextRange.allOf(yamlKeyValue.keyText)
                    .shiftRight(ElementManipulators.getOffsetInElement(yamlKeyValue))
            }
        })
    }

}
