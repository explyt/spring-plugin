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

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.SpringWebClasses.REQUEST_MAPPING
import com.explyt.spring.web.inspections.quickfix.AddPathVariableQuickFix
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.util.ExplytPsiUtil.getHighlightRange
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.toSourcePsi
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.evaluateString

class SpringOmittedPathVariableParameterInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkMethod(
        method: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val psiMethod = method.javaPsi
        if (!psiMethod.isMetaAnnotatedBy(REQUEST_MAPPING)) return null
        val module = ModuleUtilCore.findModuleForPsiElement(psiMethod) ?: return null

        val urlPathParams = collectUrlPathParams(module, method)
        val pathVariableInfos = SpringWebUtil.collectPathVariables(psiMethod)

        val problems = mutableListOf<ProblemDescriptor>()

        for (pathVariableInfo in pathVariableInfos) {
            if (!pathVariableInfo.isMap
                && pathVariableInfo.isRequired
                && urlPathParams.any { urlPathParam ->
                    urlPathParam.namesWithRanges.none {
                        it.name == pathVariableInfo.name
                    }
                }
            ) {
                val pathVariableSourcePsi = pathVariableInfo.psiElement.toSourcePsi() ?: continue
                problems += manager.createProblemDescriptor(
                    pathVariableSourcePsi,
                    pathVariableSourcePsi.getHighlightRange(),
                    SpringWebBundle.message("explyt.spring.web.inspection.pathVariable"),
                    ProblemHighlightType.WARNING,
                    isOnTheFly,
                )
            }
        }

        if (pathVariableInfos.none { it.isMap }) {
            for (urlPathParam in urlPathParams) {
                for (nameAndRange in urlPathParam.namesWithRanges) {
                    if (pathVariableInfos.none {
                            it.name == nameAndRange.name
                        }
                    ) {
                        val urlPathParamSourcePsi = urlPathParam.element.toSourcePsi() ?: continue
                        val fixes = mutableListOf<LocalQuickFix>()
                        if (urlPathParam.element.isInJavaFile()) {
                            fixes.add(AddPathVariableQuickFix(psiMethod, nameAndRange.name))
                        }

                        problems += manager.createProblemDescriptor(
                            urlPathParamSourcePsi,
                            nameAndRange.range,
                            SpringWebBundle.message("explyt.spring.web.inspection.pathVariable"),
                            ProblemHighlightType.WARNING,
                            isOnTheFly,
                            *fixes.toTypedArray()
                        )
                    }
                }
            }
        }

        return problems.toTypedArray()
    }

    private fun collectUrlPathParams(module: Module, method: UMethod): MutableList<RefInfo> {
        val mahRequestMapping = SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, REQUEST_MAPPING)
        val urlPaths = mahRequestMapping.getAnnotationValues(method, setOf("value", "path"))

        val urlPathParams = mutableListOf<RefInfo>()
        for (memberValue in urlPaths) {
            val sourcePsi = memberValue.sourcePsi ?: continue
            memberValue.evaluateString() ?: continue
            val urlPath = memberValue.asSourceString()
            val namesWithRanges = SpringWebUtil.NameInBracketsRx.findAll(urlPath)
                .mapNotNull { it.groups["name"] }
                .mapTo(mutableListOf()) {
                    val pathParameterName = it.value.substringBefore(":")
                    val rangeStart = it.range.first
                    val range = TextRange(rangeStart, rangeStart + pathParameterName.length)
                    NameWithRange(pathParameterName, range)
                }
            urlPathParams.add(RefInfo(sourcePsi, namesWithRanges))
        }
        return urlPathParams
    }

    private fun PsiElement.isInJavaFile() = containingFile.fileType is JavaFileType

    private data class RefInfo(val element: PsiElement, val namesWithRanges: List<NameWithRange>)
    private data class NameWithRange(val name: String, val range: TextRange)
}