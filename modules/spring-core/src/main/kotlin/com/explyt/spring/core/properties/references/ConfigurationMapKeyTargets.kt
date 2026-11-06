/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.references

import com.explyt.spring.core.completion.properties.ConfigurationProperty
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.explyt.spring.core.util.PropertyUtil

interface ConfigurationMapKeyTargets {
    fun findTargets(module: Module, mapProperty: ConfigurationProperty, mapKey: String): List<PsiElement>

    companion object {
        val EP_NAME = ExtensionPointName<ConfigurationMapKeyTargets>(
            "com.explyt.spring.core.configurationMapKeyTargets"
        )

        fun resolveTargets(module: Module, property: ConfigurationProperty, propertyKey: String): Array<ResolveResult> =
            findTargets(module, property, PropertyUtil.getMapKeyForLookup(propertyKey, property))
                .map(::PsiElementResolveResult).toTypedArray()

        fun findTargets(module: Module, mapProperty: ConfigurationProperty, mapKey: String): List<PsiElement> =
            EP_NAME.extensionList.flatMap { it.findTargets(module, mapProperty, mapKey) }.distinct()
    }
}
