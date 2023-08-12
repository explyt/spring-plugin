package com.esprito.spring.core.inspections

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses.COMPONENT_SCAN
import com.esprito.spring.core.util.PsiPackagesSearcher
import com.esprito.util.EspritoAnnotationUtil
import com.intellij.codeInspection.*
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope

class SpringComponentScanInvalidPackageInspection : AbstractBaseJavaLocalInspectionTool() {
    override fun checkClass(
        aClass: PsiClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        if (!aClass.hasAnnotation(COMPONENT_SCAN)) return null

        val problems = mutableListOf<ProblemDescriptor>()

        aClass.annotations
            .filter { annotation -> annotation.hasQualifiedName(COMPONENT_SCAN) }
            .forEach { annotation ->
                val attributesAsPsiLiteral =
                    EspritoAnnotationUtil.getArrayAttributeAsPsiLiteral(annotation, setOf("value", "basePackages"))

                // if countOfPackagesFound = 0 than we create only 1 problem
                var countOfPackagesFound = 0
                val currentProblems = mutableListOf<ProblemDescriptor>()
                attributesAsPsiLiteral.forEach { psiLiteral ->
                    val text = psiLiteral.value.toString()
                    val range = ElementManipulators.getValueTextRange(psiLiteral)
                    val words = text.split(".")

                    var path = ""
                    for (word in words) {
                        path += word

                        val packages =
                            PsiPackagesSearcher.getFilteredPackages(
                                manager.project,
                                GlobalSearchScope.allScope(manager.project),
                                path
                            )
                        countOfPackagesFound += packages.size

                        if (packages.isEmpty()) {
                            var curTextRange = TextRange(
                                path.length - word.length,
                                path.length
                            ).shiftRight(range.startOffset)

                            //it's for case "org.demo." show problem in "." (not to ")
                            if (word.isEmpty()) {
                                curTextRange = curTextRange.shiftLeft(1)
                            }

                            val problem = manager.createProblemDescriptor(
                                psiLiteral,
                                curTextRange,
                                SpringCoreBundle.message("esprito.spring.inspection.method.componentScan.notFound"),
                                ProblemHighlightType.ERROR,
                                true
                            )

                            currentProblems += problem
                        }

                        path += "."
                    }

                    if (countOfPackagesFound == 0) {
                        val problem = manager.createProblemDescriptor(
                            psiLiteral,
                            TextRange(0, range.endOffset + 1),
                            SpringCoreBundle.message("esprito.spring.inspection.method.componentScan.notFound"),
                            ProblemHighlightType.ERROR,
                            true
                        )
                        problems += problem
                    } else {
                        problems += currentProblems
                    }

                }


            }


        return problems.toTypedArray()
    }

}