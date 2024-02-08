package com.esprito.spring.core.inspections

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringCoreClasses.ANNOTATIONS_WITH_PACKAGE_ANT_REFERENCES
import com.esprito.spring.core.SpringCoreClasses.COMPONENT_SCAN
import com.esprito.spring.core.util.PsiPackagesSearcher
import com.esprito.util.EspritoAnnotationUtil
import com.intellij.codeInspection.AbstractBaseUastLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.uast.UClass

class SpringComponentScanInvalidPackageInspection : AbstractBaseUastLocalInspectionTool() {
    override fun checkClass(
        aClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val problems = mutableListOf<ProblemDescriptor>()

        aClass.uAnnotations
            .filter { annotation -> ANNOTATIONS_WITH_PACKAGE_ANT_REFERENCES.contains(annotation.qualifiedName) }
            .forEach { annotation ->
                val attributesName =
                    when (annotation.qualifiedName) {
                        COMPONENT_SCAN -> setOf("value", "basePackages")
                        SpringCoreClasses.SPRING_BOOT_APPLICATION -> setOf("scanBasePackages")
                        else -> emptySet()
                    }
                val attributesAsPsiLiteral = EspritoAnnotationUtil.getAttributeValues(annotation, attributesName)

                // if countOfPackagesFound = 0 than we create only 1 problem
                var countOfPackagesFound = 0
                val currentProblems = mutableListOf<ProblemDescriptor>()
                attributesAsPsiLiteral.mapNotNull { it.sourcePsi }.forEach { sourcePsi ->
                    val text = ElementManipulators.getValueText(sourcePsi)
                    val range = ElementManipulators.getValueTextRange(sourcePsi)
                    val words = text.split(".")

                    var path = ""
                    for (word in words) {
                        path += word

                        val packages = PsiPackagesSearcher.getFilteredPackages(
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
                                sourcePsi,
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
                            sourcePsi,
                            range,
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