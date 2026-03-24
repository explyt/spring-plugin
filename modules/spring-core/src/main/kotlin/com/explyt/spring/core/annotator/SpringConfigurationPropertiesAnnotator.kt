/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.annotator

import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl
import com.intellij.lang.properties.psi.impl.PropertyValueImpl
import com.intellij.psi.PsiElement

class SpringConfigurationPropertiesAnnotator : SpringConfigurationAnnotator() {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is PropertyKeyImpl && element !is PropertyValueImpl) return

        val file = holder.currentAnnotationSession.file
        if (!SpringCoreUtil.isConfigurationPropertyFile(file)) return

        when (element) {
            is PropertyKeyImpl -> annotateKey(element, holder)
            is PropertyValueImpl -> annotateValue(element, holder)
        }
    }
}