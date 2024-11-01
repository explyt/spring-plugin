/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

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