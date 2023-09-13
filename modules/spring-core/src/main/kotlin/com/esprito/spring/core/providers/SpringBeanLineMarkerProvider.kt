package com.esprito.spring.core.providers

import com.esprito.base.LibraryClassCache
import com.esprito.spring.core.JavaEeClasses
import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.util.SpringCoreUtil
import com.esprito.spring.core.util.SpringSearchService
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
import org.jetbrains.uast.*

class SpringBeanLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val processor = getLineMarkerElementProcessor(element) ?: return

        if (processor.isComponentClassOrBeanMethod()) {
            val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringBean)
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTargets(NotNullLazyValue.lazy { processor.findFieldsAndMethodsWithAutowired() })
                .setTooltipText(SpringCoreBundle.message("esprito.spring.gutter.tooltip.title.choose.autowired.candidate"))
                .setPopupTitle(SpringCoreBundle.message("esprito.spring.gutter.popup.title.choose.autowired.candidate"))
                .setEmptyPopupText(SpringCoreBundle.message("esprito.spring.gutter.notfound.title.choose.autowired.candidate"))
            result.add(builder.createLineMarkerInfo(element))
        } else if (processor.isFieldOrAutowiredParameter()) {
            val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringBeanDependencies)
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTargets(NotNullLazyValue.lazy { processor.getBeanComponentsTargets() })
                .setTooltipText(SpringCoreBundle.message("esprito.spring.gutter.tooltip.title.choose.bean.candidate"))
                .setPopupTitle(SpringCoreBundle.message("esprito.spring.gutter.popup.title.choose.bean.candidate"))
                .setEmptyPopupText(SpringCoreBundle.message("esprito.spring.gutter.notfound.title.choose.bean.candidate"))

            result.add(builder.createLineMarkerInfo(element))
        }
    }

    private fun getLineMarkerElementProcessor(element: PsiElement): LineMarkerElementProcessor? {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return null
        val uParent = getUParentForIdentifier(element) ?: return null
        return LineMarkerElementProcessor(element, module, uParent)
    }

    class LineMarkerElementProcessor(
        private val element: PsiElement,
        private val module: Module,
        private val uParent: UElement,
    ) {
        private val springSearchService = SpringSearchService.getInstance(module.project)

        fun isComponentClassOrBeanMethod(): Boolean {
            if (uParent is UClass && SpringCoreUtil.isSpringBeanCandidateClass(uParent.javaPsi)) {
                val isComponentExpression = uParent.javaPsi.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT)
                return isComponentExpression || findTargetClass() in springSearchService.getAllBeansClassesWithInheritors(module)
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

        fun isFieldOrAutowiredParameter(): Boolean =
            uParent is UField
                    && uParent.javaPsi?.let { it is PsiField && isAutowiredFieldExpression(it) } == true
                    || uParent is UMethod
                    && uParent.javaPsi.let { it is PsiMethod && it.isConstructor && isAutowiredConstructorExpression(it) }

        private fun getAutowiredFieldAnnotations(): Collection<PsiClass> {
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

        fun getBeanComponentsTargets(): Collection<PsiElement> {
            val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyList()
            val componentPsiClasses = springSearchService.getBeanPsiClassesAnnotatedByComponent(module)
            if (componentPsiClasses.isEmpty()) {
                return emptyList()
            }
            val resolvedPsiClass = element.parentOfType<PsiField>()?.returnPsiClass ?: return emptyList()

            return componentPsiClasses.filter { it == resolvedPsiClass }
        }


        private fun findTargetClass(): PsiClass? {
            if (element !is PsiIdentifier) {
                return null
            }

            return element.parentOfType<PsiMethod>()?.returnPsiClass  // method annotated by Bean
                ?: element.parentOfType<PsiClass>() // class annotated as Component
        }

        private fun PsiType.canResolveBeanClass(targetClasses: Set<PsiClass>): Boolean {
            if (resolvedDeepPsiClass in targetClasses) {
                // Bean[]
                // Bean
                return true
            }
            if (this !is PsiClassType) {
                return false
            }
            if (isCollection || isOptional) {
                // Collection<Bean>
                // Optional<Bean>
                val genericPsiClass = parameters.firstOrNull()?.resolvedPsiClass ?: return false
                return genericPsiClass in targetClasses
            }
            if (isMap && parameterCount == 2 && parameters[0].isString) {
                // Map<String, Bean>
                val genericPsiClass = parameters[1].resolvedPsiClass ?: return false
                return genericPsiClass in targetClasses
            }
            return false
        }

        fun findFieldsAndMethodsWithAutowired(): Collection<PsiElement> {
            val targetClass = findTargetClass() ?: return emptyList()
            val targetClasses = targetClass.supers.toSet() + targetClass

            val allAutowiredAnnotations = getAutowiredFieldAnnotations()
            val allAutowiredAnnotationsNames = allAutowiredAnnotations.mapNotNull { it.qualifiedName }

            val allBeans = springSearchService.getAllBeansClasses(module)

            val allFieldsWithAutowired = allBeans.asSequence().flatMap {
                it.allFields.asSequence()
                    .filter { it.isAnnotatedBy(allAutowiredAnnotationsNames) }
                    .filter { it.type.canResolveBeanClass(targetClasses) }
            }.toSet()

            val allMethodsWithAutowired = allBeans.asSequence().flatMap { bean ->
                bean.allMethods.asSequence()
                    .filter {
                        it.isAnnotatedBy(allAutowiredAnnotationsNames)
                                || it.isAnnotatedBy(SpringCoreClasses.BEAN)
                                || it.isConstructor && bean in springSearchService.getBeanPsiClassesAnnotatedByComponent(module)
                    }
                    .flatMap { it.parameterList.parameters.asSequence() }
                    .filter { it.type.canResolveBeanClass(targetClasses) }
            }.toSet()

            return allFieldsWithAutowired + allMethodsWithAutowired
        }
    }

}
