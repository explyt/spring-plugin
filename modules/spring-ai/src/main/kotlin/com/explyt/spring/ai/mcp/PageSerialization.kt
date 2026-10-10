/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.core.service.conditional.ConditionPropertyValue
import com.explyt.spring.core.service.conditional.PropertyConditionValues
import com.explyt.spring.web.util.WebApplicationStack
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UResolvable
import org.jetbrains.uast.toUElementOfType

/**
 * How Spring Data writes a `PageImpl` returned by a handler of a module, and where that was decided.
 *
 * A project `@EnableSpringDataWebSupport` decides it on a servlet application: Boot's `SpringDataWebAutoConfiguration`,
 * which binds `spring.data.web.pageable.serialization-mode`, backs off when the annotation registers its own argument
 * resolvers. Without the annotation the property decides, read by the rule `@ConditionalOnProperty` is evaluated
 * with, and its absence means `DIRECT`. A reactive application never binds the property, and fails to start with the
 * annotation unless Spring MVC is on the classpath, so the annotation there leaves the mode unknown.
 *
 * @property source `DEFAULT`, the property name, `@EnableSpringDataWebSupport on <class>`, or `UNKNOWN` whenever
 * [mode] is.
 */
internal data class PageSerialization(val mode: PageSerializationMode, val source: String) {

    companion object {
        const val PROPERTY = "spring.data.web.pageable.serialization-mode"
        const val ENABLE_WEB_SUPPORT = "org.springframework.data.web.config.EnableSpringDataWebSupport"
        private const val MODE_ATTRIBUTE = "pageSerializationMode"

        private val DEFAULT = PageSerialization(PageSerializationMode.DIRECT, "DEFAULT")
        private val UNKNOWN = PageSerialization(PageSerializationMode.UNKNOWN, "UNKNOWN")

        fun of(module: Module?): PageSerialization {
            module ?: return UNKNOWN
            val webSupport = webSupportDeclaredIn(module)
            val reactive = WebApplicationStack.of(module) == WebApplicationStack.REACTIVE
            return when {
                webSupport != null && reactive -> UNKNOWN
                webSupport != null -> declared(modeDeclaredBy(webSupport), "@EnableSpringDataWebSupport on ${webSupport.qualifiedName}")
                reactive -> DEFAULT
                else -> configuredIn(module)
            }
        }

        private fun configuredIn(module: Module): PageSerialization =
            when (val value = PropertyConditionValues(module).valueOf(PROPERTY)) {
                ConditionPropertyValue.Missing -> DEFAULT
                ConditionPropertyValue.Unresolvable -> UNKNOWN
                is ConditionPropertyValue.Known -> declared(PageSerializationMode.parse(value.text), PROPERTY)
            }

        private fun declared(mode: PageSerializationMode, source: String): PageSerialization =
            if (mode == PageSerializationMode.UNKNOWN) UNKNOWN else PageSerialization(mode, source)

        private fun webSupportDeclaredIn(module: Module): PsiClass? {
            val production = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false)
            val annotation = JavaPsiFacade.getInstance(module.project).findClass(ENABLE_WEB_SUPPORT, production)
                ?: return null
            val projectSources = production.intersectWith(ProjectScope.getProjectScope(module.project))
            return AnnotatedElementsSearch.searchPsiClasses(annotation, projectSources)
                .filter { it.qualifiedName != null }
                .minByOrNull { it.qualifiedName.orEmpty() }
        }

        private fun modeDeclaredBy(psiClass: PsiClass): PageSerializationMode {
            val annotation = psiClass.toUElementOfType<UClass>()?.findAnnotation(ENABLE_WEB_SUPPORT)
                ?: return PageSerializationMode.UNKNOWN
            val value = annotation.findDeclaredAttributeValue(MODE_ATTRIBUTE) ?: return PageSerializationMode.DIRECT
            val constant = (value as? UResolvable)?.resolve() as? PsiEnumConstant
            return constant?.name?.let(PageSerializationMode::parse) ?: PageSerializationMode.UNKNOWN
        }
    }
}

internal enum class PageSerializationMode {
    DIRECT, VIA_DTO, UNKNOWN;

    companion object {
        /** Boot's relaxed enum conversion: case, `-` and `_` do not matter, so `via-dto` is `VIA_DTO`. */
        fun parse(text: String): PageSerializationMode {
            val normalized = text.trim().uppercase().replace('-', '_')
            return entries.firstOrNull { it != UNKNOWN && it.name == normalized } ?: UNKNOWN
        }
    }
}

/** The JSON a Spring Data page or slice is written as. */
internal enum class PageWireShape { PAGE_DIRECT, PAGED_MODEL, SLICE_DIRECT }
