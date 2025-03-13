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

import com.explyt.spring.web.language.http.psi.HttpRequestDefiner
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.psi.PsiElement

class HttpSyntaxHighlighterAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element is HttpRequestDefiner)
            holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES).textAttributes(REQUEST_DEFINER).create()
    }

    companion object {
        @JvmStatic
        val REQUEST_DEFINER = createTextAttributesKey(
            "HTTP_REQUEST_DEFINER",
            DefaultLanguageHighlighterColors.LINE_COMMENT
        )
    }

}