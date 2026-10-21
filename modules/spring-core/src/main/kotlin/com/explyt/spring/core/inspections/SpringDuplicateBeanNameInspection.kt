/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.service.conditional.ConditionPropertyValue
import com.explyt.spring.core.service.conditional.PropertyConditionValues
import com.explyt.spring.core.util.BeanFactoryMethods
import com.explyt.spring.core.util.SpringBootUtil
import com.explyt.spring.core.util.SpringCoreUtil.hasComponentAnnotation
import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanName
import com.explyt.util.ExplytPsiUtil.getMetaAnnotation
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.util.text.VersionComparatorUtil
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
        val module = ModuleUtilCore.findModuleForPsiElement(psiClass)
        return collisions.mapNotNull { collision ->
            overridingOf(aClass, module, collision)
                .takeUnless { it == Overriding.ALLOWED || collision.isReportedByDeclaringComponent(psiClass, module, it) }
                ?.let { collision.toProblem(aClass, manager, isOnTheFly, it) }
        }.toTypedArray()
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
            collision
        }.toList() + overridesOfRegisteredBaseFactories(psiClass)
    }

    private fun overridesOfRegisteredBaseFactories(psiClass: PsiClass): List<Collision> {
        if (psiClass.isInterface || !psiClass.hasComponentAnnotation()) return emptyList()
        val module = ModuleUtilCore.findModuleForPsiElement(psiClass) ?: return emptyList()
        val registeredClasses by lazy(LazyThreadSafetyMode.NONE) { registeredComponentClassesOf(module) }
        val isRegistered by lazy(LazyThreadSafetyMode.NONE) { psiClass.isAnyOf(registeredClasses) }
        return psiClass.methods.filterNot { it.isConditional() }.flatMap { override ->
            ProgressManager.checkCanceled()
            override.findSuperMethods()
                .filter { it.isFactoryOverriddenBy(override, psiClass) }
                .filter { isRegistered && it.containingClass.isAnyOf(registeredClasses) }
                .map { Collision(it.primaryBeanName, it, override, acrossConfigurations = true) }
        }
    }

    private fun registeredComponentClassesOf(module: Module): List<PsiClass> =
        SpringSearchServiceFacade.getInstance(module.project).getAllActiveBeans(module)
            .filterNot { it.isMember() }
            .map { it.psiClass }

    private fun PsiMethod.isFactoryOverriddenBy(override: PsiMethod, psiClass: PsiClass): Boolean {
        val declaringClass = containingClass ?: return false
        if (declaringClass.isInterface || isDeclaredIn(psiClass)) return false
        if (declaringClass.hasModifierProperty(PsiModifier.ABSTRACT)) return false
        if (!isMetaAnnotatedBy(SpringCoreClasses.BEAN) || isConditional()) return false
        if (override.isMetaAnnotatedBy(SpringCoreClasses.BEAN) && override.primaryBeanName != primaryBeanName) return false
        return declaringClass.hasComponentAnnotation()
    }

    private fun PsiClass?.isAnyOf(classes: List<PsiClass>): Boolean =
        this != null && classes.any { manager.areElementsEquivalent(it, this) }

    private fun Collision.isReportedByDeclaringComponent(
        psiClass: PsiClass,
        module: Module?,
        overriding: Overriding
    ): Boolean {
        val declaringClass = first.containingClass ?: return false
        if (first.isDeclaredIn(psiClass)) return false
        if (!psiClass.manager.areElementsEquivalent(declaringClass, second.containingClass)) return false
        if (!declaringClass.hasComponentAnnotation()) return false
        val declaringUClass = declaringClass.toUElementOfType<UClass>() ?: return false
        val declaringOverriding = overridingOf(declaringUClass, module, this)
        return declaringOverriding != Overriding.ALLOWED &&
                (!declaringOverriding.weak || declaringOverriding == overriding)
    }

    private fun PsiMethod.isConditional(): Boolean = getMetaAnnotation(SpringCoreClasses.CONDITIONAL) != null

    private fun overridingOf(aClass: UClass, module: Module?, collision: Collision): Overriding {
        module ?: return Overriding.DISALLOWED
        val springVersion = SpringBootUtil.getSpringCoreVersion(module)
        val kind = collision.kind
        val primaryNames = kind == CollisionKind.PRIMARY_NAMES
        val ignoredBeforeSpring62 = primaryNames && springVersion?.isBefore(SPRING_6_2) == true
        if (ignoredBeforeSpring62) return Overriding.IGNORED_BEFORE_SPRING_6_2
        val ignoredOverriding =
            if (primaryNames && aClass.enforcesUniqueMethods(module)) ignoredOverridingOf(module) else null
        if (ignoredOverriding == Overriding.IGNORED_ON_SPRING_7) return ignoredOverriding
        val disallowed = when (kind) {
            CollisionKind.PRIMARY_NAMES ->
                if (springVersion == null) Overriding.DISALLOWED_ON_UNKNOWN_SPRING else Overriding.DISALLOWED

            CollisionKind.PRIMARY_NAME_AND_ALIAS ->
                if (springVersion?.isBefore(SPRING_6_0) == false) Overriding.DISALLOWED
                else Overriding.DISALLOWED_IN_REGISTRATION_ORDER

            CollisionKind.ALIASES, CollisionKind.ACROSS_CONFIGURATIONS -> Overriding.DISALLOWED
        }
        return when (val value = PropertyConditionValues(module).valueOf(ALLOW_OVERRIDING_KEY)) {
            is ConditionPropertyValue.Known ->
                if (value.text.trim().equals("true", ignoreCase = true))
                    ignoredOverriding ?: collision.allowedOverridingOf(springVersion)
                else disallowed

            ConditionPropertyValue.Missing -> disallowed
            ConditionPropertyValue.Unresolvable ->
                if (disallowed == Overriding.DISALLOWED) Overriding.UNRESOLVED else disallowed
        }
    }

    private fun Collision.allowedOverridingOf(springVersion: String?): Overriding = when (kind) {
        CollisionKind.PRIMARY_NAMES, CollisionKind.ACROSS_CONFIGURATIONS -> Overriding.ALLOWED_ONE_DROPPED
        CollisionKind.ALIASES -> Overriding.ALLOWED
        CollisionKind.PRIMARY_NAME_AND_ALIAS -> {
            val declarationOrderDecides = springVersion?.isBefore(SPRING_6_2_7) == false &&
                    first.manager.areElementsEquivalent(first.containingClass, second.containingClass)
            when {
                !declarationOrderDecides -> Overriding.ALLOWED_MAY_DROP
                listOf(first, second).minBy { it.textOffset }.primaryBeanName == beanName ->
                    Overriding.ALLOWED_ONE_DROPPED

                else -> Overriding.ALLOWED
            }
        }
    }

    private fun String.isBefore(version: String): Boolean = VersionComparatorUtil.compare(this, version) < 0

    private val Collision.kind: CollisionKind
        get() = if (acrossConfigurations) CollisionKind.ACROSS_CONFIGURATIONS
        else when (listOf(first, second).count { it.primaryBeanName == beanName }) {
            2 -> CollisionKind.PRIMARY_NAMES
            1 -> CollisionKind.PRIMARY_NAME_AND_ALIAS
            else -> CollisionKind.ALIASES
        }

    private val PsiMethod.primaryBeanName: String
        get() = resolveBeanName.first()

    private fun ignoredOverridingOf(module: Module): Overriding? {
        val springMajor = SpringBootUtil.getSpringCoreMajorVersion(module) ?: return Overriding.IGNORED_ON_UNKNOWN_SPRING
        return Overriding.IGNORED_ON_SPRING_7.takeIf { springMajor >= SPRING_7 }
    }

    private fun UClass.enforcesUniqueMethods(module: Module): Boolean {
        val configurations = SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, SpringCoreClasses.CONFIGURATION)
        val configuration = uAnnotations.firstOrNull { it.qualifiedName == SpringCoreClasses.CONFIGURATION }
            ?: uAnnotations.firstOrNull { configurations.contains(it) }
            ?: return false
        val values = configurations.getAnnotationMemberValues(configuration, setOf(ENFORCE_UNIQUE_METHODS))
        return values.firstOrNull()?.evaluate() != false
    }

    private fun Collision.toProblem(
        aClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean,
        overriding: Overriding
    ): ProblemDescriptor? {
        val psiClass = aClass.javaPsi
        val declared = listOf(second, first).firstOrNull { it.isDeclaredIn(psiClass) }
        val anchor = (declared?.toUElementOfType<UMethod>() ?: aClass).uastAnchor?.sourcePsi ?: return null
        val (messageKey, highlightType) = when (overriding) {
            Overriding.UNRESOLVED -> UNRESOLVED_OVERRIDING_MESSAGE to ProblemHighlightType.WARNING
            Overriding.IGNORED_ON_UNKNOWN_SPRING -> UNKNOWN_SPRING_OVERRIDING_MESSAGE to ProblemHighlightType.WARNING
            Overriding.IGNORED_ON_SPRING_7 -> SPRING_7_OVERRIDING_MESSAGE to ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            Overriding.IGNORED_BEFORE_SPRING_6_2 -> IGNORED_METHOD_MESSAGE to ProblemHighlightType.WARNING
            Overriding.DISALLOWED_ON_UNKNOWN_SPRING -> UNKNOWN_SPRING_VERSION_MESSAGE to ProblemHighlightType.WARNING
            Overriding.DISALLOWED_IN_REGISTRATION_ORDER -> REGISTRATION_ORDER_MESSAGE to ProblemHighlightType.WARNING
            Overriding.ALLOWED_ONE_DROPPED -> ALLOWED_OVERRIDING_MESSAGE to ProblemHighlightType.WEAK_WARNING
            Overriding.ALLOWED_MAY_DROP -> ALLOWED_OVERRIDING_ORDER_MESSAGE to ProblemHighlightType.WEAK_WARNING
            else -> MESSAGE to ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        }
        val description = if (declared != null) {
            val other = if (declared == second) first else second
            SpringCoreBundle.message(messageKey, beanName, other.labelIn(psiClass))
        } else {
            SpringCoreBundle.message(
                INHERITED_METHODS_MESSAGE,
                second.labelIn(psiClass),
                SpringCoreBundle.message(messageKey, beanName, first.labelIn(psiClass))
            )
        }
        return manager.createProblemDescriptor(
            anchor,
            description,
            isOnTheFly,
            emptyArray(),
            highlightType
        )
    }

    private fun PsiMethod.isDeclaredIn(psiClass: PsiClass): Boolean =
        psiClass.manager.areElementsEquivalent(containingClass, psiClass)

    private fun PsiMethod.labelIn(psiClass: PsiClass): String =
        if (isDeclaredIn(psiClass)) name else listOfNotNull(containingClass?.name, name).joinToString(".")

    private data class Collision(
        val beanName: String,
        val first: PsiMethod,
        val second: PsiMethod,
        val acrossConfigurations: Boolean = false
    )

    private enum class CollisionKind {
        PRIMARY_NAMES,
        PRIMARY_NAME_AND_ALIAS,
        ALIASES,
        ACROSS_CONFIGURATIONS
    }

    private enum class Overriding(val weak: Boolean = false) {
        ALLOWED,
        ALLOWED_ONE_DROPPED(weak = true),
        ALLOWED_MAY_DROP(weak = true),
        DISALLOWED,
        DISALLOWED_ON_UNKNOWN_SPRING,
        DISALLOWED_IN_REGISTRATION_ORDER,
        UNRESOLVED,
        IGNORED_BEFORE_SPRING_6_2,
        IGNORED_ON_SPRING_7,
        IGNORED_ON_UNKNOWN_SPRING
    }

    private companion object {
        const val ALLOW_OVERRIDING_KEY = "spring.main.allow-bean-definition-overriding"
        const val ENFORCE_UNIQUE_METHODS = "enforceUniqueMethods"
        const val SPRING_6_0 = "6.0.0"
        const val SPRING_6_2 = "6.2.0"
        const val SPRING_6_2_7 = "6.2.7"
        const val SPRING_7 = 7
        const val MESSAGE = "explyt.spring.inspection.bean.name.duplicate"
        const val UNRESOLVED_OVERRIDING_MESSAGE = "explyt.spring.inspection.bean.name.duplicate.overriding.unresolved"
        const val SPRING_7_OVERRIDING_MESSAGE = "explyt.spring.inspection.bean.name.duplicate.overriding.spring7"
        const val IGNORED_METHOD_MESSAGE = "explyt.spring.inspection.bean.name.duplicate.ignored"
        const val UNKNOWN_SPRING_VERSION_MESSAGE = "explyt.spring.inspection.bean.name.duplicate.version.unknown"
        const val REGISTRATION_ORDER_MESSAGE = "explyt.spring.inspection.bean.name.duplicate.alias.order"
        const val INHERITED_METHODS_MESSAGE = "explyt.spring.inspection.bean.name.duplicate.inherited"
        const val ALLOWED_OVERRIDING_MESSAGE = "explyt.spring.inspection.bean.name.duplicate.overriding.allowed"
        const val ALLOWED_OVERRIDING_ORDER_MESSAGE =
            "explyt.spring.inspection.bean.name.duplicate.overriding.allowed.order"
        const val UNKNOWN_SPRING_OVERRIDING_MESSAGE =
            "explyt.spring.inspection.bean.name.duplicate.overriding.unknown.spring"
    }
}
