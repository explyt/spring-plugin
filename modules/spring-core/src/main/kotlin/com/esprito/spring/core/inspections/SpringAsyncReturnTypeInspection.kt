package com.esprito.spring.core.inspections

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.esprito.util.EspritoPsiUtil.isPrivate
import com.intellij.codeInsight.daemon.impl.quickfix.MethodReturnTypeFix
import com.intellij.codeInspection.*
import com.intellij.psi.*
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod


class SpringAsyncReturnTypeInspection : AbstractBaseUastLocalInspectionTool() {

    override fun checkClass(
        uClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val psiClass = uClass.javaPsi
        val isClassAsync = psiClass.isMetaAnnotatedBy(SpringCoreClasses.ASYNC)

        val futurePsiClass = lazy {
            val javaPsiFacade = JavaPsiFacade.getInstance(psiClass.project)
            javaPsiFacade.findClass(SpringCoreClasses.FUTURE, psiClass.resolveScope)
        }

        return uClass.methods.asSequence()
            .filter { !it.javaPsi.isPrivate }
            .flatMapTo(mutableListOf()) {
                checkMethod(it, manager, isOnTheFly, isClassAsync, futurePsiClass.value)
            }
            .toTypedArray()
    }

    private fun checkMethod(
        uMethod: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean,
        isClassAsync: Boolean,
        genericPsiClass: PsiClass?
    ): List<ProblemDescriptor> {
        if (genericPsiClass == null) return emptyList()
        if (uMethod.returnType == PsiTypes.voidType()) return emptyList()
        val psiIdentifier = uMethod.uastAnchor?.sourcePsi ?: return emptyList()
        val psiMethod = uMethod.javaPsi
        if (!isClassAsync && !psiMethod.isMetaAnnotatedBy(SpringCoreClasses.ASYNC)) return emptyList()
        val returnType = uMethod.returnType ?: return emptyList()
        if (returnType.isInheritorOf(SpringCoreClasses.FUTURE)) return emptyList()

        return listOf(
            manager.createProblemDescriptor(
                psiIdentifier,
                SpringCoreBundle.message("esprito.spring.inspection.async.signature.incorrect"),
                isOnTheFly,
                arrayOf(
                    MethodReturnTypeFix(psiMethod, createReturnType(genericPsiClass, psiMethod, returnType), true)
                ),
                ProblemHighlightType.WARNING,
            )
        )
    }

    private fun createReturnType(genericPsiClass: PsiClass, psiMethod: PsiMethod, returnType: PsiType): PsiType {
        return JavaPsiFacade.getInstance(psiMethod.project)
            .elementFactory.createType(genericPsiClass, boxedIfPrimitive(returnType, psiMethod))
    }

    private fun boxedIfPrimitive(returnType: PsiType, psiMethod: PsiMethod): PsiType {
        if (returnType !is PsiPrimitiveType) return returnType

        return returnType.getBoxedType(psiMethod) ?: returnType
    }

}