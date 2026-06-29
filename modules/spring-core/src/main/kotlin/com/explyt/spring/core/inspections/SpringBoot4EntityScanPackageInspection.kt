/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.inspections.quickfix.RewriteAnnotationQuickFix
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiNameValuePair
import org.jetbrains.uast.UClass

/**
 * Reports the legacy `@EntityScan` annotation (from `org.springframework.boot.autoconfigure.domain`) in Spring Boot
 * 4+ projects, and offers a quick-fix that replaces it with the one from the new
 * `org.springframework.boot.persistence.autoconfigure` package, preserving any declared attributes.
 *
 * The annotation usage itself is highlighted (it is always visible in code).
 *
 * @see <a href="https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide">Spring Boot 4.0 Migration Guide</a>
 */
class SpringBoot4EntityScanPackageInspection : Spring4UastLocalInspectionTool() {

    override fun checkClass(
        uClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<out ProblemDescriptor?> {
        val uAnnotation = uClass.uAnnotations.firstOrNull { it.qualifiedName == OLD_ENTITY_SCAN } ?: return emptyArray()
        val highlightElement = uAnnotation.sourcePsi ?: return emptyArray()
        if (!isTargetResolvable(uClass)) return emptyArray()

        val attributes = reconstructAttributes(uClass)
        return arrayOf(
            manager.createProblemDescriptor(
                highlightElement,
                message("explyt.spring.inspection.boot4.entityscan"),
                isOnTheFly,
                arrayOf<LocalQuickFix>(RewriteAnnotationQuickFix(NEW_ENTITY_SCAN, uClass.javaPsi, attributes, OLD_ENTITY_SCAN)),
                ProblemHighlightType.LIKE_DEPRECATED
            )
        )
    }

    private fun isTargetResolvable(uClass: UClass): Boolean {
        return JavaPsiFacade.getInstance(uClass.javaPsi.project)
            .findClass(NEW_ENTITY_SCAN, uClass.javaPsi.resolveScope) != null
    }

    private fun reconstructAttributes(uClass: UClass): Array<PsiNameValuePair> {
        val oldAnnotation = uClass.javaPsi.modifierList?.findAnnotation(OLD_ENTITY_SCAN)
        val argsText = oldAnnotation?.parameterList?.attributes
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(", ") { it.text }
            ?: return PsiNameValuePair.EMPTY_ARRAY
        return runCatching {
            JavaPsiFacade.getInstance(uClass.javaPsi.project).elementFactory
                .createAnnotationFromText("@$NEW_ENTITY_SCAN($argsText)", uClass.javaPsi)
                .parameterList.attributes
        }.getOrDefault(PsiNameValuePair.EMPTY_ARRAY)
    }

    companion object {
        const val OLD_ENTITY_SCAN = "org.springframework.boot.autoconfigure.domain.EntityScan"
        const val NEW_ENTITY_SCAN = "org.springframework.boot.persistence.autoconfigure.EntityScan"
    }
}
