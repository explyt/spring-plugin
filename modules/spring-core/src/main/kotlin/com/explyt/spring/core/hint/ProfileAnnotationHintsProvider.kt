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

package com.explyt.spring.core.hint

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringProperties.VALUE
import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.service.ProfilesService
import com.explyt.spring.core.service.SpringSearchService
import com.intellij.codeInsight.hints.declarative.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.endOffset
import org.jetbrains.uast.*

class ProfileAnnotationHintsProvider : InlayHintsProvider {
    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector? {
        val module = ModuleUtilCore.findModuleForPsiElement(file) ?: return null
        val metaAnnotationsHolder = SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, SpringCoreClasses.PROFILE)
        return Collector(metaAnnotationsHolder)
    }

    class Collector(private val metaAnnotationsHolder: MetaAnnotationsHolder) : SharedBypassCollector {

        override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
            val uAnnotation = (element.toUElement() as? UAnnotation) ?: return
            val annotationQn = uAnnotation.qualifiedName ?: return
            val annotation = uAnnotation.javaPsi ?: return
            if (!metaAnnotationsHolder.contains(annotation)) return
            val uExpression = uAnnotation.attributeValues
                .find {
                    metaAnnotationsHolder.isAttributeRelatedWith(
                        annotationQn, it.name ?: VALUE, SpringCoreClasses.PROFILE, setOf(VALUE)
                    )
                }?.expression ?: return

            if (uExpression is ULiteralExpression) {
                addHint(uExpression, sink)
            }
            if (uExpression is UCallExpression) {
                uExpression.valueArguments
                    .filterIsInstance<ULiteralExpression>()
                    .forEach { addHint(it, sink) }
            }
        }

        private fun addHint(uExpression: UExpression, sink: InlayTreeSink) {
            val sourcePsi = uExpression.sourcePsi ?: return
            val profileExpression = ElementManipulators.getValueText(sourcePsi)
            val result = ProfilesService.getInstance(sourcePsi.project).compute(profileExpression)

            sink.addPresentation(
                InlineInlayPosition(sourcePsi.endOffset, true), hintFormat = HintFormat.default
            ) {
                text(": $result")
            }
        }
    }

}
