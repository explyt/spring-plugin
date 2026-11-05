/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.references

import com.explyt.spring.core.completion.properties.ConfigurationProperty
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement

interface ConfigurationMapKeyTargets {
    fun findTargets(module: Module, mapProperty: ConfigurationProperty, mapKey: String): List<PsiElement>

    companion object {
        val EP_NAME = ExtensionPointName<ConfigurationMapKeyTargets>(
            "com.explyt.spring.core.configurationMapKeyTargets"
        )

        fun findTargets(module: Module, mapProperty: ConfigurationProperty, mapKey: String): List<PsiElement> =
            EP_NAME.extensionList.flatMap { it.findTargets(module, mapProperty, mapKey) }.distinct()
    }
}
