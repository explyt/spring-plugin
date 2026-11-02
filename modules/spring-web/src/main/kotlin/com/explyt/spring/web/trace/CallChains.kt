/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.trace

import com.explyt.spring.web.contract.EndpointContracts
import com.explyt.spring.web.loader.EndpointElement
import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getUastParentOfType

data class CallChainStart(val method: PsiMethod, val route: FunctionalRouteTarget?)

object CallChains {

    const val MAX_TRACED_METHODS = 50
    const val DEFAULT_DEPTH = 3

    fun startOf(psiFile: PsiFile, document: Document, line: Int): CallChainStart? {
        val route = FunctionalRouteTarget.atLine(psiFile, document, line)
        val method = route?.start ?: methodAtLine(psiFile, document, line) ?: return null
        return CallChainStart(method, route)
    }

    fun startOf(endpoint: EndpointElement): CallChainStart? {
        FunctionalRouteTarget.of(endpoint)?.let { return CallChainStart(it.start, it) }
        return EndpointContracts.declaringMethodOf(endpoint)
            ?.takeUnless { FunctionalRouteTarget.isRouterFactory(it) }
            ?.let { CallChainStart(it, route = null) }
    }

    fun trace(start: CallChainStart, depth: Int = DEFAULT_DEPTH): CallChain =
        CallChainTracer(start.method.project, MAX_TRACED_METHODS).trace(start.method, depth, start.route?.startBody)

    /**
     * The method declared on [line], whether that line carries its signature, its body or its annotations.
     *
     * Only the line's *first* offset used to be examined, and that offset is the indentation whitespace. Inside a
     * body that whitespace is a child of the method, so a body line resolved; on a declaration line it is a
     * sibling of the method under the class body, so the search reached the class and reported that no method
     * exists. The declaration line is the one every other tool hands out - `explyt_find_spring_endpoint` reports
     * a handler at its declaration - so the two tools disagreed about the same method.
     *
     * Scanning the line leaf by leaf rather than widening the parent search keeps a blank line between two methods
     * unresolved, which is a genuine miss and must not silently trace a neighbour.
     */
    private fun methodAtLine(psiFile: PsiFile, document: Document, line: Int): PsiMethod? {
        val lineIndex = (line - 1).coerceIn(0, document.lineCount - 1)
        val lineEnd = document.getLineEndOffset(lineIndex)
        var offset = document.getLineStartOffset(lineIndex)

        while (offset <= lineEnd) {
            ProgressManager.checkCanceled()
            val leaf = psiFile.findElementAt(offset) ?: return null
            leaf.getUastParentOfType<UMethod>()?.javaPsi?.let { return it }
            offset = maxOf(leaf.textRange.endOffset, offset + 1)
        }
        return null
    }
}
