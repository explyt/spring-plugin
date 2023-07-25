package com.esprito.jpa.ql.reference

import com.esprito.jpa.ql.psi.JpqlInputParameterExpression
import com.intellij.openapi.extensions.ProjectExtensionPointName
import com.intellij.psi.ResolveResult

interface InputParameterReferenceResolver {
    fun getVariants(identifier: JpqlInputParameterExpression): List<Any>

    fun resolve(identifier: JpqlInputParameterExpression): List<ResolveResult>

    companion object {
        val EP = ProjectExtensionPointName<InputParameterReferenceResolver>(
            "com.esprito.jpa.ql.reference.inputParameterReferenceResolver"
        )
    }
}