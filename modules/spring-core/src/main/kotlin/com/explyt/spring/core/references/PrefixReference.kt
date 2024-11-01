/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core.references

import com.explyt.spring.core.SpringProperties
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

enum class PrefixReferenceType {
    ANNOTATION_PROPERTY,
    FILE_PROPERTY
}

class PrefixReference(
    element: PsiElement,
    textRange: TextRange,
    private val type: PrefixReferenceType
) : PsiReferenceBase<PsiElement>(element, textRange, false) {

    override fun resolve(): PsiElement {
        return this.element
    }

    override fun getVariants(): Array<Any> {
        return when (type) {
            PrefixReferenceType.ANNOTATION_PROPERTY -> {
                arrayOfAnnotationProperties()
            }
            PrefixReferenceType.FILE_PROPERTY -> {
                arrayOfAll()
            }
        }
    }

    private fun arrayOfAnnotationProperties(): Array<Any> {
        return arrayOf(
            LookupElementBuilder.create(SpringProperties.PREFIX_CLASSPATH).bold(),
            LookupElementBuilder.create(SpringProperties.PREFIX_FILE).bold(),
        )
    }

    private fun arrayOfAll(): Array<Any> {
        return arrayOf(
            LookupElementBuilder.create(SpringProperties.PREFIX_CLASSPATH_STAR).bold(),
            LookupElementBuilder.create(SpringProperties.PREFIX_CLASSPATH).bold(),
            LookupElementBuilder.create(SpringProperties.PREFIX_FILE).bold(),
            LookupElementBuilder.create(SpringProperties.PREFIX_HTTP).bold(),
        )
    }
}

