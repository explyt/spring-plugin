/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.idea.base.psi.getLineNumber

/**
 * The one rule every MCP tool uses to point at a declaration, so the tools cannot disagree about where a member is.
 *
 * Light and synthetic members - a Kotlin `data class` `copy()`, an enum `values()`, or any light method whose origin
 * declaration is absent - have no text range, and reading a line number from them fails. Such a member is reported
 * through its declaring class, and through nothing at all when that class is synthetic too. A method Lombok generates
 * reports an empty range at the start of the file instead; it is reported through the field it was generated from.
 */
object McpSourcePositions {

    /** The physical element a position may be reported for, or `null` when there is none. */
    fun sourceAnchorOf(element: PsiElement): PsiElement? =
        element.withSourcePosition()
            ?: (element as? PsiMember)?.containingClass?.withSourcePosition()

    /**
     * 1-based line of an anchor returned by [sourceAnchorOf]: the line its name is declared on, when it has a name.
     *
     * A declaration's text starts at its documentation comment, so the start of the element is the first line of a
     * KDoc or a Javadoc - above the annotations and the signature a caller is looking for, and not the line
     * `explyt_trace_spring_call_chain` names when it lists the methods of a file.
     */
    fun lineOfAnchor(anchor: PsiElement): Int? =
        anchor.containingFile?.getLineNumber(declarationOffsetOf(anchor))?.plus(1)

    private fun declarationOffsetOf(anchor: PsiElement): Int {
        val range = anchor.textRange
        val name = (anchor as? PsiNameIdentifierOwner)?.nameIdentifier?.textRange
        return name?.takeIf { !it.isEmpty && range.contains(it) }?.startOffset ?: range.startOffset
    }

    private fun PsiElement.withSourcePosition(): PsiElement? =
        takeIf { it.hasSourcePosition() } ?: navigationElement?.takeIf { it.hasSourcePosition() }

    private fun PsiElement.hasSourcePosition(): Boolean = textRange?.isEmpty == false && containingFile != null
}
