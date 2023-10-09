package com.esprito.spring.core.providers

import com.esprito.base.LibraryClassCache
import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses.APPLICATION_LISTENER
import com.esprito.spring.core.SpringCoreClasses.EVENT_LISTENER
import com.esprito.spring.core.SpringCoreClasses.EVENT_PUBLISHER
import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.SpringProperties.ON_APPLICATION_EVENT
import com.esprito.spring.core.SpringProperties.PUBLISH_EVENT_METHOD
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
import com.esprito.util.EspritoPsiUtil.findChildrenOfType
import com.esprito.util.EspritoPsiUtil.isEqualOrInheritor
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.esprito.util.EspritoPsiUtil.isPublic
import com.esprito.util.EspritoPsiUtil.isStatic
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
import com.intellij.psi.util.*
import com.intellij.uast.UastModificationTracker
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
                    .setTargets(findPublishEvents(psiMethod))
                    .setTooltipText(SpringCoreBundle.message("esprito.spring.gutter.tooltip.title.choose.event.publisher"))
                    .setPopupTitle(SpringCoreBundle.message("esprito.spring.gutter.popup.title.choose.event.publisher"))
                    .setEmptyPopupText(SpringCoreBundle.message("esprito.spring.gutter.notfound.title.choose.event.publisher"))

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
                        .setTooltipText(SpringCoreBundle.message("esprito.spring.gutter.tooltip.title.choose.event.listener"))
                        .setPopupTitle(SpringCoreBundle.message("esprito.spring.gutter.popup.title.choose.event.listener"))
                        .setEmptyPopupText(SpringCoreBundle.message("esprito.spring.gutter.notfound.title.choose.event.listener"))

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
        // @EventListener - method with parameters
        val eventListenerWithParameter = psiMethod.parameterList.parametersCount == 1
                && psiMethod.parameterList.parameters[0].type is PsiClassType
        if (eventListenerWithParameter) {
            return true
        }

        // @EventListener - annotation value or classes
        val module = ModuleUtilCore.findModuleForPsiElement(psiMethod) ?: return false
        val metaHolder = SpringSearchService.getInstance(module.project).getMetaAnnotations(module, EVENT_LISTENER)
        val annotationValues = metaHolder.getAnnotationMemberValues(psiMethod, setOf("value", "classes"))
        return annotationValues.isNotEmpty()
    }

    private fun isApplicationEventMethod(psiMethod: PsiMethod): Boolean {
        val module = ModuleUtilCore.findModuleForPsiElement(psiMethod) ?: return false
        val classes = LibraryClassCache.searchForLibraryClasses(module, listOf(APPLICATION_LISTENER))
        val supers = psiMethod.parentOfType<PsiClass>()?.supers ?: return false
        val byApplicationEvent = supers.asSequence()
            .map { psiClass -> classes.any { psiClass.isEqualOrInheritor(it) } }
            .any { it }

        return (psiMethod.name == ON_APPLICATION_EVENT) && byApplicationEvent
    }

    private fun findPublishEvents(psiMethod: PsiMethod): List<PsiMethodCallExpression> {
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
                UastModificationTracker.getInstance(module.project)
            )
        }
    }

    private fun getPsiClassesByAnnotation(module: Module, psiMethod: PsiMethod): Set<PsiClass> {
        val metaHolder = SpringSearchService.getInstance(module.project).getMetaAnnotations(module, EVENT_LISTENER)
        val annotationValues = metaHolder.getAnnotationMemberValues(psiMethod, setOf("value", "classes"))
        return annotationValues.asSequence()
            .map { it.findChildrenOfType<PsiJavaCodeReferenceElement>() }
            .filter { it.isNotEmpty() }.map { it[0].resolve() }
            .filter { it is PsiClass }.map { it as PsiClass }
            .toSet()
    }

    private fun getPublishMethods(module: Module): List<PsiMethod> {
        val eventPublisherClasses = SpringSearchService.getInstance(module.project).findClassesByQualifiedName(module, EVENT_PUBLISHER)
        return eventPublisherClasses.asSequence()
            .flatMap { it.findMethodsByName(PUBLISH_EVENT_METHOD, false).asSequence() }
            .filterNotNull()
            .toList()
    }

    private fun getPublishMethodCalls(methodArgumentTypes: List<MethodCallArgumentTypes>, eventPsiType: PsiType?, eventPsiClass: Set<PsiClass>): List<PsiMethodCallExpression> {
        val publishEvents = mutableListOf<PsiMethodCallExpression>()
        methodArgumentTypes.forEach { it ->
            val type = it.argumentType ?: return@forEach
            if (it.element is PsiMethodCallExpression) {
                if (eventPsiType != null) {
                    if (eventPsiType.isAssignableFrom(type)) {
                        publishEvents += it.element
                    }
                }
                else if (eventPsiClass.isNotEmpty() && eventPsiClass.any {type.resolveBeanPsiClass?.isEqualOrInheritor(it) == true}) {
                    publishEvents += it.element
                }
            }
        }
        return publishEvents
    }

    private fun getMethodArgumentTypes( module: Module): List<MethodCallArgumentTypes> {
        val publishMethods = getPublishMethods(module)
        return publishMethods
            .asSequence()
            .map { SpringSearchService.getInstance(module.project).searchReferenceByMethod(module, it) }
            .flatMap { it.asSequence() }
            .mapNotNull { it.element.parentOfType<PsiCallExpression>() }
            .map { MethodCallArgumentTypes(it) }
            .toList()
    }

    private fun isPublishEventMethods(uMethodCall: UCallExpression): Boolean {
        if (uMethodCall.methodName != PUBLISH_EVENT_METHOD) return false
        val psiMethod = uMethodCall.resolve() ?: return false
        val containClass = psiMethod.containingClass
        return containClass != null && InheritanceUtil.isInheritor(containClass, EVENT_PUBLISHER)
    }

    private fun findEventListeners(psiElement: PsiElement): Collection<PsiElement> {
        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return Collections.emptyList()
        val uCallExpression = psiElement.toUElementOfType<UCallExpression>() ?: return Collections.emptyList()
        if (uCallExpression.valueArgumentCount != 1) return Collections.emptyList()
        val eventPsiType = uCallExpression.valueArguments[0].getExpressionType() ?: return Collections.emptyList()

        val eventListenerMethods = mutableListOf<MethodArgumentClasses>()
        eventListenerMethods += getMethodByApplicationEventCached(module)
        eventListenerMethods += getMethodByEventListenerCached(module)

        return eventListenerMethods.asSequence()
            .filter { isEqualsTypeOrClass(it, eventPsiType) }
            .mapNotNull { it.element.navigationElement }
            .toList()
    }

    private fun getMethodByApplicationEventCached(module: Module): Collection<MethodArgumentClasses> {
        val cacheManager = CachedValuesManager.getManager(module.project)

        return cacheManager.getCachedValue(module) {
            CachedValueProvider.Result.create(
                getMethodArgumentClassesByApplicationEvent(module),
                UastModificationTracker.getInstance(module.project)
            )
        }
    }

    private fun getMethodByEventListenerCached(module: Module): Collection<MethodArgumentClasses> {
        val cacheManager = CachedValuesManager.getManager(module.project)

        return cacheManager.getCachedValue(module) {
            CachedValueProvider.Result.create(
                getMethodArgumentClassesByEventListener(module),
                UastModificationTracker.getInstance(module.project)
            )
        }
    }

    private fun getMethodArgumentClassesByApplicationEvent(module: Module): List<MethodArgumentClasses> {
        val scope = GlobalSearchScope.moduleWithDependenciesScope(module)
        val listenerClass =
            SpringSearchService.getInstance(module.project).findClassesByQualifiedName(module, APPLICATION_LISTENER)
        val publisherClass = listenerClass
            .flatMap { ClassInheritorsSearch.search(it, scope, true).findAll() }
            .filterNotNull()
        return publisherClass
            .flatMap { it.findMethodsByName(ON_APPLICATION_EVENT, false).asSequence() }
            .filterNotNull()
            .map { MethodArgumentClasses(it) }
    }

    private fun getMethodArgumentClassesByEventListener(module: Module): List<MethodArgumentClasses> {
        val methodArguments = mutableListOf<MethodArgumentClasses>()
        val scope = GlobalSearchScope.moduleWithDependenciesScope(module)
        val eventListenerClasses = SpringSearchService.getInstance(module.project).findClassesByQualifiedName(module, EVENT_LISTENER)
        for (eventListenerClass in eventListenerClasses) {
            val listenerMethods = AnnotatedElementsSearch.searchPsiMethods(eventListenerClass, scope)
            for (element in listenerMethods) {
                val byAnnotation = getPsiClassesByAnnotationCached(module, element)
                methodArguments.add(MethodArgumentClasses(element, byAnnotation))
            }
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

    class MethodCallArgumentTypes(method: PsiCallExpression) {
        var argumentType: PsiType? = null
        val element: PsiElement = method

        init {
            if (element is PsiMethodCallExpression) {
                val expression = element.argumentList.expressions
                if (expression.size == 1 && expression[0].type != null && expression[0].type is PsiType) {
                    argumentType = expression[0].type as PsiType
                }
            }
        }
    }

    class MethodArgumentClasses(method: PsiMethod, psiClassesFromAnnotation: Set<PsiClass>? = null) {
        var argumentClasses: Set<PsiClass> = emptySet()
        var argumentType: PsiType? = null
        val element: PsiElement = method

        init {
            if (element is PsiMethod) {
                if (element.parameterList.parametersCount == 1) {
                    argumentType = element.parameterList.parameters[0].type
                } else if (psiClassesFromAnnotation != null){
                    argumentClasses += psiClassesFromAnnotation
                }
            }
        }
    }

}