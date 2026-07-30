/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses.COMPONENT_SCAN
import com.explyt.spring.core.SpringCoreClasses.CONFIGURATION_PROPERTIES_SCAN
import com.explyt.spring.core.search.PsiPackageFqnSearchService
import com.explyt.spring.core.util.SpringCoreUtil
import com.explyt.util.ExplytAnnotationUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import org.jetbrains.uast.UClass
import org.jetbrains.uast.ULiteralExpression

class SpringComponentScanInvalidPackageInspection : SpringBaseUastLocalInspectionTool() {
    override fun checkClass(
        aClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()
        val packageFqnSearcher = PsiPackageFqnSearchService.getInstance(manager.project)

        aClass.uAnnotations
            .forEach { annotation ->
                val attributesName =
                    when (annotation.qualifiedName) {
                        COMPONENT_SCAN, CONFIGURATION_PROPERTIES_SCAN -> setOf("value", SpringCoreUtil.BASE_PACKAGES)
                        else -> setOf(SpringCoreUtil.BASE_PACKAGES, SpringCoreUtil.SCAN_BASE_PACKAGES)
                    }
                val attributeValues = ExplytAnnotationUtil.getAttributeValues(annotation, attributesName)

                attributeValues.forEach attributes@{ expression ->
                    val packageName = expression.evaluate() as? String ?: return@attributes
                    val sourcePsi = expression.sourcePsi ?: return@attributes
                    val sourceRange = ElementManipulators.getValueTextRange(sourcePsi)
                    val isPlainLiteral = expression is ULiteralExpression
                            && ElementManipulators.getValueText(sourcePsi) == packageName
                    val words = packageName.split(".")

                    var path = ""
                    for (word in words) {
                        path += word

                        if (!packageFqnSearcher.isPackageExist(path)) {
                            val curTextRange = if (isPlainLiteral) {
                                TextRange(0, path.length).shiftRight(sourceRange.startOffset)
                            } else {
                                sourceRange
                            }

                            problems.add(
                                manager.createProblemDescriptor(
                                    sourcePsi,
                                    curTextRange,
                                    SpringCoreBundle.message("explyt.spring.inspection.method.componentScan.notFound"),
                                    ProblemHighlightType.ERROR,
                                    true
                                )
                            )
                            return@attributes
                        }
                        path += "."
                    }
                }
            }

        return problems.toTypedArray()
    }

}