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
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiFile
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

    override fun isAvailableForFile(file: PsiFile): Boolean {
        // Only the replacement has to be resolvable: the Boot 4 upgrade relocates the legacy annotation, and the
        // stale source that still references the old package is exactly what must be reported.
        return super.isAvailableForFile(file) && isClassAvailable(file, NEW_ENTITY_SCAN)
    }

    override fun checkClass(
        uClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<out ProblemDescriptor?> {
        val uAnnotation = uClass.uAnnotations
            .firstOrNull { legacyAnnotationFqn(it, LEGACY_FQNS) != null } ?: return emptyArray()
        val highlightElement = uAnnotation.sourcePsi ?: return emptyArray()

        val attributes = reconstructAttributes(uClass, uAnnotation.javaPsi)
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

    /**
     * The annotation's own `javaPsi` is used rather than a lookup by FQN on the owner, which a relocated legacy class
     * cannot match once it no longer resolves.
     */
    private fun reconstructAttributes(uClass: UClass, oldAnnotation: PsiAnnotation?): Array<PsiNameValuePair> {
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

        private val LEGACY_FQNS = setOf(OLD_ENTITY_SCAN)
    }
}
