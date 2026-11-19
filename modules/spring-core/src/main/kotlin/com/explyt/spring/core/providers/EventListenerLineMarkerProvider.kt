/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses.APPLICATION_LISTENER
import com.explyt.spring.core.SpringCoreClasses.EVENT_LISTENER
import com.explyt.spring.core.SpringCoreClasses.EVENT_PUBLISHER
import com.explyt.spring.core.SpringCoreClasses.GENERIC_APPLICATION_LISTENER
import com.explyt.spring.core.SpringCoreClasses.SMART_APPLICATION_LISTENER
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.SpringProperties.ON_APPLICATION_EVENT
import com.explyt.spring.core.SpringProperties.PUBLISH_EVENT_METHOD
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.SpringSearchUtils
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.core.util.SpringContextScope
import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
import com.explyt.util.ExplytPsiUtil.isEqualOrInheritor
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isPublic
import com.explyt.util.ExplytPsiUtil.isStatic
import com.explyt.util.ExplytPsiUtil.resolvedPsiClass
import com.explyt.util.ModuleUtil

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.codeInsight.navigation.fileStatusAttributes
import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.VfsPresentationUtil
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.idea.base.psi.getLineNumber
import org.jetbrains.uast.*
import java.util.*

class EventListenerLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val uParent = getUParentForIdentifier(element)
        if (uParent is UMethod) {
            val psiMethod = uParent.javaPsi
            if (isMethodModifierForEvent(psiMethod) &&
                (isEventListenerMethod(psiMethod, uParent) || isApplicationEventMethod(psiMethod))
            ) {
                val builder = NavigationGutterIconBuilder.create(
                    SpringIcons.EventPublisher,
                    SpringCoreBundle.message("explyt.spring.gutter.group.event.publisher")
                )
                    .setAlignment(GutterIconRenderer.Alignment.LEFT)
                    .setTargets(NotNullLazyValue.lazy { findPublishEvents(psiMethod) })
                    .setTooltipText(SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.event.publisher"))
                    .setPopupTitle(publisherPopupTitle(psiMethod))
                    .setEmptyPopupText(SpringCoreBundle.message("explyt.spring.gutter.notfound.title.choose.event.publisher"))
                    .setTargetRenderer { publisherTargetRenderer() }

                result += builder.createLineMarkerInfo(element)

            }
        } else {
            val uCallExpression = element.toUElementOfType<UCallExpression>()
            if (uCallExpression != null && uCallExpression.kind == UastCallKind.METHOD_CALL &&
                isPublishEventMethods(uCallExpression)
            ) {
                val sourcePsi = uCallExpression.sourcePsi
                val sourceElement = uCallExpression.methodIdentifier.sourcePsiElement
                if (sourceElement != null && sourcePsi != null
                ) {
                    val builder = NavigationGutterIconBuilder.create(
                        SpringIcons.EventListener,
                        SpringCoreBundle.message("explyt.spring.gutter.group.event.listener")
                    )
                        .setAlignment(GutterIconRenderer.Alignment.LEFT)
                        .setTargets(NotNullLazyValue.createValue { findEventListeners(sourcePsi) })
                        .setTooltipText(SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.event.listener"))
                        .setPopupTitle(SpringCoreBundle.message("explyt.spring.gutter.popup.title.choose.event.listener"))
                        .setEmptyPopupText(SpringCoreBundle.message("explyt.spring.gutter.notfound.title.choose.event.listener"))

                    result.add(builder.createLineMarkerInfo(sourceElement))
                }
            }
        }
    }

    /**
     * Only the declared parameter type is read here: the marker is created on a highlighting hot path, while the
     * `@EventListener(classes = ...)` types need meta-annotation resolution and stay in the lazy target supplier.
     */
    private fun publisherPopupTitle(psiMethod: PsiMethod): String {
        val eventType = psiMethod.parameterList.parameters.singleOrNull()?.type as? PsiClassType
            ?: return SpringCoreBundle.message("explyt.spring.gutter.popup.title.choose.event.publisher")
        return SpringCoreBundle.message(
            "explyt.spring.gutter.popup.title.choose.event.publisher.typed", eventType.presentableText
        )
    }

    private fun publisherTargetRenderer(): PsiTargetPresentationRenderer<PsiElement> {
        return object : PsiTargetPresentationRenderer<PsiElement>() {

            override fun getPresentation(element: PsiElement): TargetPresentation {
                val project = element.project
                val file = element.containingFile?.virtualFile
                val moduleTextWithIcon = SpringBeanLineMarkerProvider.getModuleTextWithIcon(element)
                return TargetPresentation
                    .builder(getElementText(element))
                    .backgroundColor(file?.let { VfsPresentationUtil.getFileBackgroundColor(project, it) })
                    .icon(element.getIcon(Iconable.ICON_FLAG_VISIBILITY or Iconable.ICON_FLAG_READ_STATUS))
                    .containerText(getContainerText(element), file?.let { fileStatusAttributes(project, it) })
                    .locationText(moduleTextWithIcon?.text, moduleTextWithIcon?.icon)
                    .presentation()
            }

            override fun getElementText(element: PsiElement): String {
                val uMethod = element.toUElement()?.getParentOfType<UMethod>() ?: return super.getElementText(element)
                val className = uMethod.javaPsi.containingClass?.name ?: return uMethod.name
                return "$className#${uMethod.name}"
            }

            override fun getContainerText(element: PsiElement): String? {
                val fileName = element.containingFile?.virtualFile?.name ?: return null
                return "$fileName:${element.getLineNumber() + 1}"
            }
        }
    }

    private fun isMethodModifierForEvent(psiMethod: PsiMethod): Boolean {
        return psiMethod.isPublic && !psiMethod.isStatic && !psiMethod.isConstructor
    }

    private fun isEventListenerMethod(psiMethod: PsiMethod, uMethod: UMethod): Boolean {
        if (!psiMethod.isMetaAnnotatedBy(EVENT_LISTENER)) return false
        val eventListenerWithParameter = psiMethod.parameterList.parameters.singleOrNull()?.type is PsiClassType
        return eventListenerWithParameter || eventClassLiterals(psiMethod, uMethod).isNotEmpty()
    }

    private fun eventClassLiterals(psiMethod: PsiMethod, uMethod: UMethod): Set<UExpression> {
        val module = ModuleUtil.findModuleOrLibraryOwner(psiMethod) ?: return emptySet()
        val metaHolder = SpringSearchService.getInstance(module.project).getMetaAnnotations(module, EVENT_LISTENER)
        return metaHolder.getAnnotationValues(uMethod, setOf("value", "classes"))
    }

    private fun isApplicationEventMethod(psiMethod: PsiMethod): Boolean {
        val module = ModuleUtil.findModuleOrLibraryOwner(psiMethod) ?: return false
        val applicationListenerClass =
            LibraryClassCache.searchForLibraryClass(module, APPLICATION_LISTENER) ?: return false
        val supers = psiMethod.parentOfType<PsiClass>()?.supers ?: return false
        val byApplicationEvent = supers.asSequence()
            .map { psiClass -> psiClass.isEqualOrInheritor(applicationListenerClass) }
            .any { it }

        return (psiMethod.name == ON_APPLICATION_EVENT) && byApplicationEvent
    }

    private fun findPublishEvents(psiMethod: PsiMethod): List<PsiElement> {
        StatisticService.getInstance().addActionUsage(StatisticActionId.GUTTER_TARGET_PUBLISH_EVENT)
        val module = ModuleUtil.findModuleOrLibraryOwner(psiMethod) ?: return emptyList()
        val eventPsiType = psiMethod.parameterList.parameters.singleOrNull()?.type
        val eventClasses = getPsiClassesByAnnotationCached(psiMethod)
        val libraries = classpathLibraries(module)
        val fileIndex = ProjectFileIndex.getInstance(module.project)
        val libraryCalls = libraryPublishCalls(module.project).filter { call ->
            fileIndex.getOrderEntriesForFile(call.file).any { (it as? LibraryOrderEntry)?.library in libraries }
        }
        return (contextPublishCalls(module) + libraryCalls).asSequence()
            .filter { isAcceptableType(eventPsiType, it.argumentType, eventClasses) }
            .map { it.call }
            .toList()
    }

    private fun getPsiClassesByAnnotationCached(psiMethod: PsiMethod): Set<PsiClass> {
        return CachedValuesManager.getCachedValue(psiMethod) {
            val project = psiMethod.project
            val classes = getPsiClassesByAnnotation(psiMethod)
            CachedValueProvider.Result.create(
                classes,
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    private fun getPsiClassesByAnnotation(psiMethod: PsiMethod): Set<PsiClass> {
        val uMethod = psiMethod.toUElementOfType<UMethod>() ?: return emptySet()
        return eventClassLiterals(psiMethod, uMethod).asSequence()
            .filterIsInstance<UClassLiteralExpression>()
            .mapNotNull { it.type?.resolvedPsiClass }
            .toSet()
    }

    private fun publishEventMethods(eventPublisherClasses: Collection<PsiClass>): List<PsiMethod> =
        eventPublisherClasses.flatMap { it.findMethodsByName(PUBLISH_EVENT_METHOD, false).asList() }

    private fun isAcceptableType(
        eventPsiType: PsiType?, argumentType: PsiType, eventPsiClass: Set<PsiClass>
    ): Boolean {
        return (eventPsiType != null && eventPsiType.isAssignableFrom(argumentType))
                || (eventPsiClass.isNotEmpty()
                && eventPsiClass.any { argumentType.resolveBeanPsiClass?.isEqualOrInheritor(it) == true })
    }

    private fun contextPublishCalls(module: Module): List<PublishCall> {
        return CachedValuesManager.getManager(module.project).getCachedValue(module) {
            val publisherClass = LibraryClassCache.searchForLibraryClass(module, EVENT_PUBLISHER)
            CachedValueProvider.Result(
                findPublishCalls(listOfNotNull(publisherClass), SpringContextScope.of(module)),
                ModificationTrackerManager.getInstance(module.project).getUastModelAndLibraryTracker(),
                ProjectRootModificationTracker.getInstance(module.project)
            )
        }
    }

    private fun classpathLibraries(module: Module): Set<Library> {
        val libraries = mutableSetOf<Library>()
        OrderEnumerator.orderEntries(module).recursively().forEachLibrary { libraries += it; true }
        return libraries
    }

    private fun libraryPublishCalls(project: Project): List<PublishCall> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            val publisherClasses = JavaPsiFacade.getInstance(project)
                .findClasses(EVENT_PUBLISHER, GlobalSearchScope.allScope(project)).asList()
            CachedValueProvider.Result(
                findPublishCalls(publisherClasses, ModuleUtil.getOnlyLibrarySearchScope(project)),
                ModificationTrackerManager.getInstance(project).getLibraryTracker()
            )
        }
    }

    private fun findPublishCalls(publisherClasses: Collection<PsiClass>, scope: GlobalSearchScope): List<PublishCall> {
        return publishEventMethods(publisherClasses).asSequence()
            .flatMap { MethodReferencesSearch.search(it, scope, true) }
            .mapNotNull { toPublishCall(it) }
            .distinctBy { it.call }
            .toList()
    }

    private fun toPublishCall(reference: PsiReference): PublishCall? {
        val uCallExpression = reference.element.toUElement()?.getParentOfType<UCallExpression>() ?: return null
        if (uCallExpression.getArgumentForParameter(1) != null) return null
        val argumentType = uCallExpression.getArgumentForParameter(0)?.getExpressionType() ?: return null
        val call = uCallExpression.sourcePsi ?: return null
        val file = call.containingFile?.viewProvider?.virtualFile ?: return null
        return PublishCall(call, file, argumentType)
    }

    private fun isPublishEventMethods(uMethodCall: UCallExpression): Boolean {
        if (uMethodCall.methodName != PUBLISH_EVENT_METHOD) return false
        val psiMethod = uMethodCall.resolve() ?: return false
        val containClass = psiMethod.containingClass
        return containClass != null && InheritanceUtil.isInheritor(containClass, EVENT_PUBLISHER)
    }

    private fun findEventListeners(psiElement: PsiElement): Collection<PsiElement> {
        StatisticService.getInstance().addActionUsage(StatisticActionId.GUTTER_TARGET_EVENT_LISTENER)
        val uCallExpression = psiElement.toUElementOfType<UCallExpression>() ?: return Collections.emptyList()
        if (uCallExpression.valueArgumentCount != 1) return Collections.emptyList()
        val eventPsiType = uCallExpression.valueArguments[0].getExpressionType() ?: return Collections.emptyList()

        val ownModule = ModuleUtilCore.findModuleForPsiElement(psiElement)
        val module = ownModule ?: ModuleUtil.findModuleOrLibraryOwner(psiElement) ?: return Collections.emptyList()
        val projectListeners = if (ownModule == null) projectListenerMethods(module) else contextListenerMethods(module)
        return (projectListeners + getLibraryListenerMethodsCached(module)).asSequence()
            .filter { isEqualsTypeOrClass(it, eventPsiType) }
            .map { it.element.navigationElement }
            .toList()
    }

    private fun contextListenerMethods(module: Module): Collection<MethodArgumentClasses> {
        return CachedValuesManager.getManager(module.project).getCachedValue(module) {
            CachedValueProvider.Result.create(
                getListenerMethods(module, SpringContextScope.of(module)),
                ModificationTrackerManager.getInstance(module.project).getUastModelAndLibraryTracker(),
                ProjectRootModificationTracker.getInstance(module.project)
            )
        }
    }

    private fun projectListenerMethods(module: Module): Collection<MethodArgumentClasses> {
        return CachedValuesManager.getManager(module.project).getCachedValue(module) {
            CachedValueProvider.Result.create(
                getListenerMethods(module, GlobalSearchScope.projectScope(module.project)),
                ModificationTrackerManager.getInstance(module.project).getUastModelAndLibraryTracker()
            )
        }
    }

    private fun getLibraryListenerMethodsCached(module: Module): Collection<MethodArgumentClasses> {
        return CachedValuesManager.getManager(module.project).getCachedValue(module) {
            CachedValueProvider.Result.create(
                getListenerMethods(module, ModuleUtil.getOnlyLibrarySearchScope(module))
                    .filterNot { it.element.isEventTypeDecidedAtRuntime() },
                ModificationTrackerManager.getInstance(module.project).getLibraryTracker()
            )
        }
    }

    private fun PsiMethod.isEventTypeDecidedAtRuntime(): Boolean {
        val listenerClass = containingClass ?: return false
        return InheritanceUtil.isInheritor(listenerClass, SMART_APPLICATION_LISTENER)
                || InheritanceUtil.isInheritor(listenerClass, GENERIC_APPLICATION_LISTENER)
    }

    private fun getListenerMethods(module: Module, scope: GlobalSearchScope): List<MethodArgumentClasses> {
        return getMethodsByApplicationEvent(module, scope) + getMethodsByEventListener(module, scope)
    }

    private fun getMethodsByApplicationEvent(module: Module, scope: GlobalSearchScope): List<MethodArgumentClasses> {
        val listenerClass = SpringSearchUtils.findAnnotationClassesByQualifiedName(module, APPLICATION_LISTENER)
        return listenerClass.asSequence()
            .flatMap { ClassInheritorsSearch.search(it, scope, true).findAll() }
            .filterNotNull()
            .flatMap { it.findMethodsByName(ON_APPLICATION_EVENT, false).asSequence() }
            .filterNotNull()
            .map { MethodArgumentClasses(it) }
            .toList()
    }

    private fun getMethodsByEventListener(module: Module, scope: GlobalSearchScope): List<MethodArgumentClasses> {
        val listenerClass = SpringSearchUtils.findAnnotationClassesByQualifiedName(module, EVENT_LISTENER)
        val listenerMethods = listenerClass.asSequence()
            .flatMap { AnnotatedElementsSearch.searchPsiMethods(it, scope) }

        val methodArguments = mutableListOf<MethodArgumentClasses>()
        for (element in listenerMethods) {
            val byAnnotation = getPsiClassesByAnnotationCached(element)
            methodArguments.add(MethodArgumentClasses(element, byAnnotation))
        }
        return methodArguments
    }

    private fun isEqualsTypeOrClass(methodArgumentClasses: MethodArgumentClasses, eventPsiType: PsiType): Boolean {
        val argumentType = methodArgumentClasses.argumentType
            ?: return methodArgumentClasses.argumentClasses.any {
                eventPsiType.resolveBeanPsiClass?.isEqualOrInheritor(it) == true
            }
        return argumentType.isAssignableFrom(eventPsiType)
    }

    private class PublishCall(val call: PsiElement, val file: VirtualFile, val argumentType: PsiType)

    private class MethodArgumentClasses(val element: PsiMethod, psiClassesFromAnnotation: Set<PsiClass>? = null) {
        val argumentType: PsiType? = element.parameterList.parameters.singleOrNull()?.type
        val argumentClasses: Set<PsiClass> =
            if (argumentType == null) psiClassesFromAnnotation.orEmpty() else emptySet()
    }
}
