package com.esprito.spring.core.profile

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.service.MetaAnnotationsHolder
import com.esprito.spring.core.service.ProfilesService
import com.intellij.codeInsight.hints.declarative.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.*
import com.intellij.psi.util.parentOfType
import com.intellij.refactoring.suggested.endOffset

class ProfileAnnotationHintsProvider : InlayHintsProvider {
    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector? {
        val module = ModuleUtilCore.findModuleForPsiElement(file) ?: return null
        val metaAnnotationsHolder = MetaAnnotationsHolder.of(module, SpringCoreClasses.PROFILE)
        return Collector(metaAnnotationsHolder)
    }

    class Collector(private val metaAnnotationsHolder: MetaAnnotationsHolder) : SharedBypassCollector {
        //TODO: UAST
        override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
            if (element !is PsiLiteralExpression) return
            val annotation = element.parentOfType<PsiAnnotation>() ?: return
            val annotationQn = annotation.qualifiedName ?: return
            if (!metaAnnotationsHolder.contains(annotation)) return
            val nameValuePair = element.parent as? PsiNameValuePair ?: return
            val parameterName = nameValuePair.name ?: "value"

            if (!metaAnnotationsHolder.isAttributeRelatedWith(
                    annotationQn,
                    parameterName,
                    SpringCoreClasses.PROFILE,
                    setOf("value")
                )
            ) return

            val profileExpression = ElementManipulators.getValueText(element)
            val result = ProfilesService.getInstance(element.project).compute(profileExpression)

            sink.addPresentation(
                InlineInlayPosition(element.endOffset, true), hasBackground = true
            ) {
                text(": $result")
            }
        }

    }

}
