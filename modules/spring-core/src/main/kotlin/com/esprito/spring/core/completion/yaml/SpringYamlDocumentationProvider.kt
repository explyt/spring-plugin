package com.esprito.spring.core.completion.yaml

import com.esprito.spring.core.properties.ConfigurationPropertiesDocumentationProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLKeyValue

class SpringYamlDocumentationProvider : ConfigurationPropertiesDocumentationProvider<YAMLKeyValue>() {

    override fun getPropertyFullKey(extractedOriginalElement: YAMLKeyValue): String {
        return YAMLUtil.getConfigFullName(extractedOriginalElement)
    }

    override fun extractOriginalElement(originalElement: PsiElement?): YAMLKeyValue? {
        return originalElement?.parentOfType<YAMLKeyValue>()
    }
}
