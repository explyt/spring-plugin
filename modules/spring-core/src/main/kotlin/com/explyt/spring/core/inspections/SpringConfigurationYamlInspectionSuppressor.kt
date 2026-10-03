/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.psi.PsiElement

private val SCALAR_TYPE_INSPECTIONS = setOf("YAMLIncompatibleTypes")

/**
 * Silences YAML inspections that judge a value by its YAML scalar type inside a Spring configuration file.
 *
 * Spring converts every scalar to the type of the member it binds to, so `enabled: true` and
 * `enabled: "${FLAG:false}"` are the same `Boolean` to the binder. The YAML type of a scalar carries no meaning
 * there; whether a value fits its property is decided by [SpringYamlInspection] against the declared type.
 */
class SpringConfigurationYamlInspectionSuppressor : InspectionSuppressor {

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean =
        toolId in SCALAR_TYPE_INSPECTIONS && SpringCoreUtil.isConfigurationPropertyFile(element.containingFile)

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> =
        SuppressQuickFix.EMPTY_ARRAY
}
