/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringProperties
import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.service.conditional.ConditionPropertyValue
import com.explyt.spring.core.service.conditional.PropertyConditionValues
import com.explyt.spring.web.util.WebApplicationStack
import com.explyt.util.ExplytPsiUtil.resolveUAnnotationType
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UResolvable
import org.jetbrains.uast.toUElementOfType

/**
 * How Spring Data writes a `PageImpl` returned by a handler of a module, and where that was decided.
 *
 * An `@EnableSpringDataWebSupport` class the context registers ([WebSupportDeclarations.reached]) decides it on a
 * servlet application: Boot's `SpringDataWebAutoConfiguration`,
 * which binds `spring.data.web.pageable.serialization-mode`, backs off when the annotation registers its own argument
 * resolvers. Without the annotation the property decides, read by the rule `@ConditionalOnProperty` is evaluated
 * with, and its absence means `DIRECT`. A reactive application never binds the property, and fails to start with the
 * annotation unless Spring MVC is on the classpath, so the annotation there leaves the mode unknown. A library
 * auto-configuration that declares a mode other than `DIRECT` and that project code does not reach leaves the mode
 * unknown when registering it would change the answer.
 *
 * @property source `DEFAULT`, the property name, `@EnableSpringDataWebSupport on <class>`, or `UNKNOWN` whenever
 * [mode] is.
 */
internal data class PageSerialization(val mode: PageSerializationMode, val source: String) {

    companion object {
        const val PROPERTY = "spring.data.web.pageable.serialization-mode"
        const val ENABLE_WEB_SUPPORT = "org.springframework.data.web.config.EnableSpringDataWebSupport"

        private const val MODE_ATTRIBUTE = "pageSerializationMode"
        private const val ALLOW_BEAN_DEFINITION_OVERRIDING = "spring.main.allow-bean-definition-overriding"

        private val DEFAULT = PageSerialization(PageSerializationMode.DIRECT, "DEFAULT")
        private val UNKNOWN = PageSerialization(PageSerializationMode.UNKNOWN, "UNKNOWN")

        fun of(module: Module?): PageSerialization {
            module ?: return UNKNOWN
            val declarations = WebSupportDeclarations.of(module)
            val webSupport = MetaAnnotationsHolder.of(module, ENABLE_WEB_SUPPORT)
            val decided = decidedBy(declarations.reached, module)
            val mayRegister = declarations.unreachedAutoConfigurations
                .filter { modeDeclaredBy(it, webSupport) != PageSerializationMode.DIRECT }
            if (mayRegister.isEmpty()) return decided
            val registered = decidedBy((declarations.reached + mayRegister).sortedBy { it.qualifiedName }, module)
            return if (registered.mode == decided.mode) decided else UNKNOWN
        }

        private fun decidedBy(declarations: List<PsiClass>, module: Module): PageSerialization {
            val reactive = WebApplicationStack.of(module) == WebApplicationStack.REACTIVE
            return when {
                declarations.isNotEmpty() && reactive -> UNKNOWN
                declarations.isNotEmpty() -> decisiveOf(declarations, module)
                reactive -> DEFAULT
                else -> configuredIn(module)
            }
        }

        private fun decisiveOf(declarations: List<PsiClass>, module: Module): PageSerialization {
            val webSupport = MetaAnnotationsHolder.of(module, ENABLE_WEB_SUPPORT)
            val modes = declarations.map { it to modeDeclaredBy(it, webSupport) }
            val viaDto = modes.filter { it.second == PageSerializationMode.VIA_DTO }
            val unreadable = modes.filter { it.second == PageSerializationMode.UNKNOWN }
            val mayCompete = viaDto.isNotEmpty() && viaDto.size + unreadable.size > 1
            if (mayCompete && !allowsBeanDefinitionOverriding(module)) return UNKNOWN
            val (decisive, mode) = viaDto.firstOrNull() ?: unreadable.firstOrNull() ?: modes.first()
            return declared(mode, "@EnableSpringDataWebSupport on ${decisive.qualifiedName}")
        }

        private fun allowsBeanDefinitionOverriding(module: Module): Boolean =
            when (val value = PropertyConditionValues(module).valueOf(ALLOW_BEAN_DEFINITION_OVERRIDING)) {
                is ConditionPropertyValue.Known -> value.text.trim().equals("true", ignoreCase = true)
                ConditionPropertyValue.Missing, ConditionPropertyValue.Unresolvable -> false
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

        private fun modeDeclaredBy(psiClass: PsiClass, webSupport: MetaAnnotationsHolder): PageSerializationMode {
            val annotation = psiClass.toUElementOfType<UClass>()?.uAnnotations?.firstOrNull(webSupport::contains)
                ?: return PageSerializationMode.UNKNOWN
            if (leavesTheModeToAnUnreadableDefault(annotation, webSupport)) return PageSerializationMode.UNKNOWN
            val values = webSupport.getAnnotationMemberValues(annotation, setOf(MODE_ATTRIBUTE))
            if (values.isEmpty()) return PageSerializationMode.DIRECT
            val constant = (values.singleOrNull() as? UResolvable)?.resolve() as? PsiEnumConstant
            return constant?.name?.let(PageSerializationMode::parse) ?: PageSerializationMode.UNKNOWN
        }

        private fun leavesTheModeToAnUnreadableDefault(
            annotation: UAnnotation,
            webSupport: MetaAnnotationsHolder,
        ): Boolean {
            val fqn = annotation.qualifiedName ?: return false
            val composed = annotationTypeOf(annotation, fqn) ?: return false
            val setAttributes = annotation.attributeValues.mapTo(mutableSetOf()) { it.name ?: SpringProperties.VALUE }
            return hidesTheModeOnThePath(composed, setAttributes, false, webSupport, emptySet())
        }

        private fun hidesTheModeOnThePath(
            composed: PsiClass,
            setAttributes: Set<String>,
            modeSetAbove: Boolean,
            webSupport: MetaAnnotationsHolder,
            visited: Set<String>,
        ): Boolean {
            val fqn = composed.qualifiedName ?: return false
            if (fqn == ENABLE_WEB_SUPPORT || fqn in visited) return false
            if (declaresAConventionModeAttribute(composed)) return true
            val modeAliases = composed.methods.map { it.name }.filter { name ->
                webSupport.isAttributeRelatedWith(fqn, name, ENABLE_WEB_SUPPORT, setOf(MODE_ATTRIBUTE))
            }
            val modeSet = modeSetAbove || modeAliases.any { it in setAttributes }
            if (modeAliases.isNotEmpty() && !modeSet) return true
            return composed.annotations.filter(webSupport::contains).any { meta ->
                val metaType = meta.resolveUAnnotationType() ?: return@any false
                val metaSetAttributes = meta.parameterList.attributes
                    .mapTo(mutableSetOf()) { it.name ?: SpringProperties.VALUE }
                hidesTheModeOnThePath(metaType, metaSetAttributes, modeSet, webSupport, visited + fqn)
            }
        }

        private fun declaresAConventionModeAttribute(composed: PsiClass): Boolean =
            composed.findMethodsByName(MODE_ATTRIBUTE, false).any { !it.hasAnnotation(SpringCoreClasses.ALIAS_FOR) }

        private fun annotationTypeOf(annotation: UAnnotation, fqn: String): PsiClass? =
            annotation.javaPsi?.resolveUAnnotationType()
                ?: annotation.sourcePsi?.let {
                    JavaPsiFacade.getInstance(it.project).findClass(fqn, GlobalSearchScope.allScope(it.project))
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
internal enum class PageWireShape {
    PAGE_DIRECT, PAGED_MODEL, SLICE_DIRECT;

    companion object {
        const val DECLARED_TYPE = "DECLARED_TYPE"
    }
}
