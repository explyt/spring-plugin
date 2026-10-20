/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.core.service.conditional.ConditionPropertyValue
import com.explyt.spring.core.service.conditional.PropertyConditionValues
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiType
import com.intellij.psi.PsiWildcardType

internal object HateoasSchemas {
    const val ENTITY_MODEL = "org.springframework.hateoas.EntityModel"
    const val COLLECTION_MODEL = "org.springframework.hateoas.CollectionModel"
    const val PAGED_MODEL = "org.springframework.hateoas.PagedModel"
    const val EMBEDDED = "_embedded"
    const val MAP = "java.util.Map"
    private const val PAGE_METADATA = "org.springframework.hateoas.PagedModel.PageMetadata"
    private const val LINK = "org.springframework.hateoas.Link"
    private const val RELATION = "org.springframework.hateoas.server.core.Relation"
    const val STRING = "java.lang.String"

    val MODELS = setOf(ENTITY_MODEL, COLLECTION_MODEL, PAGED_MODEL)

    fun links(): DtoFieldJson = field("_links", "$MAP<$STRING,$LINK>")

    fun pageMetadata(depth: Int): DtoFieldJson = field(
        "page", PAGE_METADATA,
        DtoSchemaJson(
            className = PAGE_METADATA,
            fields = listOf("size", "totalElements", "totalPages", "number").map { field(it, "long") },
        ).takeIf { depth > 1 },
    )

    /**
     * The `_embedded` key of a collection of [element]: the `@Relation(collectionRelation)` of its content type, else
     * `uncapitalize(simpleName) + "List"` - unless Evo Inflector [inflects] the default, which is then not derived.
     */
    fun collectionRelationOf(element: PsiType, inflects: Boolean): String? {
        val item = contentClassOf(element) ?: return null
        return declaredCollectionRelationOf(item)
            ?: item.name?.takeUnless { inflects }?.let { it.replaceFirstChar(Char::lowercaseChar) + "List" }
    }

    private fun contentClassOf(type: PsiType): PsiClass? {
        val classType = (if (type is PsiWildcardType) type.bound else type) as? PsiClassType ?: return null
        val resolved = classType.resolve() ?: return null
        if (resolved.qualifiedName != ENTITY_MODEL) return resolved
        return classType.parameters.firstOrNull()?.let(::contentClassOf)
    }

    private fun declaredCollectionRelationOf(item: PsiClass): String? =
        AnnotationUtil.findAnnotation(item, RELATION)
            ?.let { AnnotationUtil.getStringAttributeValue(it, "collectionRelation") }
            ?.takeIf { it.isNotEmpty() }

    fun field(name: String, type: String, nested: DtoSchemaJson? = null): DtoFieldJson =
        DtoFieldJson(name = name, declaredName = null, type = type, nullable = false, nested = nested)
}

/**
 * How Spring HATEOAS writes a representation model returned by a handler of a module.
 *
 * Boot makes HAL the default for any `Accept` and for `application/json`; with
 * `spring.hateoas.use-hal-as-default-json-media-type` set to `false`, `application/json` gets plain JSON on Boot 3
 * and `406` on Boot 4, so the HAL shape is then only assumed. Evo Inflector on the runtime classpath pluralises the
 * default collection relation by English rules that are not reproduced here.
 */
internal data class HalSerialization(val halAssumed: Boolean, val inflectsRelations: Boolean) {

    companion object {
        const val PROPERTY = "spring.hateoas.use-hal-as-default-json-media-type"
        private const val EVO_INFLECTOR = "org.atteo.evo.inflector.English"

        fun of(module: Module?): HalSerialization {
            module ?: return HalSerialization(halAssumed = true, inflectsRelations = true)
            return HalSerialization(halAssumed = !isHalDefault(module), inflectsRelations = inflects(module))
        }

        private fun isHalDefault(module: Module): Boolean =
            when (val value = PropertyConditionValues(module).valueOf(PROPERTY)) {
                ConditionPropertyValue.Missing -> true
                ConditionPropertyValue.Unresolvable -> false
                is ConditionPropertyValue.Known -> value.text.isBlank() || value.text.trim().equals("true", ignoreCase = true)
            }

        private fun inflects(module: Module): Boolean =
            JavaPsiFacade.getInstance(module.project).findClass(EVO_INFLECTOR, module.getModuleRuntimeScope(false)) != null
    }
}
