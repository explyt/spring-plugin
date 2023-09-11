package com.esprito.spring.core.providers

import com.esprito.base.LibraryClassCache
import com.esprito.spring.core.JavaEeClasses
import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.util.SpringCoreUtil
import com.esprito.spring.core.util.SpringSearchUtil
import com.esprito.util.EspritoPsiUtil.isAnnotatedBy
import com.esprito.util.EspritoPsiUtil.isCollection
import com.esprito.util.EspritoPsiUtil.isMap
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.esprito.util.EspritoPsiUtil.isOptional
import com.esprito.util.EspritoPsiUtil.isString
import com.esprito.util.EspritoPsiUtil.resolvedDeepPsiClass
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
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UField
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getUParentForIdentifier

class SpringBeanLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val uParent = getUParentForIdentifier(element)
        if (isComponentClassOrBeanMethod(element)) {
            val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringBean)
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTargets(NotNullLazyValue.lazy { findFieldsAndMethodsWithAutowired(element) })
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
            return isComponentExpression || findTargetClass(element) in SpringSearchUtil.getAllBeansClassesWithInheritors(module)
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
        return CachedValuesManager.getManager(module.project).getCachedValue(module) {
            CachedValueProvider.Result(
                run {
                    val annotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(module, SpringCoreClasses.AUTOWIRED, false)
                    annotations += LibraryClassCache.searchForLibraryClasses(module, JavaEeClasses.INJECT.allFqns + JavaEeClasses.RESOURCE.allFqns)
                    return@run annotations
                },
                UastModificationTracker.getInstance(module.project)
            )
        }
    }

    private fun getBeanComponentsTargets(element: PsiElement): Collection<PsiElement> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyList()
        val componentPsiClasses = SpringSearchUtil.getBeanPsiClassesAnnotatedByComponent(module)
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

    private fun PsiType.matchesTarget(targetClasses: Set<PsiClass>): Boolean {
        if (resolvedDeepPsiClass in targetClasses) {
            return true
        }
        if (this !is PsiClassType) {
            return false
        }
        if (isCollection || isOptional) {
            val genericPsiClass = parameters.firstOrNull()?.resolvedPsiClass ?: return false
            return genericPsiClass in targetClasses
        }
        if (isMap && parameterCount == 2) {
            if (parameters[0].isString) {
                val genericPsiClass = parameters[1].resolvedPsiClass ?: return false
                return genericPsiClass in targetClasses
            }
        }
        return false
    }

    private fun findFieldsAndMethodsWithAutowired(element: PsiElement): Collection<PsiElement> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyList()
        val targetClass = findTargetClass(element) ?: return emptyList()
        val targetClasses = targetClass.supers.toSet() + targetClass

        val allAutowiredAnnotations = getAutowiredFieldAnnotations(module)
        val allAutowiredAnnotationsNames = allAutowiredAnnotations.mapNotNull { it.qualifiedName }

        val allBeans = SpringSearchUtil.getAllBeansClasses(module)

        val allFieldsWithAutowired = allBeans.asSequence().flatMap {
            it.allFields.asSequence()
                .filter { it.isAnnotatedBy(allAutowiredAnnotationsNames) }
                .filter { it.type.matchesTarget(targetClasses) }
        }.toSet()

        val allMethodsWithAutowired = allBeans.asSequence().flatMap { bean ->
            bean.allMethods.asSequence()
                .filter {
                    it.isAnnotatedBy(allAutowiredAnnotationsNames)
                        || it.isAnnotatedBy(SpringCoreClasses.BEAN)
                        || it.isConstructor && bean in SpringSearchUtil.getBeanPsiClassesAnnotatedByComponent(module)
                }
                .flatMap { it.parameterList.parameters.asSequence() }
                .filter { it.type.matchesTarget(targetClasses) }
        }.toSet()

        return allFieldsWithAutowired + allMethodsWithAutowired
    }

}
