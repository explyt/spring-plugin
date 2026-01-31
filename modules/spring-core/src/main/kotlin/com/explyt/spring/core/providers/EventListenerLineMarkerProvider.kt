/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core.providers

import com.explyt.base.LibraryClassCache
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
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.parentOfType
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
                (isEventListenerMethod(psiMethod) || isApplicationEventMethod(psiMethod))
            ) {
                val builder = NavigationGutterIconBuilder.create(SpringIcons.EventPublisher)
                    .setAlignment(GutterIconRenderer.Alignment.LEFT)
                    .setTargets(NotNullLazyValue.lazy { findPublishEvents(psiMethod) })
                    .setTooltipText(SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.event.publisher"))
                    .setPopupTitle(SpringCoreBundle.message("explyt.spring.gutter.popup.title.choose.event.publisher"))
                    .setEmptyPopupText(SpringCoreBundle.message("explyt.spring.gutter.notfound.title.choose.event.publisher"))

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
                    val builder = NavigationGutterIconBuilder.create(SpringIcons.EventListener)
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

    private fun findEventListeners(psiElement: PsiElement): Collection<PsiElement> {
        StatisticService.getInstance().addActionUsage(StatisticActionId.GUTTER_TARGET_EVENT_LISTENER)
        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return Collections.emptyList()
        val uCallExpression = psiElement.toUElementOfType<UCallExpression>() ?: return Collections.emptyList()
        if (uCallExpression.valueArgumentCount != 1) return Collections.emptyList()
        val eventPsiType = uCallExpression.valueArguments[0].getExpressionType() ?: return Collections.emptyList()

        val eventListenerMethods = mutableListOf<MethodArgumentClasses>()
        eventListenerMethods += getMethodsByApplicationEventCached(module)
        eventListenerMethods += getMethodsByEventListenerCached(module)

        return eventListenerMethods.asSequence()
            .filter { isEqualsTypeOrClass(it, eventPsiType) }
            .map { it.element.navigationElement }
            .toList()
    }

    private fun getMethodsByApplicationEventCached(module: Module): Collection<MethodArgumentClasses> {
        val cacheManager = CachedValuesManager.getManager(module.project)

        return cacheManager.getCachedValue(module) {
            CachedValueProvider.Result.create(
                getMethodsByApplicationEvent(module),
                ModificationTrackerManager.getInstance(module.project).getUastModelAndLibraryTracker()
            )
        }
    }

    private fun getMethodsByEventListenerCached(module: Module): Collection<MethodArgumentClasses> {
        val cacheManager = CachedValuesManager.getManager(module.project)

        return cacheManager.getCachedValue(module) {
            CachedValueProvider.Result.create(
                getMethodsByEventListener(module),
                ModificationTrackerManager.getInstance(module.project).getUastModelAndLibraryTracker()
            )
        }
    }

    private fun getMethodsByApplicationEvent(module: Module): List<MethodArgumentClasses> {
        val scope = GlobalSearchScope.moduleWithDependenciesScope(module)
        val listenerClass = SpringSearchUtils.findAnnotationClassesByQualifiedName(module, APPLICATION_LISTENER)
        return listenerClass.asSequence()
            .flatMap { ClassInheritorsSearch.search(it, scope, true).findAll() }
            .filterNotNull()
            .flatMap { it.findMethodsByName(ON_APPLICATION_EVENT, false).asSequence() }
            .filterNotNull()
            .map { MethodArgumentClasses(it) }
            .toList()
    }

    private fun getMethodsByEventListener(module: Module): List<MethodArgumentClasses> {
        val scope = GlobalSearchScope.moduleWithDependenciesScope(module)
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

}