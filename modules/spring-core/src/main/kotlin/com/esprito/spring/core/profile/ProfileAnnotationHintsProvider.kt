package com.esprito.spring.core.profile

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringProperties.VALUE
import com.esprito.spring.core.service.MetaAnnotationsHolder
import com.esprito.spring.core.service.ProfilesService
import com.esprito.spring.core.service.SpringSearchService
import com.intellij.codeInsight.hints.declarative.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.endOffset
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
                InlineInlayPosition(sourcePsi.endOffset, true), hasBackground = true
            ) {
                text(": $result")
            }
        }
    }

}
