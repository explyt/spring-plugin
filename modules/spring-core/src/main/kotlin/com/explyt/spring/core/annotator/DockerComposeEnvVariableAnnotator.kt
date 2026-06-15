/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.annotator

import com.explyt.spring.core.completion.doker.isDockerEnvCandidate
import com.explyt.spring.core.properties.providers.ConfigurationPropertyKeyReference
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.ui.SimpleTextAttributes

class DockerComposeEnvVariableAnnotator : SpringConfigurationAnnotator() {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!isDockerEnvCandidate(element)) return
        val offset = element.node.startOffset
        element.references.filterIsInstance<ConfigurationPropertyKeyReference>().forEach {
            val textRange = it.rangeInElement.shiftRight(offset)

            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(textRange)
                .enforcedTextAttributes(SimpleTextAttributes.LINK_ATTRIBUTES.toTextAttributes())
                .create()
        }
    }
}