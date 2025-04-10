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

package com.explyt.spring.web.language.http

import com.explyt.spring.web.language.http.psi.HttpComment
import com.explyt.spring.web.language.http.psi.HttpFieldName
import com.explyt.spring.web.language.http.psi.HttpFieldValue
import com.explyt.spring.web.language.http.psi.HttpRequestBody
import com.explyt.spring.web.language.http.psi.HttpRequestDefiner
import com.explyt.spring.web.language.http.psi.HttpRequestTarget
import com.explyt.spring.web.language.http.psi.HttpTypes
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement

class HttpSyntaxHighlighterAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val leafElement = element as? LeafPsiElement ?: return

        if (leafElement.tokenType in BRACES_TYPES) {
            val grandParentElement = element.parent?.parent ?: return

            val textAttribute = when (grandParentElement) {
                is HttpRequestDefiner -> HttpSyntaxHighlighter.META_TOKEN
                is HttpComment        -> HttpSyntaxHighlighter.META_TOKEN
                is HttpRequestTarget  -> HttpSyntaxHighlighter.REQUEST_TARGET
                is HttpFieldName      -> HttpSyntaxHighlighter.HTTP_TOKEN
                is HttpFieldValue     -> HttpSyntaxHighlighter.FIELD_CONTENT
                is HttpRequestBody    -> HttpSyntaxHighlighter.REQUEST_BODY
                else                  -> return
            }

            holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES).textAttributes(textAttribute).create()
        }
    }

    companion object {
        private val BRACES_TYPES = setOf(HttpTypes.LBRACES, HttpTypes.RBRACES)
    }

}