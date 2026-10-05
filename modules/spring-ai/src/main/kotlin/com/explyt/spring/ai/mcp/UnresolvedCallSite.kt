/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UCallableReferenceExpression
import org.jetbrains.uast.UExpression

/**
 * A call, or a callable reference, naming a method the IDE cannot resolve: the receiver's type comes from a jar that
 * was never downloaded, or the method does not exist.
 *
 * Such a call runs once the classpath is complete, and `dsl.update(...)` on an injected `DSLContext` whose jar is
 * absent is the one call of the method a trace is after; dropping it made the method look like one that calls
 * nothing. What the source states - the receiver expression, the name written at the call site and its line - is
 * kept; what the callee is stays unknown.
 *
 * @property receiver the instance the method is invoked on, or `null` when the call names none.
 */
internal class UnresolvedCallSite private constructor(
    val methodName: String,
    val receiver: UExpression?,
    override val line: Int?,
) : CallSite {

    companion object {

        fun of(call: UCallExpression): UnresolvedCallSite? {
            val name = call.methodIdentifier?.name
                ?: (call.sourcePsi as? KtCallExpression)?.calleeExpression?.text
                ?: return null
            val line = (call.methodIdentifier?.sourcePsi ?: call.sourcePsi)?.let(McpSourcePositions::lineOfAnchor)
            return UnresolvedCallSite(name, call.receiver, line)
        }

        fun of(reference: UCallableReferenceExpression): UnresolvedCallSite {
            val line = reference.sourcePsi?.let(McpSourcePositions::lineOfAnchor)
            return UnresolvedCallSite(reference.callableName, reference.qualifierExpression, line)
        }
    }
}
