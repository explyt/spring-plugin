/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.properties.references.ActuatorEndpointKeys
import com.explyt.spring.core.service.AliasUtils
import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.conditional.ConditionVerdict
import com.explyt.spring.core.service.conditional.combined
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElementOfType

/**
 * A project `@EndpointWebExtension` bean as Boot's web endpoint discoverer collects it: the extension annotation is read
 * merged over the type hierarchy, so [carrier] is the type declaring the [annotationName] annotation — the extension
 * itself or one of its supertypes. [isBean] tells whether the bean model registers the extension; [verdict] is the most
 * active condition verdict among its beans, `null` when it is not a bean or conditions are not evaluated.
 */
class ActuatorWebExtension(
    val psiClass: PsiClass,
    val carrier: PsiClass,
    val annotationName: String,
    val isBean: Boolean,
    val verdict: ConditionVerdict?,
)

/**
 * The project web extensions of a module's Actuator endpoints. Only beans are applied, as Boot collects extensions
 * from `beanNamesForAnnotationIncludingAncestors(EndpointExtension)`.
 */
object ActuatorWebExtensions {

    /**
     * The extensions Boot applies to [endpointClass]: a test-source extension only to a test-source endpoint, and none
     * whose bean condition does not match.
     */
    fun appliedTo(endpointClass: PsiClass, module: Module): List<ActuatorWebExtension> {
        val endpointInTests = isInTestSources(endpointClass)
        return byEndpoint(module)[endpointClass.qualifiedName].orEmpty()
            .filter { it.isBean }
            .filter { endpointInTests || !isInTestSources(it.psiClass) }
            .filterNot { it.verdict is ConditionVerdict.Inactive }
    }

    fun isDeclaredOn(extension: UClass, module: Module): Boolean {
        val annotationNames = annotationNames(module)
        return annotationNames.isNotEmpty() && extension.uAnnotations.any { it.qualifiedName in annotationNames }
    }

    private fun annotationNames(module: Module): Set<String> =
        CachedValuesManager.getManager(module.project).getCachedValue(module) {
            CachedValueProvider.Result(
                webExtensionAnnotations(module).orEmpty().mapNotNullTo(mutableSetOf()) { it.qualifiedName },
                ModificationTrackerManager.getInstance(module.project).getUastModelAndLibraryTracker()
            )
        }

    private fun webExtensionAnnotations(module: Module): Collection<PsiClass>? {
        val scopes = listOf(module.getModuleWithDependenciesAndLibrariesScope(false), module.getModuleRuntimeScope(false))
        val facade = JavaPsiFacade.getInstance(module.project)
        val annotation = scopes.firstNotNullOfOrNull {
            facade.findClass(SpringCoreClasses.ACTUATOR_ENDPOINT_WEB_EXTENSION, it)
        } ?: return null
        return scopes.flatMap { ActuatorEndpointKeys.annotationWithChildren(annotation, it) }
            .distinctBy { it.qualifiedName }
    }

    private fun byEndpoint(module: Module): Map<String, List<ActuatorWebExtension>> =
        CachedValuesManager.getManager(module.project).getCachedValue(module) {
            val trackers = ModificationTrackerManager.getInstance(module.project)
            CachedValueProvider.Result(
                discover(module),
                trackers.getUastModelAndLibraryTracker(),
                trackers.getPropertyTracker()
            )
        }

    private fun discover(module: Module): Map<String, List<ActuatorWebExtension>> {
        val project = module.project
        val webExtensionAnnotations = webExtensionAnnotations(module) ?: return emptyMap()
        val projectScope = GlobalSearchScope.moduleWithDependenciesScope(module)
        val candidates = webExtensionAnnotations
            .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, projectScope).findAll() }
            .filterNot { it.isAnnotationType }
            .flatMap { listOf(it) + ClassInheritorsSearch.search(it, projectScope, true, true, false).findAll() }
            .distinct()
            .filter { it.isConcrete }
        if (candidates.isEmpty()) return emptyMap()

        val declarations = WebExtensionDeclarations(
            webExtensionAnnotations.mapNotNullTo(mutableSetOf()) { it.qualifiedName },
            lazy { MetaAnnotationsHolder.of(module, SpringCoreClasses.ACTUATOR_ENDPOINT_EXTENSION) },
        )
        val beans = SpringSearchService.getInstance(project).foundBeans(module)
        val verdictsByClass = (beans.active + beans.excluded)
            .groupBy({ it.psiClass }, { beans.verdicts[it] })
        return candidates
            .mapNotNull { extension ->
                ProgressManager.checkCanceled()
                val verdicts = verdictsByClass[extension]
                val declaration = declarations.of(extension) ?: return@mapNotNull null
                declaration.endpointName to ActuatorWebExtension(
                    extension, declaration.carrier, declaration.annotationName,
                    isBean = verdicts != null,
                    verdict = verdicts?.filterNotNull()?.mostActive()
                )
            }
            .groupBy({ it.first }, { it.second })
    }

    private val PsiClass.isConcrete: Boolean
        get() = !isInterface && !hasModifierProperty(PsiModifier.ABSTRACT)

    private fun isInTestSources(psiClass: PsiClass): Boolean {
        val file = psiClass.containingFile?.virtualFile ?: return false
        return ProjectFileIndex.getInstance(psiClass.project).isInTestSourceContent(file)
    }

    private class Declaration(val endpointName: String, val carrier: PsiClass, val annotationName: String)

    private class WebExtensionDeclarations(
        private val qualifiedNames: Set<String>,
        endpointExtensionAttributes: Lazy<MetaAnnotationsHolder>,
    ) {
        private val endpointExtensionAttributes by endpointExtensionAttributes

        fun of(extension: PsiClass): Declaration? {
            val (carrier, annotation) = typeHierarchyOf(extension).firstNotNullOfOrNull { type ->
                type.toUElementOfType<UClass>()?.uAnnotations
                    ?.firstOrNull { it.qualifiedName in qualifiedNames }
                    ?.let { type to it }
            } ?: return null
            val endpointName = endpointExtensionAttributes.getAnnotationMemberValues(annotation, setOf(ENDPOINT_ATTRIBUTE))
                .firstNotNullOfOrNull { AliasUtils.getPsiClasses(it).firstOrNull() }
                ?.qualifiedName ?: return null
            return Declaration(endpointName, carrier, annotation.qualifiedName ?: return null)
        }

        private fun typeHierarchyOf(psiClass: PsiClass): Set<PsiClass> {
            val hierarchy = LinkedHashSet<PsiClass>()
            fun visit(type: PsiClass) {
                if (type.qualifiedName == CommonClassNames.JAVA_LANG_OBJECT || !hierarchy.add(type)) return
                type.interfaces.forEach(::visit)
                type.superClass?.let(::visit)
            }
            visit(psiClass)
            return hierarchy
        }
    }

    private const val ENDPOINT_ATTRIBUTE = "endpoint"
}

internal fun List<ConditionVerdict>.mostActive(): ConditionVerdict? = when {
    isEmpty() -> null
    ConditionVerdict.Active in this -> ConditionVerdict.Active
    else -> filterIsInstance<ConditionVerdict.Undecided>().ifEmpty { this }.asSequence().combined()
}
