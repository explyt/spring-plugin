/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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