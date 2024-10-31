package com.esprito.spring.core.service

import com.esprito.base.LibraryClassCache
import com.esprito.spring.core.SpringCoreClasses.ANNOTATION_CONFIG_CONTEXT
import com.esprito.util.ExplytPsiUtil.resolvedPsiClass
import com.intellij.codeInspection.isInheritorOf
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.util.Query
import org.jetbrains.uast.*

object AnnotationConfigApplicationService {
    fun getRootClasses(
        searchRootClasses: List<PsiClass>, rootModuleData: List<PackageScanService.ModuleRootData>
    ): List<PsiClass> {
        if (!(searchRootClasses.size == 1 && rootModuleData.isEmpty()
                    && Registry.`is`("esprito.spring.root.runConfiguration"))
        ) {
            return emptyList()
        }
        val mainClass = searchRootClasses[0]
        val module = ModuleUtilCore.findModuleForPsiElement(mainClass) ?: return emptyList()
        val annotationContext = LibraryClassCache
            .searchForLibraryClass(module, ANNOTATION_CONFIG_CONTEXT).toUElement() as? UClass
            ?: return emptyList()

        val findRegisterMethods = findRegisterClassMethod(annotationContext)
        val findConstructors = findConstructorClassMethod(annotationContext)
        val allMethods = findRegisterMethods + findConstructors

        return findComponentClasses(allMethods, mainClass, module.moduleScope)
    }

    private fun findRegisterClassMethod(annotationContext: UClass): List<UMethod> {
        return annotationContext.methods.asSequence()
            .filter { it.name == "register" }
            .filter { filterByParameterClass(it) }
            .toList()
    }

    private fun findConstructorClassMethod(annotationContext: UClass): List<UMethod> {
        return annotationContext.methods.asSequence()
            .filter { it.name == "AnnotationConfigApplicationContext" }
            .filter { filterByParameterClass(it) }
            .toList()
    }

    private fun findComponentClasses(
        uMethods: List<UMethod>, mainClass: PsiClass, moduleScope: GlobalSearchScope
    ): List<PsiClass> {
        return uMethods.asSequence()
            .flatMap { psiReferences(it, moduleScope) }
            .filter { it.element.containingFile.virtualFile == mainClass.containingFile.virtualFile }
            .mapNotNull { resolveConfigClass(it) }
            .distinct().toList()
    }

    private fun psiReferences(it: UMethod, moduleScope: GlobalSearchScope): Query<PsiReference> {
        val search = MethodReferencesSearch.search(it.javaPsi, moduleScope, false)
        return search
    }

    private fun resolveConfigClass(it: PsiReference): PsiClass? {
        val uCallExpression = it.element.getUastParentOfType<UCallExpression>() ?: return null
        if (uCallExpression.valueArgumentCount != 1) return null
        return when (val uExpression = uCallExpression.valueArguments[0]) {
            is UClassLiteralExpression -> uExpression.type?.resolvedPsiClass

            is UQualifiedReferenceExpression ->
                (uExpression.receiver as? UClassLiteralExpression)?.type?.resolvedPsiClass

            else -> null
        }
    }

    private fun filterByParameterClass(it: UMethod): Boolean {
        if (it.uastParameters.size != 1) return false
        return it.uastParameters[0].typeReference?.type?.deepComponentType
            ?.isInheritorOf(Class::class.java.canonicalName) == true
    }
}