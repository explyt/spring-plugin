/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.quickfix

import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiReference

/**
 * Rewrites the configuration key inside `${...}` placeholders that reference a renamed property.
 *
 * The usage is a string literal in arbitrary source: `@Value("${a.b.c}")`, a SpEL expression, a Kotlin raw string.
 * Rebuilding it through `PsiElementFactory.createExpressionFromText` cannot work, because the reference range may
 * cover only the key *inside* the literal — feeding that bare key to a Java expression parser throws
 * `IncorrectOperationException: Incorrect expression 'a.b.c'`, and a kebab-cased key makes it worse by parsing the
 * dashes as subtraction.
 *
 * Editing the reference's own range in the document sidesteps parsing entirely and preserves the quoting style,
 * the default value after `:` and everything else around the key.
 */
object QuickFixUsageRenamer {

    fun renameKeyInUsages(project: Project, usages: List<PsiReference>, newKey: String) {
        if (usages.isEmpty()) return

        val documentManager = PsiDocumentManager.getInstance(project)
        val rangesByDocument = LinkedHashMap<Document, MutableList<TextRange>>()
        for (usage in usages) {
            val element = usage.element
            if (!element.isValid) continue
            val document = documentManager.getDocument(element.containingFile ?: continue) ?: continue
            val range = usage.rangeInElement.shiftRight(element.textRange.startOffset)
            if (document.getText(range) == newKey) continue
            rangesByDocument.getOrPut(document) { mutableListOf() } += range
        }

        for ((document, ranges) in rangesByDocument) {
            documentManager.doPostponedOperationsAndUnblockDocument(document)
            // One file can hold several usages — a placeholder chain such as `${A:${a.b_c:false}}` even nests them.
            // Rewriting back to front keeps the offsets of the not-yet-rewritten ranges valid.
            for (range in ranges.sortedByDescending { it.startOffset }) {
                document.replaceString(range.startOffset, range.endOffset, newKey)
            }
            documentManager.commitDocument(document)
        }
    }
}
