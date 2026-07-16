/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses.AUTOWIRED
import com.explyt.spring.core.SpringCoreClasses.COMPONENT
import com.explyt.spring.core.SpringCoreClasses.CONFIGURATION_PROPERTIES
import com.explyt.spring.core.SpringCoreClasses.CONSTRUCTOR_BINDING
import com.explyt.spring.core.util.SpringBootUtil
import com.explyt.util.AddParameterMethodAnnotationKotlinFix
import com.explyt.util.ExplytPsiUtil.getHighlightRange
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiClassType
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.uast.UClass

/**
 * Reports a Spring stereotype bean injected into the constructor of a `@ConfigurationProperties` class without
 * `@Autowired` in a Spring Boot 3+ project.
 *
 * Since Spring Boot 3.0 a single-constructor `@ConfigurationProperties` class is bound through its constructor
 * automatically, so a constructor parameter is treated as a property to bind unless it is explicitly marked with
 * `@Autowired`. When the parameter type is itself a Spring bean (meta-annotated with `@Component`, i.e.
 * `@Service`/`@Repository`/`@Configuration`/...), it is almost certainly a dependency that must be annotated with
 * `@Autowired` to avoid being mistaken for a property.
 *
 * @see <a href="https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide#constructingbinding-no-longer-needed-at-the-type-level">Spring Boot 3.0 Migration Guide</a>
 */
class SpringBoot3ConfigPropertiesAutowiredInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkClass(
        uClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<out ProblemDescriptor?> {
        if (uClass.lang != KotlinLanguage.INSTANCE) return emptyArray()
        val javaPsi = uClass.javaPsi
        if (!javaPsi.isMetaAnnotatedBy(CONFIGURATION_PROPERTIES)) return emptyArray()
        // Constructor binding is only automatic for a single-constructor class.
        val singleConstructor = javaPsi.constructors.singleOrNull() ?: return emptyArray()
        // Already disambiguated by the developer.
        if (singleConstructor.isMetaAnnotatedBy(AUTOWIRED)) return emptyArray()
        if (singleConstructor.isMetaAnnotatedBy(CONSTRUCTOR_BINDING)) return emptyArray()
        if (!SpringBootUtil.isAtLeastSpringBoot3(javaPsi)) return emptyArray()

        val problems = mutableListOf<ProblemDescriptor>()
        for (method in uClass.methods) {
            if (!method.isConstructor) continue
            for (parameter in method.uastParameters) {
                val ktParameter = parameter.sourcePsi as? KtParameter ?: continue
                val parameterClass = (parameter.type as? PsiClassType)
                    ?.let { PsiTypesUtil.getPsiClass(it) } ?: continue
                if (!parameterClass.isMetaAnnotatedBy(COMPONENT)) continue

                problems += manager.createProblemDescriptor(
                    ktParameter,
                    ktParameter.getHighlightRange(),
                    SpringCoreBundle.message("explyt.spring.inspection.boot3.configprops.autowired"),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    isOnTheFly,
                    AddParameterMethodAnnotationKotlinFix(AUTOWIRED)
                )
            }
        }
        return problems.toTypedArray()
    }
}
