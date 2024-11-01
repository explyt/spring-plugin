package com.explyt.spring.core.completion.properties

import com.intellij.lang.properties.IProperty
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLKeyValue

interface DefinedConfigurationProperty {
    val key: String

    val value: String?

    val psiElement: PsiElement?

    val sourceFile: String
}

class YamlDefinedConfigurationProperty(
    property: YAMLKeyValue,
    override val sourceFile: String
) : DefinedConfigurationProperty {

    private val pointer = SmartPointerManager.createPointer<YAMLKeyValue>(property)

    override val key: String
        get() = psiElement?.let { YAMLUtil.getConfigFullName(it) } ?: ""

    override val value: String?
        get() = psiElement?.valueText

    override val psiElement: YAMLKeyValue?
        get() = pointer.element

    override fun toString(): String {
        return key
    }
}

class PropertyDefinedConfigurationProperty(
    property: IProperty,
    override val sourceFile: String
) : DefinedConfigurationProperty {

    private val pointer = SmartPointerManager.createPointer(property.psiElement)

    override val key: String
        get() = property?.key ?: ""

    override val value: String?
        get() = property?.value

    override val psiElement: PsiElement?
        get() = pointer.element

    private val property: IProperty?
        get() = (psiElement as? IProperty)

}