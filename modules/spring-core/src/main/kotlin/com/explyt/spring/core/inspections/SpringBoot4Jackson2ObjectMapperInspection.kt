/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.SpringCoreClasses.AUTOWIRED
import com.explyt.spring.core.SpringCoreClasses.BEAN
import com.explyt.spring.core.SpringCoreClasses.COMPONENT
import com.explyt.spring.core.inspections.quickfix.ReplaceTypeQuickFix
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.uast.UField
import org.jetbrains.uast.UMethod

/**
 * Reports an injection point (autowired field, constructor parameter of a Spring bean, or `@Bean` method parameter)
 * whose type is the Jackson 2 `com.fasterxml.jackson.databind.ObjectMapper` in a Spring Boot 4+ project.
 *
 * Spring Boot 4 ships Jackson 3: `JacksonAutoConfiguration` registers only Jackson 3 beans (the primary mapper is
 * `tools.jackson.databind.json.JsonMapper`), and no Jackson 2 `ObjectMapper` bean exists. Such an injection compiles
 * (Jackson 2 usually remains a transitive dependency) but fails at runtime with `NoSuchBeanDefinitionException`.
 *
 * The inspection is skipped when the project opted into the deprecated Jackson 2 support
 * (`spring-boot-jackson2`, which auto-configures Jackson 2 beans again).
 *
 * @see <a href="https://github.com/explyt/spring-plugin/issues/246">Issue #246</a>
 */
class SpringBoot4Jackson2ObjectMapperInspection : Spring4UastLocalInspectionTool() {

    override fun isAvailableForFile(file: PsiFile): Boolean {
        return super.isAvailableForFile(file)
                // Nothing to flag unless the legacy Jackson 2 mapper is on the classpath.
                && isClassAvailable(file, JACKSON2_OBJECT_MAPPER)
                // Jackson 3 must actually be the replacement offered.
                && isClassAvailable(file, JACKSON3_JSON_MAPPER)
                // The project explicitly opted into Jackson 2 auto-configuration: ObjectMapper beans exist again.
                && !isClassAvailable(file, JACKSON2_AUTO_CONFIGURATION)
    }

    override fun checkField(field: UField, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor> {
        val javaPsi = field.javaPsi as? PsiModifierListOwner ?: return emptyArray()
        if (!javaPsi.isMetaAnnotatedBy(AUTOWIRED)) return emptyArray()
        val problem = checkInjectedType(field.typeReference?.sourcePsi, field.type.canonicalText, manager, isOnTheFly)
        return problem?.let { arrayOf(it) } ?: emptyArray()
    }

    override fun checkMethod(method: UMethod, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor> {
        if (!isInjectionMethod(method)) return emptyArray()
        val problems = mutableListOf<ProblemDescriptor>()
        for (parameter in method.uastParameters) {
            checkInjectedType(parameter.typeReference?.sourcePsi, parameter.type.canonicalText, manager, isOnTheFly)
                ?.let { problems += it }
        }
        return problems.toTypedArray()
    }

    /**
     * An injection method is a `@Bean` method, an `@Autowired` method/constructor, or the single constructor of a
     * `@Component`-stereotyped class.
     */
    private fun isInjectionMethod(method: UMethod): Boolean {
        val javaPsi = method.javaPsi
        if (javaPsi.isMetaAnnotatedBy(BEAN) || javaPsi.isMetaAnnotatedBy(AUTOWIRED)) return true
        if (!method.isConstructor) return false
        val containingClass = javaPsi.containingClass ?: return false
        return containingClass.isMetaAnnotatedBy(COMPONENT) && containingClass.constructors.size == 1
    }

    private fun checkInjectedType(
        typeSourcePsi: PsiElement?,
        canonicalText: String?,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): ProblemDescriptor? {
        if (typeSourcePsi == null || canonicalText != JACKSON2_OBJECT_MAPPER) return null
        return manager.createProblemDescriptor(
            typeSourcePsi,
            message("explyt.spring.inspection.boot4.jackson2.objectmapper"),
            isOnTheFly,
            arrayOf<LocalQuickFix>(ReplaceTypeQuickFix(JACKSON3_JSON_MAPPER)),
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        )
    }

    companion object {
        const val JACKSON2_OBJECT_MAPPER = "com.fasterxml.jackson.databind.ObjectMapper"
        const val JACKSON3_JSON_MAPPER = "tools.jackson.databind.json.JsonMapper"
        const val JACKSON2_AUTO_CONFIGURATION = "org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration"
    }
}
