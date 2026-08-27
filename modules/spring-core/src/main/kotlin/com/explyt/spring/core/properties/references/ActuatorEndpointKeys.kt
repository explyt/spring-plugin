/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.references

import com.explyt.spring.core.JavaCoreClasses
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

/** An Actuator endpoint declared in the project: the id it publishes and the class publishing it. */
data class ActuatorEndpoint(val id: String, val psiClass: PsiClass, val defaultAccess: String?)

/**
 * Spring declares `management.endpoint.<id>.*` metadata for built-in endpoint ids, but cannot declare ids supplied by
 * application code: `PropertiesEndpointAccessResolver` formats the key from the endpoint id at runtime. The ids live
 * in the `@Endpoint` meta-annotation, which is why application endpoints are read from the code instead.
 */
object ActuatorEndpointKeys {

    const val MANAGEMENT_ENDPOINT = "management.endpoint"

    private const val DOT = "."
    private const val ID_ATTRIBUTE = "id"
    private const val DEFAULT_ACCESS_ATTRIBUTE = "defaultAccess"
    private const val DURATION = "java.time.Duration"

    /** The default of `@Endpoint#defaultAccess`, used when the annotation leaves the attribute out. */
    const val UNRESTRICTED = "UNRESTRICTED"

    /** The key tails Spring resolves per endpoint id, each mapped to the type of the value it takes. */
    val KEY_TYPES: Map<String, String> = mapOf(
        "access" to SpringCoreClasses.ACTUATOR_ENDPOINT_ACCESS,
        "enabled" to JavaCoreClasses.BOOLEAN,
        "cache.time-to-live" to DURATION
    )

    /** The endpoints [module] declares, by id. An id may be declared twice, which is a project error, not ours. */
    fun endpointsById(module: Module): Map<String, List<ActuatorEndpoint>> {
        if (DumbService.isDumb(module.project)) return emptyMap()
        val accessAvailable = JavaPsiFacade.getInstance(module.project)
            .findClass(
                SpringCoreClasses.ACTUATOR_ENDPOINT_ACCESS,
                GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
            ) != null
        return CachedValuesManager.getManager(module.project).getCachedValue(module) {
            CachedValueProvider.Result(
                findEndpoints(module, accessAvailable),
                ModificationTrackerManager.getInstance(module.project).getUastModelAndLibraryTracker()
            )
        }
    }

    /**
     * The references covering [propertyKey], one per segment, or none when the key names no declared endpoint.
     *
     * Each segment answers a different question — which group, which endpoint, which value — so each gets its own
     * range and its own single target rather than one reference offering all three.
     */
    fun referencesForKey(element: PsiElement, module: Module, propertyKey: String): Array<PsiReference> {
        val parsed = parse(propertyKey) ?: return PsiReference.EMPTY_ARRAY
        val endpoints = endpointsById(module)[parsed.id]?.takeIf { it.isNotEmpty() } ?: return PsiReference.EMPTY_ARRAY

        // In YAML the key is split across nesting levels, so this element carries only a tail of the full key: a
        // segment absent from the element text belongs to an enclosing key and is covered there.
        val elementText = element.text
        var searchFrom = 0
        fun rangeOf(segment: String): TextRange? {
            val start = elementText.indexOf(segment, searchFrom)
            if (start < 0) return null
            searchFrom = start + segment.length
            return TextRange(start, start + segment.length)
        }

        val references = mutableListOf<PsiReference>()
        rangeOf(MANAGEMENT_ENDPOINT)?.let {
            references += MetaConfigurationKeyReference(element, module, MANAGEMENT_ENDPOINT, it)
        }
        rangeOf(parsed.id)?.let { references += ActuatorEndpointIdReference(element, it, endpoints) }
        parsed.tail?.let { tail ->
            val localTail = tail.substringAfterLast(DOT)
            rangeOf(localTail)?.let {
                references += ActuatorEndpointValueTypeReference(element, it, module, KEY_TYPES[tail])
            }
        }
        return references.toTypedArray()
    }

    private fun findEndpoints(module: Module, accessAvailable: Boolean): Map<String, List<ActuatorEndpoint>> {
        // `@Endpoint` is the linking meta-annotation of `@WebEndpoint`, `@JmxEndpoint` and the controller endpoints,
        // so matching it covers every specialization, including ones a project or a future Boot release declares.
        val annotations = MetaAnnotationUtil
            .getAnnotationTypesWithChildren(module, SpringCoreClasses.ACTUATOR_ENDPOINT, false)

        val result = LinkedHashMap<String, MutableList<ActuatorEndpoint>>()
        for (annotationClass in annotations) {
            val annotationName = annotationClass.qualifiedName ?: continue
            // An endpoint class is not a bean by virtue of the annotation - `@Endpoint` carries only `@Reflective` -
            // so it is found by annotation search, not through the bean model.
            AnnotatedElementsSearch.searchPsiClasses(annotationClass, module.moduleScope).forEach { psiClass ->
                toEndpoint(psiClass, annotationName, accessAvailable)
                    ?.let { result.getOrPut(it.id) { mutableListOf() } += it }
            }
        }
        return result
    }

    private fun toEndpoint(psiClass: PsiClass, annotationName: String, accessAvailable: Boolean): ActuatorEndpoint? {
        val annotation = psiClass.getAnnotation(annotationName) ?: return null
        // `@Endpoint` and `@JmxEndpoint` declare `id() default ""`, so a class may carry no id; a key built from it
        // would read `management.endpoint..access`, which Spring resolves for nothing.
        val id = AnnotationUtil.getStringAttributeValue(annotation, ID_ATTRIBUTE)?.takeIf { it.isNotBlank() }
            ?: return null
        val defaultAccess = if (accessAvailable) {
            annotation.findAttributeValue(DEFAULT_ACCESS_ATTRIBUTE)?.text
                ?.substringAfterLast(DOT)?.takeIf { it.isNotBlank() } ?: UNRESTRICTED
        } else {
            null
        }
        return ActuatorEndpoint(id, psiClass, defaultAccess)
    }

    private fun parse(propertyKey: String): ParsedKey? {
        val prefix = "$MANAGEMENT_ENDPOINT$DOT"
        if (!propertyKey.startsWith(prefix)) return null
        val rest = propertyKey.substring(prefix.length)
        val id = rest.substringBefore(DOT).takeIf { it.isNotEmpty() } ?: return null
        val tail = rest.removePrefix(id).removePrefix(DOT).takeIf { it.isNotEmpty() }
        // A tail Spring does not resolve stays unhandled here, so it is still reported as an unresolved key.
        if (tail != null && tail !in KEY_TYPES) return null
        return ParsedKey(id, tail)
    }

    private data class ParsedKey(val id: String, val tail: String?)
}

/** The `<id>` segment of `management.endpoint.<id>.*`: the endpoint class publishing that id. */
class ActuatorEndpointIdReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val endpoints: List<ActuatorEndpoint>
) : PsiReferenceBase.Poly<PsiElement>(element, rangeInElement, true) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> =
        PsiElementResolveResult.createResults(endpoints.map { it.psiClass })
}

/** The tail segment of `management.endpoint.<id>.<tail>`: the type of the value the key takes. */
class ActuatorEndpointValueTypeReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val module: Module,
    private val valueType: String?
) : PsiReferenceBase.Poly<PsiElement>(element, rangeInElement, true) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val type = valueType ?: return ResolveResult.EMPTY_ARRAY
        val psiClass = JavaPsiFacade.getInstance(module.project)
            .findClass(type, GlobalSearchScope.allScope(module.project)) ?: return ResolveResult.EMPTY_ARRAY
        return PsiElementResolveResult.createResults(psiClass)
    }
}
