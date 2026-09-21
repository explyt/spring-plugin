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
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.SpringProperties.ON_APPLICATION_EVENT
import com.explyt.spring.core.SpringProperties.PUBLISH_EVENT_METHOD
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.SpringSearchUtils
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
import com.explyt.util.ExplytPsiUtil.isEqualOrInheritor
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isPublic
import com.explyt.util.ExplytPsiUtil.isStatic
import com.explyt.util.ExplytPsiUtil.resolvedPsiClass
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.codeInsight.navigation.fileStatusAttributes
import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.openapi.vfs.newvfs.VfsPresentationUtil
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.idea.base.psi.getLineNumber
import org.jetbrains.uast.*
import java.util.*

private class MethodArgumentClasses(val element: PsiMethod, psiClassesFromAnnotation: Set<PsiClass>? = null) {
    var argumentClasses: Set<PsiClass> = emptySet()
    var argumentType: PsiType? = null

    init {
        if (element.parameterList.parametersCount == 1) {
            argumentType = element.parameterList.parameters[0].type
        } else if (psiClassesFromAnnotation != null) {
            argumentClasses += psiClassesFromAnnotation
        }
    }
}

private val PROJECT_WIDE_LISTENER_METHODS =
    Key.create<CachedValue<Collection<MethodArgumentClasses>>>("explyt.spring.event.listeners.projectWide")

class EventListenerLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        if (PluginIds.SPRING_JB.isEnabledWithUltimate()) return
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
                (isEventListenerMethod(psiMethod) || isApplicationEventMethod(psiMethod))
            ) {
                if (isSuppressedByJetBrainsSpring()) return
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
                    if (isSuppressedByJetBrainsSpring()) return
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
     * [collectNavigationMarkers] is also reached directly by `RelatedItemLineMarkerGotoAdapter`
     * (Navigate | Related Symbol), bypassing [collectSlowLineMarkers], so the JetBrains Spring
     * suppression has to be repeated per element and cannot be moved to the batch entry point only.
     *
     * Consulted only where a marker would actually be produced: an element rejected by the cheap
     * UAST checks above yields nothing either way.
     */
    private fun isSuppressedByJetBrainsSpring() = PluginIds.SPRING_JB.isEnabledWithUltimate()

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

    private fun isEventListenerMethod(psiMethod: PsiMethod): Boolean {
        if (!psiMethod.isMetaAnnotatedBy(EVENT_LISTENER)) return false
        val eventListenerWithParameter = psiMethod.parameterList.parametersCount == 1
                && psiMethod.parameterList.parameters[0].type is PsiClassType
        if (eventListenerWithParameter) {
            return true
        }

        val module = ModuleUtilCore.findModuleForPsiElement(psiMethod) ?: return false
        val metaHolder = SpringSearchService.getInstance(module.project).getMetaAnnotations(module, EVENT_LISTENER)
        val annotationValues = metaHolder.getAnnotationMemberValues(psiMethod, setOf("value", "classes"))
        return annotationValues.isNotEmpty()
    }

    private fun isApplicationEventMethod(psiMethod: PsiMethod): Boolean {
        val module = ModuleUtilCore.findModuleForPsiElement(psiMethod) ?: return false
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
        val module = ModuleUtilCore.findModuleForPsiElement(psiMethod) ?: return emptyList()

        var eventPsiType: PsiType? = null
        if (psiMethod.parameterList.parametersCount == 1) {
            eventPsiType = psiMethod.parameterList.parameters[0].type
        }
        val eventPsiClassByAnnotation = getPsiClassesByAnnotationCached(module, psiMethod)

        val methodArgumentTypes = getMethodArgumentTypes(module)
        return getPublishMethodCalls(methodArgumentTypes, eventPsiType, eventPsiClassByAnnotation)
    }

    private fun getPsiClassesByAnnotationCached(module: Module, psiMethod: PsiMethod): Set<PsiClass> {
        val cacheManager = CachedValuesManager.getManager(module.project)

        return cacheManager.getCachedValue(psiMethod) {
            CachedValueProvider.Result.create(
                getPsiClassesByAnnotation(module, psiMethod),
                ModificationTrackerManager.getInstance(module.project).getUastModelAndLibraryTracker()
            )
        }
    }

    private fun getPsiClassesByAnnotation(module: Module, psiMethod: PsiMethod): Set<PsiClass> {
        val metaHolder = SpringSearchService.getInstance(module.project).getMetaAnnotations(module, EVENT_LISTENER)
        val annotationValues = metaHolder.getAnnotationMemberValues(psiMethod, setOf("value", "classes"))
        return annotationValues
            .mapNotNullTo(mutableSetOf()) { (it.toUElement() as? UClassLiteralExpression)?.type?.resolvedPsiClass }
    }

    private fun getPublishMethods(module: Module): List<PsiMethod> {
        val eventPublisherClass = LibraryClassCache.searchForLibraryClass(module, EVENT_PUBLISHER) ?: return emptyList()
        return eventPublisherClass
            .findMethodsByName(PUBLISH_EVENT_METHOD, false)
            .filterNotNull()
    }

    private fun getPublishMethodCalls(
        methodArgumentTypes: List<MethodCallArgumentTypes>, eventPsiType: PsiType?, eventPsiClass: Set<PsiClass>
    ): List<PsiElement> {
        return methodArgumentTypes.asSequence()
            .filter { isAcceptableType(eventPsiType, it.argumentType, eventPsiClass) }
            .mapNotNull { it.element.sourcePsi }
            .toList()
    }

    private fun isAcceptableType(
        eventPsiType: PsiType?, argumentType: PsiType, eventPsiClass: Set<PsiClass>
    ): Boolean {
        return (eventPsiType != null && eventPsiType.isAssignableFrom(argumentType))
                || (eventPsiClass.isNotEmpty()
                && eventPsiClass.any { argumentType.resolveBeanPsiClass?.isEqualOrInheritor(it) == true })
    }

    private fun getMethodArgumentTypes(module: Module): List<MethodCallArgumentTypes> {
        val publishMethods = getPublishMethods(module)
        return publishMethods
            .asSequence()
            .map { SpringSearchUtils.searchReferenceByMethod(module, it) }
            .flatMap { it.asSequence() }
            .mapNotNull { toMethodCallArgumentTypes(it) }
            .toList()
    }

    private fun toMethodCallArgumentTypes(it: PsiReference): MethodCallArgumentTypes? {
        val uCallExpression = it.element.toUElement()?.getParentOfType<UCallExpression>() ?: return null
        val argumentType = uCallExpression.getArgumentForParameter(0)?.getExpressionType() ?: return null
        if (uCallExpression.getArgumentForParameter(1) != null) return null
        return MethodCallArgumentTypes(uCallExpression, argumentType)
    }

    private fun isPublishEventMethods(uMethodCall: UCallExpression): Boolean {
        if (uMethodCall.methodName != PUBLISH_EVENT_METHOD) return false
        val psiMethod = uMethodCall.resolve() ?: return false
        val containClass = psiMethod.containingClass
        return containClass != null && InheritanceUtil.isInheritor(containClass, EVENT_PUBLISHER)
    }

    /**
     * [ModuleUtilCore.findModuleForPsiElement] reaches its library branch only for a [PsiFileSystemItem], so an
     * expression inside a dependency — where Spring itself publishes most lifecycle events — resolves to no
     * module. Asking about the containing file instead takes that branch and answers with a module the library
     * is attached to.
     */
    private fun findLibraryOwnerModule(psiElement: PsiElement): Module? =
        psiElement.containingFile?.originalFile?.let { ModuleUtilCore.findModuleForPsiElement(it) }

    private fun findEventListeners(psiElement: PsiElement): Collection<PsiElement> {
        StatisticService.getInstance().addActionUsage(StatisticActionId.GUTTER_TARGET_EVENT_LISTENER)
        val elementModule = ModuleUtilCore.findModuleForPsiElement(psiElement)
        val module = elementModule
            ?: findLibraryOwnerModule(psiElement)
            ?: return Collections.emptyList()
        val uCallExpression = psiElement.toUElementOfType<UCallExpression>() ?: return Collections.emptyList()
        if (uCallExpression.valueArgumentCount != 1) return Collections.emptyList()
        val eventPsiType = uCallExpression.valueArguments[0].getExpressionType() ?: return Collections.emptyList()

        return listenerMethods(module, anchoredInLibrary = elementModule == null).asSequence()
            .filter { isEqualsTypeOrClass(it, eventPsiType) }
            .map { it.element.navigationElement }
            .toList()
    }

    /**
     * A call inside a library belongs to no module: the platform answers with whichever attached module sorts
     * first by dependency order, so that module's dependency closure is an arbitrary slice of the project and
     * would hide listeners declared in sibling modules. Only an element that really belongs to a module can be
     * searched through that module.
     */
    private fun listenerMethods(module: Module, anchoredInLibrary: Boolean): Collection<MethodArgumentClasses> {
        if (!anchoredInLibrary) {
            return getMethodsByApplicationEventCached(module) + getMethodsByEventListenerCached(module)
        }
        return CachedValuesManager.getManager(module.project).getCachedValue(
            module, PROJECT_WIDE_LISTENER_METHODS, {
                val scope = GlobalSearchScope.projectScope(module.project)
                CachedValueProvider.Result.create(
                    getMethodsByApplicationEvent(module, scope) + getMethodsByEventListener(module, scope),
                    ModificationTrackerManager.getInstance(module.project).getUastModelAndLibraryTracker()
                )
            }, false
        )
    }

    private fun getMethodsByApplicationEventCached(module: Module): Collection<MethodArgumentClasses> {
        val cacheManager = CachedValuesManager.getManager(module.project)

        return cacheManager.getCachedValue(module) {
            CachedValueProvider.Result.create(
                getMethodsByApplicationEvent(module, GlobalSearchScope.moduleWithDependenciesScope(module)),
                ModificationTrackerManager.getInstance(module.project).getUastModelAndLibraryTracker()
            )
        }
    }

    private fun getMethodsByEventListenerCached(module: Module): Collection<MethodArgumentClasses> {
        val cacheManager = CachedValuesManager.getManager(module.project)

        return cacheManager.getCachedValue(module) {
            CachedValueProvider.Result.create(
                getMethodsByEventListener(module, GlobalSearchScope.moduleWithDependenciesScope(module)),
                ModificationTrackerManager.getInstance(module.project).getUastModelAndLibraryTracker()
            )
        }
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
            val byAnnotation = getPsiClassesByAnnotationCached(module, element)
            methodArguments.add(MethodArgumentClasses(element, byAnnotation))
        }
        return methodArguments
    }

    private fun isEqualsTypeOrClass(methodArgumentClasses: MethodArgumentClasses, eventPsiType: PsiType): Boolean {
        val argumentType = methodArgumentClasses.argumentType
        val argumentClasses = methodArgumentClasses.argumentClasses
        return if (argumentType != null) {
            eventPsiType.isAssignableFrom(argumentType)
        } else if (argumentClasses.isNotEmpty()) {
            argumentClasses.any { eventPsiType.resolveBeanPsiClass?.isEqualOrInheritor(it) ?: false }
        } else {
            false
        }
    }

    private class MethodCallArgumentTypes(val element: UCallExpression, val argumentType: PsiType)

}
