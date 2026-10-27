/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.completion.properties

import com.intellij.lang.properties.IProperty
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLSequence

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

    /**
     * `null` for a sequence, which has no scalar value: `YAMLKeyValue.getValueText` answers a synthetic
     * `<sequence:1f2e3d4c>` marker for one. Handing that to the value checks would report the marker as an
     * unresolved class, bean or resource, so a list-valued key reports `null` — the same contract every consumer
     * already handles for a key without a value.
     */
    override val value: String?
        get() = psiElement?.takeIf { it.value !is YAMLSequence }?.valueText

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