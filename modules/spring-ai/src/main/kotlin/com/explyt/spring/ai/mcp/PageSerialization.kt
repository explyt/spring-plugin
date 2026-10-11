/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.service.conditional.ConditionPropertyValue
import com.explyt.spring.core.service.conditional.PropertyConditionValues
import com.explyt.spring.web.util.WebApplicationStack
import com.intellij.codeInsight.MetaAnnotationUtil
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
        const val DECLARED_TYPE = "DECLARED_TYPE"
        private const val MODE_ATTRIBUTE = "pageSerializationMode"

        private val DEFAULT = PageSerialization(PageSerializationMode.DIRECT, "DEFAULT")
        private val UNKNOWN = PageSerialization(PageSerializationMode.UNKNOWN, "UNKNOWN")

        fun of(module: Module?): PageSerialization {
            module ?: return UNKNOWN
            val declarations = webSupportDeclaredIn(module)
            val reactive = WebApplicationStack.of(module) == WebApplicationStack.REACTIVE
            return when {
                declarations.isNotEmpty() && reactive -> UNKNOWN
                declarations.isNotEmpty() -> decisiveOf(declarations, MetaAnnotationsHolder.of(module, ENABLE_WEB_SUPPORT))
                reactive -> DEFAULT
                else -> configuredIn(module)
            }
        }

        private fun decisiveOf(declarations: List<PsiClass>, webSupport: MetaAnnotationsHolder): PageSerialization {
            val modes = declarations.map { it to modeDeclaredBy(it, webSupport) }
            val (decisive, mode) = modes.firstOrNull { it.second == PageSerializationMode.VIA_DTO }
                ?: modes.firstOrNull { it.second == PageSerializationMode.UNKNOWN }
                ?: modes.first()
            return declared(mode, "@EnableSpringDataWebSupport on ${decisive.qualifiedName}")
        }

        private fun configuredIn(module: Module): PageSerialization =
            when (val value = PropertyConditionValues(module).valueOf(PROPERTY)) {
                ConditionPropertyValue.Missing -> DEFAULT
                ConditionPropertyValue.Unresolvable -> UNKNOWN
                is ConditionPropertyValue.Known ->
                    if (value.text.isBlank()) DEFAULT else declared(PageSerializationMode.parse(value.text), PROPERTY)
            }

        private fun declared(mode: PageSerializationMode, source: String): PageSerialization =
            if (mode == PageSerializationMode.UNKNOWN) UNKNOWN else PageSerialization(mode, source)

        private fun webSupportDeclaredIn(module: Module): List<PsiClass> {
            val production = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false)
            JavaPsiFacade.getInstance(module.project).findClass(ENABLE_WEB_SUPPORT, production) ?: return emptyList()
            val projectSources = production.intersectWith(ProjectScope.getProjectScope(module.project))
            return MetaAnnotationUtil.getAnnotationTypesWithChildren(module, ENABLE_WEB_SUPPORT, false)
                .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, projectSources) }
                .filter { !it.isAnnotationType && it.qualifiedName != null }
                .distinctBy { it.qualifiedName }
                .sortedBy { it.qualifiedName }
        }

        private fun modeDeclaredBy(psiClass: PsiClass, webSupport: MetaAnnotationsHolder): PageSerializationMode {
            val annotation = psiClass.toUElementOfType<UClass>()?.uAnnotations?.firstOrNull(webSupport::contains)
                ?: return PageSerializationMode.UNKNOWN
            val values = webSupport.getAnnotationMemberValues(annotation, setOf(MODE_ATTRIBUTE))
            if (values.isEmpty()) return PageSerializationMode.DIRECT
            val constant = (values.singleOrNull() as? UResolvable)?.resolve() as? PsiEnumConstant
            return constant?.name?.let(PageSerializationMode::parse) ?: PageSerializationMode.UNKNOWN
        }
    }
}

internal enum class PageSerializationMode {
    DIRECT, VIA_DTO, UNKNOWN;

    companion object {
        fun parse(text: String): PageSerializationMode {
            val lenient = lenientFormOf(text)
            return entries.firstOrNull { it != UNKNOWN && lenientFormOf(it.name) == lenient } ?: UNKNOWN
        }

        private fun lenientFormOf(text: String): String = text.filter { it.isLetterOrDigit() }.lowercase()
    }
}

/** The JSON a Spring Data page or slice is written as. */
internal enum class PageWireShape { PAGE_DIRECT, PAGED_MODEL, SLICE_DIRECT }
