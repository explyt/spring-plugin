package com.esprito.spring.core.providers

import com.esprito.base.LibraryClassCache
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringIcons
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.search.GlobalSearchScope.moduleWithDependenciesAndLibrariesScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.parentOfType
import org.jetbrains.uast.*

class SpringBeanLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val uParent = getUParentForIdentifier(element)
        if (uParent is UClass
            && isComponentExpression(uParent.javaPsi)
        ) {
            // Add Line Marker for @EventListener methods

            val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringBean)
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                .setTooltipText("Navigate to autowired fields")
                .setTargets(getAutowiredTargets(element))

            result.add(builder.createLineMarkerInfo(element))

        } else if (uParent is UField
                && uParent.javaPsi?.let { it is PsiField && isAutowiredExpression(it) } == true
        ) {
            // Add Line Marker for publishEvent(event) expressions
            val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringBeanDependencies)
                .setAlignment(GutterIconRenderer.Alignment.LEFT)
                // Example of Lazy marker evaluation
                .setTargets(getBeanComponentsTargets(element))
                .setTooltipText("Navigate to bean components")

            result.add(builder.createLineMarkerInfo(element))
        }
    }

    private fun isComponentExpression(javaPsi: PsiClass): Boolean {
        return javaPsi.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT)
    }

    private fun isAutowiredExpression(javaPsi: PsiField): Boolean {
        return javaPsi.isMetaAnnotatedBy(SpringCoreClasses.AUTOWIRED) // TODO: Inject, Resource (javax, jakarta)
    }

    private fun getBeanComponentsTargets(element: PsiElement): Collection<PsiElement> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyList()
        // TODO: find all meta annotations and find annotated classes
        val componentAnnotationClass = LibraryClassCache.searchForLibraryClass(module, SpringCoreClasses.COMPONENT)
            ?: return emptyList()
        val scope = moduleWithDependenciesAndLibrariesScope(module)
        val searchPsiClasses = AnnotatedElementsSearch.searchPsiClasses(componentAnnotationClass, scope)

        val psiTypeElements = element.parentOfType<PsiField>()?.childrenOfType<PsiTypeElement>() ?: return emptyList()
        val psiType = psiTypeElements.firstOrNull()?.type
        val resolvedPsiClass = (psiType as? PsiClassType)?.resolve() ?: return emptyList()
        return searchPsiClasses.filter { it == resolvedPsiClass }

    }

    private fun getAutowiredTargets(element: PsiElement): Collection<PsiElement> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyList()
        // TODO: find all meta annotations and find annotated classes
        val componentAnnotationClass = LibraryClassCache.searchForLibraryClass(module, SpringCoreClasses.COMPONENT)
            ?: return emptyList()
        val scope = moduleWithDependenciesAndLibrariesScope(module)
        val searchPsiClasses = AnnotatedElementsSearch.searchPsiClasses(componentAnnotationClass, scope)

        return searchPsiClasses.toList()
    }


}
