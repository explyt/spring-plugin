/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.references

import com.explyt.spring.core.completion.properties.ConfigurationPropertiesLoader.Companion.getKeyPsiClass
import com.explyt.spring.core.completion.properties.PropertyType
import com.explyt.spring.core.util.PropertyUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UEnumConstant
import org.jetbrains.uast.toUElement
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping

class YamlKeyMapValueReference(
    element: PsiElement,
    private val module: Module,
    private val propertyKey: String,
    val range: TextRange? = null,
    private val mapKeyRef: Boolean = false
) : PsiReferenceBase.Poly<PsiElement>(element, range, false), MapKeyReference {

    override val isMapKeyReference: Boolean = mapKeyRef

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val property = PropertyUtil.configurationProperty(module, propertyKey) ?: return emptyArray()
        if (!property.isMap()) return emptyArray()

        if (mapKeyRef && PropertyUtil.isScalarMap(property) && (element as? YAMLKeyValue)?.value !is YAMLMapping) {
            val targets = ConfigurationMapKeyTargets.findTargets(
                module, property, PropertyUtil.getMapKeyForLookup(propertyKey, property)
            )
            if (targets.isNotEmpty()) return targets.map(::PsiElementResolveResult).toTypedArray()
        }

        val keyValuePair = PropertyUtil.getKeyValuePair(propertyKey, property)
        if (property.name == propertyKey || (keyValuePair.first.isNotEmpty() && keyValuePair.second.isEmpty()
                    && property.propertyType != PropertyType.ENUM_MAP)
        ) {
            val type = property.sourceType ?: return emptyArray()
            val source = PropertyUtil.findSourceMember(property.name, type, element.project) ?: return emptyArray()
            return PropertyUtil.resolveResults(source)
        }

        if (keyValuePair.first.isNotEmpty() && keyValuePair.second.isEmpty()
            && property.propertyType == PropertyType.ENUM_MAP
        ) {
            val enumPsiClass = getKeyPsiClass(property.type, element.project) ?: return emptyArray()
            val uKeyClass = enumPsiClass.toUElement() as? UClass ?: return emptyArray()
            val psiMember = uKeyClass.uastDeclarations.filterIsInstance<UEnumConstant>()
                .firstOrNull { propertyKey.endsWith(it.name) }
                ?.javaPsi as? PsiMember ?: return emptyArray()
            return PropertyUtil.resolveResults(psiMember)
        }

        if (keyValuePair.second.isEmpty()) return emptyArray()
        val valueType = PropertyUtil.getValueClassNameInMap(property.type) ?: return emptyArray()
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()

        return BoundMemberPath.resolve(module, valueType, keyValuePair.second)
            ?.let { PropertyUtil.resolveResults(it) }
            ?: emptyArray()
    }
}