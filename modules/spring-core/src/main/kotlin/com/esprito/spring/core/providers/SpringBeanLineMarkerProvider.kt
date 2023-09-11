package com.esprito.spring.core.providers

import com.esprito.base.LibraryClassCache
import com.esprito.spring.core.JavaEeClasses
import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.util.SpringCoreUtil
import com.esprito.spring.core.util.SpringSearchUtil
import com.esprito.util.EspritoPsiUtil.isAnnotatedBy
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.esprito.util.EspritoPsiUtil.resolvedPsiClass
import com.esprito.util.EspritoPsiUtil.returnPsiClass
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.*
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.parentOfType
import com.intellij.uast.UastModificationTracker
import com.intellij.util.SmartList
import org.jetbrains.uast.*

class SpringBeanLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val uParent = getUParentForIdentifier(element)
        if (isComponentClassOrBeanMethod(element)) {
            val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringBean)
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTargets(NotNullLazyValue.lazy { findFieldsWithAutowired(element) })
                .setTooltipText(SpringCoreBundle.message("esprito.spring.gutter.tooltip.title.choose.autowired.candidate"))
                .setPopupTitle(SpringCoreBundle.message("esprito.spring.gutter.popup.title.choose.autowired.candidate"))
                .setEmptyPopupText(SpringCoreBundle.message("esprito.spring.gutter.notfound.title.choose.autowired.candidate"))
            result.add(builder.createLineMarkerInfo(element))
        } else if (uParent is UField
                && uParent.javaPsi?.let { it is PsiField && isAutowiredFieldExpression(it) } == true
            || uParent is UMethod
            && uParent.javaPsi.let { it is PsiMethod && it.isConstructor && isAutowiredConstructorExpression(it) }
        ) {
            // Add Line Marker for publishEvent(event) expressions
            val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringBeanDependencies)
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                // Example of Lazy marker evaluation
                .setTargets(NotNullLazyValue.lazy { getBeanComponentsTargets(element) })
                .setTooltipText(SpringCoreBundle.message("esprito.spring.gutter.tooltip.title.choose.bean.candidate"))
                .setPopupTitle(SpringCoreBundle.message("esprito.spring.gutter.popup.title.choose.bean.candidate"))
                .setEmptyPopupText(SpringCoreBundle.message("esprito.spring.gutter.notfound.title.choose.bean.candidate"))

            result.add(builder.createLineMarkerInfo(element))
        }
    }

    // Is Component class or Bean method
    private fun isComponentClassOrBeanMethod(element: PsiElement): Boolean {
        val uParent = getUParentForIdentifier(element) ?: return false
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return false
        if (uParent is UClass && SpringCoreUtil.isSpringBeanCandidateClass(uParent.javaPsi)) {
            val isComponentExpression = uParent.javaPsi.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT)
            return isComponentExpression || findTargetClass(element) in SpringSearchUtil.findAllBeanClasses(module)
        }
        if (uParent is UMethod) {
            return uParent.javaPsi.isMetaAnnotatedBy(SpringCoreClasses.BEAN)
        }
        return false
    }


    private fun isAutowiredFieldExpression(javaPsi: PsiField): Boolean {
        return javaPsi.isMetaAnnotatedBy(SpringCoreClasses.AUTOWIRED)
                || javaPsi.isAnnotatedBy(JavaEeClasses.INJECT.allFqns)
                || javaPsi.isAnnotatedBy(JavaEeClasses.RESOURCE.allFqns)
    }

    private fun isAutowiredConstructorExpression(javaPsi: PsiMethod): Boolean {
        return javaPsi.isMetaAnnotatedBy(SpringCoreClasses.AUTOWIRED)
                || javaPsi.isAnnotatedBy(JavaEeClasses.INJECT.allFqns)
    }

    private fun getAutowiredFieldAnnotations(module: Module): Collection<PsiClass> {
        // TODO: cache result
        val annotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(module, SpringCoreClasses.AUTOWIRED, false)
        annotations += LibraryClassCache.searchForLibraryClasses(module, JavaEeClasses.INJECT.allFqns + JavaEeClasses.RESOURCE.allFqns)
        return annotations
    }

    private fun getBeanComponentsTargets(element: PsiElement): Collection<PsiElement> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyList()
        val componentPsiClasses = SpringSearchUtil.getComponentPsiClasses(module)
        if (componentPsiClasses.isEmpty()) {
            return emptyList()
        }
        val resolvedPsiClass = element.parentOfType<PsiField>()?.returnPsiClass ?: return emptyList()

        return componentPsiClasses.filter { it == resolvedPsiClass }
    }


    private fun findTargetClass(element: PsiElement): PsiClass? {
        // todo? cache?
        if (element !is PsiIdentifier) {
            return null
        }

        return element.parentOfType<PsiMethod>()?.returnPsiClass ?:
            element.parentOfType<PsiClass>()

    }

    private fun getAllBeansClassesFromCache(module: Module): Set<PsiClass> {
        val project = module.project
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                SpringSearchUtil.findAllBeanClasses(module),
                UastModificationTracker.getInstance(project)
            )
        }
    }

    private fun findFieldsWithAutowired(element: PsiElement): Collection<PsiElement> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyList()
        val targetClass = findTargetClass(element) ?: return emptyList()
        val targetClasses = targetClass.supers.toSet() + targetClass

        val allAutowiredAnnotations = getAutowiredFieldAnnotations(module)
        val allAutowiredAnnotationsNames = allAutowiredAnnotations.mapNotNull { it.qualifiedName }

        val allBeans = getAllBeansClassesFromCache(module)

        val allFieldsWithAutowired = allBeans.asSequence().flatMap {
            it.allFields.asSequence()
                .filter { it.isAnnotatedBy(allAutowiredAnnotationsNames) }
                .filter { it.type.resolvedPsiClass in targetClasses }
        }.toSet()

        val allMethodsWithAutowired = allBeans.asSequence().flatMap { bean ->
            bean.allMethods.asSequence()
                .filter { it.isAnnotatedBy(allAutowiredAnnotationsNames)
                        || it.isConstructor && bean in SpringSearchUtil.getComponentPsiClasses(module) }
                .flatMap { it.parameterList.parameters.asSequence() }
                .filter { it.type.resolvedPsiClass in targetClasses }
        }.toSet()

        return allFieldsWithAutowired + allMethodsWithAutowired

        /**
         *
         * interface I {} // failed
         *
         * class E {} // failed
         *
         * @Component
         * class A extends E implements I {}
         * @Component
         * class B extends E implements I {}
         *
         * // 1. passed
         * @Component
         * class Foo1 {
         *     @Autowired
         *     A a; // Target A
         * }
         * // From A it navigates to a
         *
         *
         * // 2. partial
         * @Component
         * class Foo2 {
         *     @Autowired I i; // Target A & B - failed
         *     @Autowired E e; // Target A & E & I - failed
         * }
         * // From A it navigates to i - passed
         * // From I it navigates to i - failed
         *
         *
         * // 3. failed
         * @Component
         * class Foo3 {
         *     @Autowired Collection<I> setI; // Target A - failed
         *     @Autowired I[] arrayI; // Target A - failed
         *     @Autowired Collection<E> setE; // Target A & E & I - failed
         *     @Autowired E[] arrayE; // Target A - failed
         *     @Autowired Collection<A> setA; // Target A - failed
         *     @Autowired A[] arrayA; // Target A - failed
         * }
         * // From A it navigates to setI - failed
         * // From A it navigates to arrayI - failed
         * // From A it navigates to setE - failed
         * // From A it navigates to arrayE - failed
         * // From A it navigates to setA - failed
         * // From A it navigates to arrayA - failed
         *
         * // From I it navigates to setI - failed
         * // From I it navigates to arrayI - failed
         *
         * // From E it navigates to setE - failed
         * // From E it navigates to arrayE - failed
         *
         *
         * // 4. partial
         * @Component
         * class Foo4 {
         *     private final A a;
         *     Foo4(A a) {
         *         this.a = a;
         *     } // Target A - failed
         * }
         * // From A it navigates to parameter Foo(A a) - passed
         *
         */
    }

    private fun getWriteMetaAnnotations(module: Module): List<String> {
        return CachedValuesManager.getManager(module.project).getCachedValue(
            module
        ) {
            CachedValueProvider.Result.create(
                getMetaAnnotations(
                    module,
                    "org.springframework.scheduling.annotation.Scheduled",
                    "org.springframework.context.event.EventListener",
                    "org.springframework.beans.factory.annotation.Autowired",
                    "org.springframework.context.annotation.Bean",
                    "org.springframework.beans.factory.annotation.Value"
                ),
                UastModificationTracker.getInstance(module.project)
            )
        }
    }

    private fun getMetaAnnotations(module: Module, vararg annotations: String): List<String> {
        val annotationTypes: MutableList<PsiClass> = SmartList()
        for (annotation in annotations) {
            annotationTypes.addAll(MetaAnnotationUtil.getAnnotationTypesWithChildren(module, annotation, false))
        }
        return annotationTypes.mapNotNull { aClass: PsiClass ->
            val fqn = aClass.qualifiedName
            if (fqn in annotations) null else fqn
        }
    }


}
