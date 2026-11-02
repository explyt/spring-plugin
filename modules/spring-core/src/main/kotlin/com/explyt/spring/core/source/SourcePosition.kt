/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.source

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

/**
 * Location and line of a single source anchor. All of them are `null` when the reported element has no
 * physical declaration to point at, so a caller never gets a path and a line taken from different files.
 */
data class SourcePosition(val location: SourceLocation, val line: Int?) {
    val filePath: String? get() = location.filePath
    val library: String? get() = location.library

    companion object {
        val UNKNOWN = SourcePosition(SourceLocation.UNKNOWN, null)

        fun of(element: PsiElement, project: Project): SourcePosition {
            val anchor = SourcePositions.sourceAnchorOf(element) ?: return UNKNOWN
            return SourcePosition(SourceLocation.of(anchor, project), SourcePositions.lineOfAnchor(anchor))
        }
    }
}
