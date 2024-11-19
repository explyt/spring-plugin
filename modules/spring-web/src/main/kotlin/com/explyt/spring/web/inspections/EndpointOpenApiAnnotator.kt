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

package com.explyt.spring.web.inspections

import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention.EndpointInfo
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lombok.utils.decapitalize
import org.jetbrains.uast.UComment
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getUParentForIdentifier


class EndpointOpenApiAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val uMethod = getUParentForIdentifier(element) as? UMethod ?: return
        val psiMethod = uMethod.javaPsi

        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return

        if (!psiMethod.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING)) return
        val psiClass = psiMethod.containingClass ?: return
        if (!psiClass.isMetaAnnotatedBy(SpringWebClasses.CONTROLLER)) return
        val controllerName = psiClass.name ?: return

        val requestMappingMah = MetaAnnotationsHolder.of(module, SpringWebClasses.REQUEST_MAPPING)
        val path = requestMappingMah.getAnnotationMemberValues(psiMethod, setOf("path", "value")).asSequence()
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .firstOrNull() ?: ""

        val prefix = if (psiClass.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING)) {
            requestMappingMah.getAnnotationMemberValues(psiClass, setOf("path", "value")).asSequence()
                .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
                .firstOrNull() ?: ""
        } else {
            ""
        }

        val fullPath = SpringWebUtil.simplifyUrl("$prefix/$path")

        val requestMethods =
            requestMappingMah.getAnnotationMemberValues(psiMethod, setOf("method"))
                .map { it.text.split('.').last() }

        val description = uMethod.comments.firstOrNull()?.getCommentText() ?: ""
        val returnType = uMethod.returnType
        val returnTypeFqn = SpringWebUtil.getTypeFqn(returnType, psiMethod.language)

        val endpointElement = EndpointInfo(
            fullPath,
            requestMethods,
            element,
            uMethod.name,
            controllerName.replace("controller", "", true)
                .decapitalize(),
            description,
            returnTypeFqn,
            SpringWebUtil.collectPathVariables(psiMethod),
            SpringWebUtil.collectRequestParameters(psiMethod),
            SpringWebUtil.getRequestBodyInfo(psiMethod)
        )

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element.textRange)
            .withFix(AddEndpointToOpenApiIntention(endpointElement))
            .create()
    }

    private fun UComment.getCommentText(): String {
        val commentText = text.trim()

        if (commentText.startsWith("//")) {
            return commentText.substring(2).trim()
        } else if (commentText.startsWith("/*") && commentText.endsWith("*/")) {
            return commentText.substring(2, commentText.length - 2).trimIndent()
        }

        return commentText
    }

}
