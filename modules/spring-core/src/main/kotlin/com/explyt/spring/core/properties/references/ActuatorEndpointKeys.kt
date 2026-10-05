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
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

/**
 * An Actuator endpoint declared in the project: the id it publishes and the class publishing it.
 *
 * [defaultAccess] is the name of the `@Endpoint#defaultAccess` constant, `null` before Boot 3.4 introduced it;
 * [enabledByDefault] is the legacy `@Endpoint#enableByDefault`, which Boot 3.x still honours and Boot 4 removed.
 */
data class ActuatorEndpoint(
    val id: String,
    val psiClass: PsiClass,
    val defaultAccess: String?,
    val enabledByDefault: Boolean = true,
)

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
    private const val ENABLE_BY_DEFAULT_ATTRIBUTE = "enableByDefault"
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
        return CachedValuesManager.getManager(module.project).getCachedValue(module) {
            CachedValueProvider.Result(
                findEndpoints(module.project, projectDiscovery(module)),
                ModificationTrackerManager.getInstance(module.project).getUastModelAndLibraryTracker()
            )
        }
    }

    /**
     * The endpoints the libraries on [module]'s classpath declare, by id: Boot's own `health`, `info`, `env`... and any
     * a starter adds. They are endpoints the application serves, so the endpoint model lists them; they are never
     * a source of `management.endpoint.<id>.*` keys, which their library already ships as metadata.
     *
     * A library without the Actuator auto-configuration registers none of them, so none are reported for it.
     */
    fun libraryEndpointsById(module: Module): Map<String, List<ActuatorEndpoint>> {
        if (DumbService.isDumb(module.project)) return emptyMap()
        return CachedValuesManager.getManager(module.project).getCachedValue(module) {
            CachedValueProvider.Result(
                findLibraryEndpoints(module),
                ModificationTrackerManager.getInstance(module.project).getLibraryTracker()
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
        // A whole dotted segment only: `web` must not be found inside `web-access` of `management.endpoint.web-access`.
        fun rangeOf(segment: String): TextRange? {
            var start = elementText.indexOf(segment, searchFrom)
            while (start >= 0 && !elementText.isSegmentAt(start, segment.length)) {
                start = elementText.indexOf(segment, start + 1)
            }
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
            val valueType = KEY_TYPES[tail]
            if (valueType != null && (tail != "access" || endpoints.any { it.defaultAccess != null })) {
                val localTail = tail.substringAfterLast(DOT)
                rangeOf(localTail)?.let {
                    references += ActuatorEndpointValueTypeReference(element, it, module, valueType)
                }
            }
        }
        return references.toTypedArray()
    }

    private fun findLibraryEndpoints(module: Module): Map<String, List<ActuatorEndpoint>> =
        libraryDiscovery(module)?.let { findEndpoints(module.project, it) } ?: emptyMap()

    private class Discovery(
        val endpointScope: GlobalSearchScope,
        val accessScope: GlobalSearchScope,
        val annotations: Collection<PsiClass>,
    )

    /**
     * The endpoints the project declares: the module with its dependency modules - a shared starter may declare
     * `@Endpoint` next to the application module holding `application.yaml` (#382) - and never its libraries, whose
     * endpoints already ship metadata. The `Access` probe does read the libraries, since the class ships in a jar.
     */
    private fun projectDiscovery(module: Module) = Discovery(
        endpointScope = GlobalSearchScope.moduleWithDependenciesScope(module),
        accessScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module),
        annotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(module, SpringCoreClasses.ACTUATOR_ENDPOINT, false)
    )

    private fun libraryDiscovery(module: Module): Discovery? {
        val libraries = module.getModuleRuntimeScope(false).intersectWith(ProjectScope.getLibrariesScope(module.project))
        val facade = JavaPsiFacade.getInstance(module.project)
        facade.findClass(SpringCoreClasses.ACTUATOR_ENDPOINT_AUTO_CONFIGURATION, libraries) ?: return null
        val endpointAnnotation = facade.findClass(SpringCoreClasses.ACTUATOR_ENDPOINT, libraries) ?: return null
        return Discovery(libraries, libraries, annotationWithChildren(endpointAnnotation, libraries))
    }

    private fun annotationWithChildren(annotation: PsiClass, scope: GlobalSearchScope): Collection<PsiClass> {
        val byName = LinkedHashMap<String, PsiClass>()
        fun collect(psiClass: PsiClass) {
            val name = psiClass.qualifiedName ?: return
            if (byName.putIfAbsent(name, psiClass) != null) return
            MetaAnnotationUtil.getChildren(psiClass, scope).forEach(::collect)
        }
        collect(annotation)
        return byName.values
    }

    private fun findEndpoints(project: Project, discovery: Discovery): Map<String, List<ActuatorEndpoint>> {
        val accessAvailable = JavaPsiFacade.getInstance(project)
            .findClass(SpringCoreClasses.ACTUATOR_ENDPOINT_ACCESS, discovery.accessScope) != null

        val result = LinkedHashMap<String, MutableList<ActuatorEndpoint>>()
        for (annotationClass in discovery.annotations) {
            val annotationName = annotationClass.qualifiedName ?: continue
            AnnotatedElementsSearch.searchPsiClasses(annotationClass, discovery.endpointScope).forEach { psiClass ->
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
        val enabledByDefault = AnnotationUtil.getBooleanAttributeValue(annotation, ENABLE_BY_DEFAULT_ATTRIBUTE) != false
        return ActuatorEndpoint(id, psiClass, defaultAccess, enabledByDefault)
    }

    private fun String.isSegmentAt(start: Int, length: Int): Boolean {
        val end = start + length
        return (start == 0 || !this[start - 1].isKeyCharacter()) && (end == this.length || !this[end].isKeyCharacter())
    }

    private fun Char.isKeyCharacter(): Boolean = isLetterOrDigit() || this == '-' || this == '_'

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
