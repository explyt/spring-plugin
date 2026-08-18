/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isTestFiles
import com.intellij.codeInsight.intention.AddAnnotationFix
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiType
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.uast.UClass

/**
 * Reports a `@SpringBootTest` class that injects a web test client (`MockMvc` / `TestRestTemplate`) without the
 * auto-configuration annotation that Spring Boot 4 now requires, and offers a quick-fix that adds it.
 *
 * In Spring Boot 4.0 `@SpringBootTest` no longer auto-configures `MockMvc` / `TestRestTemplate`; the corresponding
 * `@AutoConfigure...` annotation must be added explicitly.
 *
 * @see <a href="https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide">Spring Boot 4.0 Migration Guide</a>
 */
class SpringBoot4TestWebClientInspection : Spring4UastLocalInspectionTool() {

    override fun isAvailableForFile(file: PsiFile): Boolean {
        return super.isAvailableForFile(file) && isTestFiles(file)
    }

    override fun checkClass(
        uClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<out ProblemDescriptor?> {
        val javaPsi = uClass.javaPsi
        if (!javaPsi.isMetaAnnotatedBy(SPRING_BOOT_TEST)) return emptyArray()

        val highlightElement = (uClass.sourcePsi as? PsiNameIdentifierOwner)?.nameIdentifier ?: return emptyArray()

        val problems = mutableListOf<ProblemDescriptor>()
        for ((clientFqn, autoConfig) in REQUIREMENTS) {
            if (!hasInjectionPointOfType(uClass, clientFqn)) continue
            if (javaPsi.isMetaAnnotatedBy(autoConfig.annotationFqn)) continue

            val fix = AddAnnotationFix(autoConfig.annotationFqn, javaPsi)
            problems += manager.createProblemDescriptor(
                highlightElement,
                message("explyt.spring.inspection.boot4.test.webclient", autoConfig.shortName, clientShortName(clientFqn)),
                isOnTheFly,
                arrayOf(fix),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            )
        }
        return problems.toTypedArray()
    }

    /**
     * The client can be injected into a field (field injection), a constructor parameter (Java constructor
     * injection, the idiomatic form in Kotlin/Java tests) or a method parameter (setter and test-method injection).
     */
    private fun hasInjectionPointOfType(uClass: UClass, typeFqn: String): Boolean {
        return uClass.fields.any { isTypeOf(it.type, typeFqn) }
                || uClass.methods.any { method -> method.uastParameters.any { isTypeOf(it.type, typeFqn) } }
    }

    private fun isTypeOf(type: PsiType, typeFqn: String): Boolean {
        val classType = type as? PsiClassType ?: return false
        return PsiTypesUtil.getPsiClass(classType)?.qualifiedName == typeFqn
    }

    private fun clientShortName(fqn: String): String = fqn.substringAfterLast('.')

    private data class AutoConfig(val annotationFqn: String) {
        val shortName: String get() = annotationFqn.substringAfterLast('.')
    }

    companion object {
        private const val SPRING_BOOT_TEST = "org.springframework.boot.test.context.SpringBootTest"

        private const val MOCK_MVC = "org.springframework.test.web.servlet.MockMvc"
        private const val TEST_REST_TEMPLATE = "org.springframework.boot.resttestclient.TestRestTemplate"

        private const val AUTO_CONFIGURE_MOCK_MVC =
            "org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc"
        private const val AUTO_CONFIGURE_TEST_REST_TEMPLATE =
            "org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate"

        // injected web-client type FQN -> required @AutoConfigure... annotation
        private val REQUIREMENTS: Map<String, AutoConfig> = mapOf(
            MOCK_MVC to AutoConfig(AUTO_CONFIGURE_MOCK_MVC),
            TEST_REST_TEMPLATE to AutoConfig(AUTO_CONFIGURE_TEST_REST_TEMPLATE),
        )
    }
}
