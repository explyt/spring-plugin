/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.beans

import com.explyt.spring.core.externalsystem.model.BeanSearch
import com.explyt.spring.core.externalsystem.model.SpringBeanData
import com.explyt.spring.core.externalsystem.model.SpringBeanType
import com.explyt.spring.core.externalsystem.setting.NativeProjectSettings
import com.explyt.spring.core.externalsystem.utils.Constants
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanNameByAnnotation
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.base.externalSystem.findAll

/**
 * Reads the beans of **one** loaded native root.
 *
 * Choosing the root before reading is what keeps two loaded applications apart; merging their records and
 * filtering afterwards cannot separate a bean both of them declare.
 */
class NativeBeanSnapshotReader(private val project: Project) {

    /**
     * Every loaded root that actually carries bean data, with the identity that ties it to an application.
     *
     * Debug roots are excluded here rather than filtered later: a debug session is not an application a caller
     * can name, and selecting it automatically would answer from a context the caller never asked for.
     */
    fun contexts(): List<NativeBeanContext> {
        val linkedSettings = ExternalSystemApiUtil.getSettings(project, SYSTEM_ID)
        return ProjectDataManager.getInstance().getExternalProjectsData(project, SYSTEM_ID).asSequence()
            .mapNotNull { info -> info.externalProjectStructure?.let { ImportedRoot(it, info.importedAt()) } }
            .filter { it.root.data.externalName != Constants.DEBUG_SESSION_NAME }
            .filter { !it.root.isIgnored }
            .filter { it.root.beanSearchEnabled() }
            .onEach { ProgressManager.checkCanceled() }
            .map { imported ->
                val linkedPath = imported.root.data.linkedExternalProjectPath
                val settings = linkedSettings.getLinkedProjectSettings(linkedPath) as? NativeProjectSettings
                toContext(imported, linkedPath, settings)
            }
            .toList()
    }

    /**
     * The beans of [context], with every class resolved in [applicationModule]'s classpath.
     *
     * The scope is not a detail: two applications can declare the same fully-qualified class against different
     * dependencies, and a project-wide lookup would hand the selected root the other application's declaration -
     * an answer that looks correct and describes the wrong code.
     */
    fun read(context: NativeBeanContext, applicationModule: Module): List<ScopedBeanRecord> {
        val root = rootOf(context) ?: return emptyList()
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(applicationModule, false)

        return root.findAll(SpringBeanData.KEY).asSequence()
            .map { it.data }
            .onEach { ProgressManager.checkCanceled() }
            .map { toRecord(it, context, scope) }
            .toList()
    }

    private fun toRecord(bean: SpringBeanData, context: NativeBeanContext, scope: GlobalSearchScope): ScopedBeanRecord {
        val declaringClass = resolveUnique(bean.className, scope)
        val factory = bean.methodName?.let { name -> declaringClass.psiClass?.findFactory(name) }

        // A factory's declaring class is never the bean's type: reporting `TimeConfig` for `@Bean Clock` would
        // name a class the caller cannot inject. The exported `methodType` wins, and the factory's own return
        // type is the fallback - both resolved inside the application's own classpath.
        val exportedType = bean.methodType?.let { resolveUnique(it, scope) }
        val declaredType = when {
            bean.methodName == null -> declaringClass.psiClass?.let { elementFactory().createType(it) }
            exportedType?.psiClass != null -> elementFactory().createType(exportedType.psiClass)
            else -> factory?.returnType
        }
        val declaredNames = factory?.let(::declaredBeanNames).orEmpty()
        val provenAliases = declaredNames.takeIf { bean.beanName in it }.orEmpty()
        val limitations = declaringClass.limitations +
                (exportedType?.limitations ?: emptySet()) +
                setOfNotNull(NO_DECLARATION_IN_SCOPE.takeIf { declaringClass.psiClass == null }) +
                setOfNotNull(ALIASES_NOT_EXPORTED.takeIf { provenAliases.isEmpty() })

        return ScopedBeanRecord(
            id = BeanSnapshotIdentity.hash(listOf(context.id, bean.className, bean.methodName ?: "", bean.beanName)),
            name = bean.beanName,
            knownNames = linkedSetOf(bean.beanName) + provenAliases,
            typeName = bean.methodType ?: declaredType?.canonicalText ?: bean.className,
            kind = kindOf(bean),
            declaration = factory ?: declaringClass.psiClass,
            declaredType = declaredType,
            declarationModule = null,
            primary = bean.primary,
            priority = null,
            details = BeanDetailsEvidence(primary = bean.primary),
            limitations = limitations,
            runtimeRole = bean.type.name,
            recordedTypeFqn = recordedBeanTypeOf(bean)
        )
    }

    /**
     * The bean's own type as the export named it: the class of a component, the exported return type of a
     * factory. The declaring class of a factory is not the bean's type, so a factory exported without its return
     * type has no recorded type at all.
     */
    private fun recordedBeanTypeOf(bean: SpringBeanData): String? =
        (if (bean.methodName == null) bean.className else bean.methodType)
            ?.substringBefore('<')
            ?.replace('$', '.')
            ?.takeIf { it.isNotBlank() }

    private fun elementFactory() = JavaPsiFacade.getElementFactory(project)

    /**
     * The export carries no aliases, but a `@Bean(name = {"a", "b"})` factory in the selected classpath still says
     * which names it registers. They are taken only when the exported canonical name is one of them, so a factory
     * that no longer matches what the application reported adds nothing.
     */
    private fun declaredBeanNames(factory: PsiMethod): Set<String> =
        factory.resolveBeanNameByAnnotation().orEmpty()

    /**
     * Read from what the export recorded, not from the selected classpath: a class outside the project sources was
     * contributed by a library. `OTHER` means the sync recognised no stereotype on the class - it may have been
     * imported, registered programmatically or carry a custom stereotype - so no declaration kind is claimed.
     */
    private fun kindOf(bean: SpringBeanData): BeanKind = when {
        bean.methodName != null -> BeanKind.BEAN_METHOD
        !bean.projectBean -> BeanKind.LIBRARY
        bean.type == SpringBeanType.OTHER -> BeanKind.UNKNOWN
        else -> BeanKind.COMPONENT
    }

    /**
     * Resolves [fqn] inside the selected application's classpath.
     *
     * Two non-equivalent declarations of one name inside that scope are reported as unknown rather than resolved
     * to the first: picking one would be a guess the caller cannot see.
     */
    private fun resolveUnique(fqn: String, scope: GlobalSearchScope): ResolvedClass {
        val classes = JavaPsiFacade.getInstance(project).findClasses(fqn.replace('$', '.'), scope)
        return when {
            classes.size == 1 -> ResolvedClass(classes.single(), emptySet())
            classes.isEmpty() -> ResolvedClass(null, setOf(TYPE_NOT_ON_APPLICATION_CLASSPATH))
            else -> ResolvedClass(null, setOf(AMBIGUOUS_TYPE_IN_SCOPE))
        }
    }

    private fun PsiClass.findFactory(methodName: String): PsiMethod? =
        findMethodsByName(methodName, true).singleOrNull()

    private fun rootOf(context: NativeBeanContext): DataNode<ProjectData>? =
        ProjectDataManager.getInstance().getExternalProjectsData(project, SYSTEM_ID).asSequence()
            .mapNotNull { it.externalProjectStructure }
            .firstOrNull { it.data.linkedExternalProjectPath == context.linkedPath }

    private fun toContext(
        imported: ImportedRoot,
        linkedPath: String,
        settings: NativeProjectSettings?
    ): NativeBeanContext {
        val applicationClassName = settings?.qualifiedMainClassName
        return NativeBeanContext(
            id = "ctx-" + BeanSnapshotIdentity.hash(listOf(linkedPath)),
            label = imported.root.data.externalName,
            linkedPath = linkedPath,
            applicationClassName = applicationClassName,
            mainSourceKey = linkedPath,
            // A root with neither a linked application class nor a linked main file cannot be tied to an
            // application; an automatic choice would be a guess, so it is never matched.
            identityProven = applicationClassName != null || linkedPath.isNotEmpty(),
            importedAt = imported.importedAt
        )
    }

    /** The platform stores `-1` until an import succeeds; any non-positive value is not a time. */
    private fun ExternalProjectInfo.importedAt(): Long? = lastSuccessfulImportTimestamp.takeIf { it > 0 }

    private data class ImportedRoot(val root: DataNode<ProjectData>, val importedAt: Long?)

    private fun DataNode<ProjectData>.beanSearchEnabled(): Boolean =
        children.any { (it.data as? BeanSearch)?.enabled == true }

    private data class ResolvedClass(val psiClass: PsiClass?, val limitations: Set<String>)

    companion object {
        const val TYPE_NOT_ON_APPLICATION_CLASSPATH = "TYPE_NOT_ON_APPLICATION_CLASSPATH"
        const val AMBIGUOUS_TYPE_IN_SCOPE = "AMBIGUOUS_TYPE_IN_SCOPE"
        const val NO_DECLARATION_IN_SCOPE = "NO_DECLARATION_IN_SCOPE"
        const val ALIASES_NOT_EXPORTED = "ALIASES_NOT_EXPORTED"
    }
}
