/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.beans

import com.explyt.spring.ai.mcp.ProjectSources
import com.intellij.psi.PsiMember

enum class BeanOrigin {
    PROJECT, LIBRARY;

    companion object {
        fun of(declaration: PsiMember?): BeanOrigin? =
            declaration?.takeIf { it.isValid }?.let { if (ProjectSources.declares(it)) PROJECT else LIBRARY }

        val names: List<String> = entries.map { it.name }
    }
}
