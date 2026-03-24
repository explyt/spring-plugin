/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.annotator

import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence

class SpringConfigurationYamlAnnotator : SpringConfigurationAnnotator() {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is YAMLKeyValue) return

        val file = holder.currentAnnotationSession.file
        if (!SpringCoreUtil.isConfigurationPropertyFile(file)) return

        annotateKey(element, holder)
        annotateValue(element, holder)

        val yamlValue = element.value ?: return
        when (yamlValue) {
            is YAMLScalar -> annotateValue(yamlValue, holder)
            is YAMLSequence -> yamlValue.items
                .mapNotNull { it.value }
                .filterIsInstance<YAMLScalar>()
                .forEach { annotateValue(it, holder) }
        }
    }
}
