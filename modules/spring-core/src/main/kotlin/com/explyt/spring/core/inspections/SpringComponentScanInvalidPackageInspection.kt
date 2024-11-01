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
                val attributesAsPsiLiteral = ExplytAnnotationUtil.getAttributeValues(annotation, attributesName)

                attributesAsPsiLiteral.mapNotNull { it.sourcePsi }.forEach attributes@{ sourcePsi ->
                    val text = ElementManipulators.getValueText(sourcePsi)
                    val range = ElementManipulators.getValueTextRange(sourcePsi)
                    val words = text.split(".")

                    var path = ""
                    for (word in words) {
                        path += word

                        if (!packageFqnSearcher.isPackageExist(path)) {
                            val curTextRange = TextRange(
                                0,
                                path.length
                            ).shiftRight(range.startOffset)

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