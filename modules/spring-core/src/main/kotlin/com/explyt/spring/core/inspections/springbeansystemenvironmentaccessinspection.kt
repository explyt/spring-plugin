/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.inspections.quickfix.ReplaceSystemAccessWithEnvironmentFix
import com.explyt.spring.core.inspections.quickfix.ReplaceSystemAccessWithValueFix
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiClassInitializer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UField
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * Reports `System.getenv(...)` / `System.getProperty(...)` calls that read a setting from the JVM instead of the
 * Spring `Environment`.
 *
 * Such a value cannot be overridden per profile, by an external configuration source or by a test property source, it
 * does not appear in `/actuator/env`, and the plugin's own property resolution cannot see it. Spring Boot exposes
 * every environment variable through `SystemEnvironmentPropertySource` with relaxed-name mapping, so `@Value("${VAR}")`
 * or `environment.getProperty("VAR")` is a drop-in replacement.
 *
 * Only classes that the plugin's bean model considers Spring beans are inspected. `System.getenv` is reported as a
 * warning; `System.getProperty` is reported as a weak warning and only when the key is also declared in one of the
 * module's configuration files, so JVM properties such as `java.io.tmpdir` or `user.home` stay unreported.
 */
class SpringBeanSystemEnvironmentAccessInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkClass(
        uClass: UClass, manager: InspectionManager, isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        if (!SpringCoreUtil.isComponentCandidate(uClass.javaPsi)) return emptyArray()

        val calls = findCandidateCalls(uClass)
        if (calls.isEmpty()) return emptyArray()

        val sourcePsi = uClass.sourcePsi ?: return emptyArray()
        val module = ModuleUtilCore.findModuleForPsiElement(sourcePsi) ?: return emptyArray()
        // The keys the module declares itself. A `System.getProperty` key that is not among them is a JVM property,
        // not an application setting, and must not be reported.
        val configurationKeys = lazy {
            DefinedConfigurationPropertiesSearch.getInstance(module.project)
                .getAllProperties(module)
                .mapTo(mutableSetOf()) { it.key }
        }

        val problems = mutableListOf<ProblemDescriptor>()
        for (call in calls) {
            if (runsBeforeContextStart(call)) continue
            val method = call.resolve() ?: continue
            if (method.containingClass?.qualifiedName != JAVA_LANG_SYSTEM) continue

            val key = constantKey(call) ?: continue
            val highlightType = when (method.name) {
                GETENV -> ProblemHighlightType.WARNING
                GETPROPERTY -> if (key in configurationKeys.value) ProblemHighlightType.WEAK_WARNING else continue
                else -> continue
            }

            val message = when (method.name) {
                GETENV -> SpringCoreBundle.message("explyt.spring.inspection.system.env.getenv", key)
                else -> SpringCoreBundle.message("explyt.spring.inspection.system.env.getproperty", key)
            }
            val callPsi = callAnchor(call) ?: continue
            problems += manager.createProblemDescriptor(
                callPsi,
                message,
                isOnTheFly,
                arrayOf(
                    ReplaceSystemAccessWithValueFix(callPsi, key),
                    ReplaceSystemAccessWithEnvironmentFix(callPsi, key),
                ),
                highlightType
            )
        }
        return problems.toTypedArray()
    }

    /**
     * The `System.getenv(...)` / `System.getProperty(...)` call sites of the class, collected through UAST so that
     * Java and Kotlin share one implementation. Calls that belong to a nested class are left to the inspection pass
     * that runs for that class.
     */
    private fun findCandidateCalls(uClass: UClass): List<UCallExpression> {
        val calls = mutableListOf<UCallExpression>()
        uClass.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                // `System.getProperty(key, default)` reads a setting too, but the replacement is not a mechanical rewrite.
                if (node.valueArguments.size == 1 && node.getContainingUClass() == uClass) {
                    calls += node
                }
                return super.visitCallExpression(node)
            }
        })
        return calls
    }

    /**
     * The expression to highlight: the whole `System.getenv("X")` call. In Kotlin the UAST call expression only spans
     * `getenv("X")`; the `System.` receiver lives in a wrapping qualified expression, so widen to it when present.
     */
    private fun callAnchor(call: UCallExpression): PsiElement? {
        val parent = call.uastParent
        if (parent is UQualifiedReferenceExpression && parent.selector === call) {
            return parent.sourcePsi ?: call.sourcePsi
        }
        return call.sourcePsi
    }

    /**
     * `static main` and static initializers run before `SpringApplication.run`, so there is no `Environment` to reach
     * for and the report would be wrong. Static members of a bean class are the same: the container has not created
     * the bean yet.
     */
    private fun runsBeforeContextStart(call: UCallExpression): Boolean {
        val sourcePsi = call.sourcePsi ?: return false

        val initializer = PsiTreeUtil.getParentOfType(sourcePsi, PsiClassInitializer::class.java)
        if (initializer != null && initializer.hasModifierProperty(PsiModifier.STATIC)) return true

        val uMethod = call.getParentOfType<UMethod>()
        if (uMethod != null) {
            val psiMethod = uMethod.javaPsi
            return psiMethod.hasModifierProperty(PsiModifier.STATIC) && psiMethod.name == MAIN_METHOD
        }

        val uField = call.getParentOfType<UField>() ?: return false
        val psiField = uField.javaPsi
        return psiField is PsiModifierListOwner && psiField.hasModifierProperty(PsiModifier.STATIC)
    }

    /** The key the call reads, when it is a compile-time constant; `null` for `System.getenv()` and for variables. */
    private fun constantKey(call: UCallExpression): String? {
        val key = call.valueArguments.singleOrNull()?.evaluate() as? String ?: return null
        return key.takeIf { it.isNotBlank() }
    }

    private companion object {
        const val JAVA_LANG_SYSTEM = "java.lang.System"
        const val GETENV = "getenv"
        const val GETPROPERTY = "getProperty"
        const val MAIN_METHOD = "main"
    }
}
