package com.esprito.spring.core.inspections

import com.esprito.inspection.SpringBaseUastLocalInspectionTool
import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringCoreClasses.ANNOTATIONS_CACHE
import com.esprito.util.EspritoPsiUtil.isEqualOrInheritor
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getContainingUClass

class SpringCacheAnnotationsOnInterfaceInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkMethod(
        uMethod: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val uClass = uMethod.getContainingUClass() ?: return null
        if (!uClass.isInterface) return null
        val psiClass = uClass.javaPsi
        if (isImplicitlySubclassed(psiClass)) return null
        val psiMethod = uMethod.javaPsi

        if (!psiMethod.isMetaAnnotatedBy(ANNOTATIONS_CACHE)) return null
        val identifyingElement = (uMethod.sourcePsi as? PsiNameIdentifierOwner)?.identifyingElement ?: return null

        return arrayOf(
            manager.createProblemDescriptor(
                identifyingElement,
                SpringCoreBundle.message("esprito.spring.inspection.cache.interface.method"),
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            )
        )
    }

    override fun checkClass(
        uClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        if (uClass.isAnnotationType || !uClass.isInterface) return null
        val psiClass = uClass.javaPsi
        if (isImplicitlySubclassed(psiClass)) return null

        if (!psiClass.isMetaAnnotatedBy(ANNOTATIONS_CACHE)) return null
        val identifyingElement = (uClass.sourcePsi as? PsiNameIdentifierOwner)?.identifyingElement ?: return null

        return arrayOf(
            manager.createProblemDescriptor(
                identifyingElement,
                SpringCoreBundle.message("esprito.spring.inspection.cache.interface.class"),
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            )
        )
    }

    private fun isImplicitlySubclassed(psiClass: PsiClass): Boolean {

        return psiClass.isEqualOrInheritor("org.springframework.data.repository.Repository")
                || psiClass.isMetaAnnotatedBy(
            listOf(
                SpringCoreClasses.NETFLIX_FEIGN_CLIENT,
                SpringCoreClasses.OPEN_FEIGN_CLIENT
            )
        )
    }

}