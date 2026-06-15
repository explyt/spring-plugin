/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.jpa.ql.reference

import com.explyt.jpa.ql.psi.JpqlInputParameterExpression
import com.intellij.openapi.extensions.ProjectExtensionPointName
import com.intellij.psi.ResolveResult

interface InputParameterReferenceResolver {
    fun getVariants(identifier: JpqlInputParameterExpression): List<Any>

    fun resolve(identifier: JpqlInputParameterExpression): List<ResolveResult>

    companion object {
        val EP = ProjectExtensionPointName<InputParameterReferenceResolver>(
            "com.explyt.jpa.ql.reference.inputParameterReferenceResolver"
        )
    }
}