/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.test.util.TestCompiledLibrary
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope

abstract class CompiledConditionTestCase : BeanConditionTestCase() {

    protected fun compiledBeanMethod(
        condition: String,
        annotationFqn: String,
        supportSources: Map<String, String> = emptyMap(),
    ): PsiMethod {
        val sources = supportSources.mapKeys { (className, _) -> "$LIBRARY_DIRECTORY/$className.java" } + mapOf(
            "$LIBRARY_DIRECTORY/VendorService.java" to "package $LIBRARY_PACKAGE;\npublic class VendorService {}\n",
            "$LIBRARY_DIRECTORY/$LIBRARY_CONFIGURATION.java" to """
                package $LIBRARY_PACKAGE;

                import org.springframework.boot.autoconfigure.condition.*;
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;

                @Configuration
                $condition
                public class $LIBRARY_CONFIGURATION {
                    @Bean
                    public VendorService $LIBRARY_BEAN() { return new VendorService(); }
                }
            """.trimIndent()
        )
        TestCompiledLibrary.attach(module, testRootDisposable, LIBRARY_NAME, sources)
        ModificationTrackerManager.getInstance(project).invalidateAll()
        val configuration = compiledClass("$LIBRARY_PACKAGE.$LIBRARY_CONFIGURATION")
        assertAnnotatedBy(configuration, annotationFqn)
        return configuration.findMethodsByName(LIBRARY_BEAN, false).single().also(::assertInBeanModel)
    }

    protected fun compiledClass(qualifiedName: String): PsiClass {
        val psiClass = JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.allScope(project))
        assertNotNull("Precondition: $qualifiedName must resolve", psiClass)
        val file = psiClass!!.containingFile.virtualFile
        val fileIndex = ProjectFileIndex.getInstance(project)
        assertTrue(
            "Precondition: $qualifiedName must come from compiled library classes",
            fileIndex.isInLibraryClasses(file)
        )
        assertFalse(
            "Precondition: $qualifiedName must be outside the project scope",
            ProjectScope.getProjectScope(project).contains(file)
        )
        assertFalse("Precondition: $qualifiedName must have no attached sources", fileIndex.isInLibrarySource(file))
        return psiClass
    }

    protected fun assertOnClasspath(vararg classes: String) = classes.forEach {
        assertNotNull("Precondition: $it must be on the classpath", findOnModuleClasspath(it))
    }

    protected fun assertNotOnClasspath(vararg classes: String) = classes.forEach {
        assertNull("Precondition: $it must be absent from the classpath", findOnModuleClasspath(it))
    }

    private fun findOnModuleClasspath(qualifiedName: String): PsiClass? = JavaPsiFacade.getInstance(project)
        .findClass(qualifiedName, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module))

    protected fun assertInactiveWith(verdict: ConditionVerdict?, annotationFqn: String, detailFragment: String) {
        assertTrue(
            "Expected an Inactive verdict of $annotationFqn naming '$detailFragment', got $verdict",
            verdict is ConditionVerdict.Inactive
        )
        val evidence = (verdict as ConditionVerdict.Inactive).condition
        assertEquals(annotationFqn, evidence.annotationFqn)
        assertEquals(ConditionReason.NOT_MATCHED, evidence.reason)
        assertTrue(
            "Detail '${evidence.detail}' must mention '$detailFragment'",
            evidence.detail.orEmpty().contains(detailFragment)
        )
    }

    protected fun verdictOf(method: PsiMethod): ConditionVerdict? {
        ModificationTrackerManager.getInstance(project).invalidateAll()
        return SpringSearchService.getInstance(project).conditionVerdictOf(method, module)
    }

    protected fun assertInBeanModel(method: PsiMethod) {
        ModificationTrackerManager.getInstance(project).invalidateAll()
        val facade = SpringSearchServiceFacade.getInstance(project)
        val found = facade.getAllActiveBeans(module) + facade.getExcludedBeansClasses(module)
        assertTrue(
            "Precondition: ${method.containingClass?.qualifiedName}#${method.name} must be in the bean model",
            found.any { it.psiMember == method }
        )
    }

    companion object {
        const val LIBRARY_NAME = "vendor-compiled-condition"
        const val LIBRARY_PACKAGE = "com.vendor.lib"
        const val LIBRARY_DIRECTORY = "com/vendor/lib"
        const val LIBRARY_CONFIGURATION = "VendorFeatureConfiguration"
        const val LIBRARY_BEAN = "vendorService"

        const val CONDITIONAL_ON_MISSING_BEAN =
            "org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean"
        const val CONDITIONAL_ON_WEB_APPLICATION =
            "org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication"
    }
}
