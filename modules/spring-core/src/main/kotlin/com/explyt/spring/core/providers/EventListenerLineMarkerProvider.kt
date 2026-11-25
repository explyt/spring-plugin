/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers

import com.explyt.base.LibraryClassCache
import com.explyt.plugin.PluginIds
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
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.codeInsight.navigation.fileStatusAttributes
import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.openapi.util.registry.Registry
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

    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        if (isLeftToJetBrainsSpring()) return
        super.collectSlowLineMarkers(elements, result)
    }

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
                if (isLeftToJetBrainsSpring()) return
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
                if (sourceElement != null && sourcePsi != null) {
                    if (isLeftToJetBrainsSpring()) return
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

    private fun isLeftToJetBrainsSpring(): Boolean =
        PluginIds.SPRING_JB.isEnabledWithUltimate() && !Registry.`is`(SHOW_WITH_JETBRAINS_SPRING_KEY, false)

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
        return eventListenerWithParameter || eventTypeValues(psiMethod, uMethod).isNotEmpty()
    }

    private fun eventTypeValues(psiMethod: PsiMethod, uMethod: UMethod): Set<UExpression> {
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
        val project = psiMethod.project
        val eventType = EventType.of(psiMethod, getPsiClassesByAnnotationCached(psiMethod))
        val ownModule = ModuleUtilCore.findModuleForPsiElement(psiMethod)
        val calls = if (ownModule == null) {
            projectPublishCalls(project) + libraryPublishCalls(project)
        } else {
            val context = SpringContextScope.of(ownModule)
            projectPublishCalls(project).filter { context.containsSource(it.file) } +
                    libraryPublishCalls(project).filter { context.containsLibraryFile(it.file) }
        }
        return calls.asSequence()
            .filter { eventType.accepts(it.argumentType) }
            .map { it.call }
            .toList()
    }

    private fun getPsiClassesByAnnotationCached(psiMethod: PsiMethod): Set<PsiClass> {
        return CachedValuesManager.getCachedValue(psiMethod) {
            CachedValueProvider.Result.create(
                getPsiClassesByAnnotation(psiMethod),
                ModificationTrackerManager.getInstance(psiMethod.project).getUastModelAndLibraryTracker()
            )
        }
    }

    private fun getPsiClassesByAnnotation(psiMethod: PsiMethod): Set<PsiClass> {
        val uMethod = psiMethod.toUElementOfType<UMethod>() ?: return emptySet()
        return eventTypeValues(psiMethod, uMethod).asSequence()
            .filterIsInstance<UClassLiteralExpression>()
            .mapNotNull { it.type?.resolvedPsiClass }
            .toSet()
    }

    private fun projectPublishCalls(project: Project): List<PublishCall> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                findPublishCalls(project, GlobalSearchScope.projectScope(project)),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker(),
                ProjectRootModificationTracker.getInstance(project)
            )
        }
    }

    private fun libraryPublishCalls(project: Project): List<PublishCall> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                findPublishCalls(project, ModuleUtil.getOnlyLibrarySearchScope(project)),
                ModificationTrackerManager.getInstance(project).getLibraryTracker(),
                ProjectRootModificationTracker.getInstance(project)
            )
        }
    }

    private fun findPublishCalls(project: Project, scope: GlobalSearchScope): List<PublishCall> {
        return JavaPsiFacade.getInstance(project)
            .findClasses(EVENT_PUBLISHER, GlobalSearchScope.allScope(project)).asSequence()
            .flatMap { it.findMethodsByName(PUBLISH_EVENT_METHOD, false).asSequence() }
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

        val project = psiElement.project
        val ownModule = ModuleUtilCore.findModuleForPsiElement(psiElement)
        val listeners = if (ownModule == null) {
            projectListeners(project) + libraryListeners(project)
        } else {
            val context = SpringContextScope.of(ownModule)
            projectListeners(project).filter { context.containsSource(it.file) } +
                    libraryListeners(project).filter { context.containsLibraryFile(it.file) }
        }
        return listeners.asSequence()
            .filter { it.eventType.accepts(eventPsiType) }
            .map { it.method.navigationElement }
            .toList()
    }

    private fun projectListeners(project: Project): List<Listener> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                findListeners(project, GlobalSearchScope.projectScope(project)),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker(),
                ProjectRootModificationTracker.getInstance(project)
            )
        }
    }

    private fun libraryListeners(project: Project): List<Listener> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                findListeners(project, ModuleUtil.getOnlyLibrarySearchScope(project))
                    .filterNot { it.method.isEventTypeDecidedAtRuntime() },
                ModificationTrackerManager.getInstance(project).getLibraryTracker(),
                ProjectRootModificationTracker.getInstance(project)
            )
        }
    }

    private fun PsiMethod.isEventTypeDecidedAtRuntime(): Boolean {
        val listenerClass = containingClass ?: return false
        return InheritanceUtil.isInheritor(listenerClass, SMART_APPLICATION_LISTENER)
                || InheritanceUtil.isInheritor(listenerClass, GENERIC_APPLICATION_LISTENER)
    }

    private fun findListeners(project: Project, scope: GlobalSearchScope): List<Listener> {
        val allScope = GlobalSearchScope.allScope(project)
        val facade = JavaPsiFacade.getInstance(project)
        val applicationListeners = facade.findClasses(APPLICATION_LISTENER, allScope).asSequence()
            .flatMap { ClassInheritorsSearch.search(it, scope, true).findAll() }
            .flatMap { it.findMethodsByName(ON_APPLICATION_EVENT, false).asSequence() }
            .map { EventType.of(it, emptySet()) to it }
        val annotatedListeners = eventListenerAnnotations(project).asSequence()
            .flatMap { AnnotatedElementsSearch.searchPsiMethods(it, scope) }
            .map { EventType.of(it, getPsiClassesByAnnotationCached(it)) to it }
        return (applicationListeners + annotatedListeners)
            .mapNotNull { (eventType, method) ->
                val file = method.containingFile?.viewProvider?.virtualFile ?: return@mapNotNull null
                Listener(method, file, eventType)
            }
            .distinctBy { it.method }
            .toList()
    }

    private fun eventListenerAnnotations(project: Project): Set<PsiClass> =
        ModuleManager.getInstance(project).modules
            .flatMapTo(mutableSetOf()) { SpringSearchUtils.findAnnotationClassesByQualifiedName(it, EVENT_LISTENER) }

    private class PublishCall(val call: PsiElement, val file: VirtualFile, val argumentType: PsiType)

    private class Listener(val method: PsiMethod, val file: VirtualFile, val eventType: EventType)

    private class EventType(private val parameterType: PsiType?, private val classes: Set<PsiClass>) {

        fun accepts(publishedType: PsiType): Boolean =
            if (classes.isNotEmpty()) {
                classes.any { publishedType.resolveBeanPsiClass?.isEqualOrInheritor(it) == true }
            } else {
                parameterType?.isAssignableFrom(publishedType) == true
            }

        companion object {
            fun of(method: PsiMethod, annotationClasses: Set<PsiClass>) =
                EventType(method.parameterList.parameters.singleOrNull()?.type, annotationClasses)
        }
    }

    private companion object {
        const val SHOW_WITH_JETBRAINS_SPRING_KEY = "explyt.spring.gutter.events.withJetBrainsSpring"
    }
}
