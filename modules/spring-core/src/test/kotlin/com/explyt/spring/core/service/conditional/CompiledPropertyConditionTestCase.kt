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
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope

abstract class CompiledPropertyConditionTestCase : BeanConditionTestCase() {

    enum class PropertyCondition(val java: String, val kotlin: String) {
        NAME_HAVING_VALUE(
            "@ConditionalOnProperty(name = \"lib.feature.enabled\", havingValue = \"true\")",
            "@ConditionalOnProperty(name = [\"lib.feature.enabled\"], havingValue = \"true\")"
        ),
        PREFIX_AND_NAME(
            "@ConditionalOnProperty(prefix = \"lib.feature\", name = \"enabled\")",
            "@ConditionalOnProperty(prefix = \"lib.feature\", name = [\"enabled\"])"
        ),
        VALUE_ALIAS(
            "@ConditionalOnProperty(value = \"lib.jndi-name\")",
            "@ConditionalOnProperty(value = [\"lib.jndi-name\"])"
        )
    }

    protected abstract fun addProjectConfiguration(condition: PropertyCondition)

    fun testSourceNameWithMatchingHavingValueIsActive() {
        addProperties("lib.feature.enabled=true")
        val method = projectBeanMethod(PropertyCondition.NAME_HAVING_VALUE)
        assertPropertyDefined("lib.feature.enabled")

        assertEquals(ConditionVerdict.Active, verdictOf(method))
    }

    fun testSourceNameWithMismatchingHavingValueIsInactive() {
        addProperties("lib.feature.enabled=false")
        val method = projectBeanMethod(PropertyCondition.NAME_HAVING_VALUE)
        assertPropertyDefined("lib.feature.enabled")

        assertInactiveNaming(verdictOf(method), "lib.feature.enabled")
    }

    fun testSourcePrefixJoinedWithNameIsActive() {
        addProperties("lib.feature.enabled=true")
        val method = projectBeanMethod(PropertyCondition.PREFIX_AND_NAME)
        assertPropertyDefined("lib.feature.enabled")

        assertEquals(ConditionVerdict.Active, verdictOf(method))
    }

    fun testSourceValueAliasWithMissingPropertyIsInactive() {
        addProperties("lib.other=present")
        val method = projectBeanMethod(PropertyCondition.VALUE_ALIAS)
        assertPropertyDefined("lib.other")
        assertPropertyUndefined("lib.jndi-name")

        assertInactiveNaming(verdictOf(method), "lib.jndi-name")
    }

    protected fun libraryBeanMethod(condition: PropertyCondition): PsiMethod {
        TestCompiledLibrary.attach(
            module, testRootDisposable, "vendor-property-condition",
            mapOf(
                "com/vendor/lib/VendorService.java" to "package com.vendor.lib;\npublic class VendorService {}\n",
                "com/vendor/lib/$LIBRARY_CONFIGURATION.java" to """
                    package com.vendor.lib;

                    import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
                    import org.springframework.context.annotation.Bean;
                    import org.springframework.context.annotation.Configuration;

                    @Configuration
                    ${condition.java}
                    public class $LIBRARY_CONFIGURATION {
                        @Bean
                        public VendorService vendorService() { return new VendorService(); }
                    }
                """.trimIndent()
            )
        )
        ModificationTrackerManager.getInstance(project).invalidateAll()
        val configuration = JavaPsiFacade.getInstance(project)
            .findClass("com.vendor.lib.$LIBRARY_CONFIGURATION", GlobalSearchScope.allScope(project))
        assertNotNull("Precondition: the compiled library configuration must resolve", configuration)
        val file = configuration!!.containingFile.virtualFile
        assertTrue(
            "Precondition: $LIBRARY_CONFIGURATION must come from compiled library classes",
            ProjectFileIndex.getInstance(project).isInLibraryClasses(file)
        )
        assertFalse(
            "Precondition: $LIBRARY_CONFIGURATION must be outside the project scope",
            ProjectScope.getProjectScope(project).contains(file)
        )
        assertFalse(
            "Precondition: $LIBRARY_CONFIGURATION must have no attached sources",
            ProjectFileIndex.getInstance(project).isInLibrarySource(file)
        )
        assertAnnotatedBy(configuration, CONDITIONAL_ON_PROPERTY)
        return configuration.findMethodsByName("vendorService", false).single().also(::assertInBeanModel)
    }

    protected fun assertInactiveNaming(verdict: ConditionVerdict?, key: String) {
        assertTrue("Expected an Inactive verdict naming $key, got $verdict", verdict is ConditionVerdict.Inactive)
        val evidence = (verdict as ConditionVerdict.Inactive).condition
        assertEquals(CONDITIONAL_ON_PROPERTY, evidence.annotationFqn)
        assertEquals(ConditionReason.NOT_MATCHED, evidence.reason)
        assertTrue("Detail '${evidence.detail}' must mention '$key'", evidence.detail.orEmpty().contains(key))
    }

    protected fun verdictOf(method: PsiMethod): ConditionVerdict? {
        ModificationTrackerManager.getInstance(project).invalidateAll()
        return SpringSearchService.getInstance(project).conditionVerdictOf(method, module)
    }

    private fun projectBeanMethod(condition: PropertyCondition): PsiMethod {
        addProjectConfiguration(condition)
        ModificationTrackerManager.getInstance(project).invalidateAll()
        val configuration = beanClass("com.app.$PROJECT_CONFIGURATION")
        assertTrue(
            "Precondition: $PROJECT_CONFIGURATION must be project source",
            ProjectScope.getProjectScope(project).contains(configuration.containingFile.virtualFile)
        )
        assertAnnotatedBy(configuration, CONDITIONAL_ON_PROPERTY)
        return configuration.findMethodsByName("projectService", false).single().also(::assertInBeanModel)
    }

    private fun assertInBeanModel(method: PsiMethod) {
        ModificationTrackerManager.getInstance(project).invalidateAll()
        val facade = SpringSearchServiceFacade.getInstance(project)
        val found = facade.getAllActiveBeans(module) + facade.getExcludedBeansClasses(module)
        assertTrue(
            "Precondition: ${method.containingClass?.qualifiedName}#${method.name} must be in the bean model",
            found.any { it.psiMember == method }
        )
    }

    companion object {
        const val LIBRARY_CONFIGURATION = "VendorFeatureConfiguration"
        const val PROJECT_CONFIGURATION = "ProjectFeatureConfiguration"
    }
}
