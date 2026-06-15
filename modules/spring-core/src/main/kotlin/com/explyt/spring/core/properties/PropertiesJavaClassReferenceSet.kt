/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceSet

class PropertiesJavaClassReferenceSet(str: String, element: PsiElement, startInElement: Int) : JavaClassReferenceSet(
    str,
    element,
    startInElement,
    false,
    JavaClassReferenceProvider().apply { setOption(JavaClassReferenceProvider.ADVANCED_RESOLVE, true) })