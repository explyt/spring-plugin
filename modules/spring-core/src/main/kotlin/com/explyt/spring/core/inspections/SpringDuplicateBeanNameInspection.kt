/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.conditional.ConditionPropertyValue
import com.explyt.spring.core.service.conditional.PropertyConditionValues
import com.explyt.spring.core.util.BeanFactoryMethods
import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanName
import com.explyt.util.ExplytPsiUtil.getMetaAnnotation
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElementOfType

class SpringDuplicateBeanNameInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkClass(
        aClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val psiClass = aClass.javaPsi
        val collisions = collisionsIn(psiClass).ifEmpty { return null }
        val overriding = overridingOf(psiClass).takeUnless { it == Overriding.ALLOWED } ?: return null
        return collisions.mapNotNull { it.toProblem(manager, isOnTheFly, overriding) }.toTypedArray()
    }

    private fun collisionsIn(psiClass: PsiClass): List<Collision> {
        val firstDeclarations = HashMap<String, PsiMethod>()
        return BeanFactoryMethods.of(psiClass).filterNot { it.isConditional() }.mapNotNull { method ->
            ProgressManager.checkCanceled()
            val names = method.resolveBeanName
            val collision = names.firstNotNullOfOrNull { name ->
                firstDeclarations[name]?.takeIf { it.name != method.name }?.let { Collision(name, it, method) }
            }
            names.forEach { firstDeclarations.putIfAbsent(it, method) }
            collision?.takeIf { psiClass.manager.areElementsEquivalent(method.containingClass, psiClass) }
        }.toList()
    }

    private fun PsiMethod.isConditional(): Boolean = getMetaAnnotation(SpringCoreClasses.CONDITIONAL) != null

    private fun overridingOf(psiClass: PsiClass): Overriding {
        val module = ModuleUtilCore.findModuleForPsiElement(psiClass) ?: return Overriding.DISALLOWED
        return when (val value = PropertyConditionValues(module).valueOf(ALLOW_OVERRIDING_KEY)) {
            is ConditionPropertyValue.Known ->
                if (value.text.trim().equals("true", ignoreCase = true)) Overriding.ALLOWED else Overriding.DISALLOWED

            ConditionPropertyValue.Missing -> Overriding.DISALLOWED
            ConditionPropertyValue.Unresolvable -> Overriding.UNKNOWN
        }
    }

    private fun Collision.toProblem(
        manager: InspectionManager,
        isOnTheFly: Boolean,
        overriding: Overriding
    ): ProblemDescriptor? {
        val anchor = second.toUElementOfType<UMethod>()?.uastAnchor?.sourcePsi ?: return null
        val (messageKey, highlightType) = when (overriding) {
            Overriding.UNKNOWN -> UNRESOLVED_OVERRIDING_MESSAGE to ProblemHighlightType.WARNING
            else -> MESSAGE to ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        }
        return manager.createProblemDescriptor(
            anchor,
            SpringCoreBundle.message(messageKey, beanName, first.name),
            isOnTheFly,
            emptyArray(),
            highlightType
        )
    }

    private data class Collision(val beanName: String, val first: PsiMethod, val second: PsiMethod)

    private enum class Overriding { ALLOWED, DISALLOWED, UNKNOWN }

    private companion object {
        const val ALLOW_OVERRIDING_KEY = "spring.main.allow-bean-definition-overriding"
        const val MESSAGE = "explyt.spring.inspection.bean.name.duplicate"
        const val UNRESOLVED_OVERRIDING_MESSAGE = "explyt.spring.inspection.bean.name.duplicate.overriding.unresolved"
    }
}
