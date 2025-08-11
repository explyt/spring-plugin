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

package com.explyt.spring.core.properties.references

import com.explyt.spring.core.completion.properties.ConfigurationPropertiesLoader.Companion.getKeyPsiClass
import com.explyt.spring.core.completion.properties.ConfigurationProperty
import com.explyt.spring.core.util.PropertyUtil
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UEnumConstant
import org.jetbrains.uast.toUElement

class PropertiesKeyMapValueReference(
    element: PsiElement,
    private val propertyKey: String,
    private val property: ConfigurationProperty,
    rangeInElement: TextRange?,
    private val baseMapRef: Boolean = false,
    private val enumMapKeyRef: Boolean = false,
) : PsiReferenceBase.Poly<PsiElement>(element, rangeInElement, true) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return handleMapProperty(element.project)
    }

    private fun handleMapProperty(project: Project): Array<ResolveResult> {
        if (baseMapRef) {
            return baseMapResolve(project)
        }
        if (enumMapKeyRef) {
            val enumPsiClass = getKeyPsiClass(property.type, project) ?: return emptyArray()
            val uKeyClass = enumPsiClass.toUElement() as? UClass ?: return emptyArray()
            val psiMember = uKeyClass.uastDeclarations.filterIsInstance<UEnumConstant>()
                .firstOrNull { propertyKey.endsWith(it.name) }
                ?.javaPsi as? PsiMember ?: return emptyArray()
            return PropertyUtil.resolveResults(psiMember)
        }

        val propertyMapValue = PropertyUtil.getKeyValuePair(propertyKey, property).second
        if (propertyMapValue.isEmpty()) return emptyArray()

        val valueType = PropertyUtil.getValueClassNameInMap(property.type) ?: return emptyArray()
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()

        val methodsTypeByMap = PropertyUtil.getMethodsTypeByMap(module, valueType, propertyMapValue)
        if (methodsTypeByMap.isEmpty()) {
            return baseMapResolve(project)
        }
        return methodsTypeByMap
            .firstOrNull { PropertyUtil.isNameSetMethod(it.name, propertyMapValue) }
            ?.let { PropertyUtil.resolveResults(it) }
            ?: emptyArray()
    }

    private fun baseMapResolve(project: Project): Array<ResolveResult> {
        val type = property.sourceType ?: return emptyArray()
        val source = PropertyUtil.findSourceMember(property.name, type, project) ?: return emptyArray()
        return PropertyUtil.resolveResults(source)
    }
}