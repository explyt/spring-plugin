/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.test.ExplytBaseLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElementOfType

abstract class BeanConditionTestCase : ExplytBaseLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    protected abstract val fixtures: ConditionFixtures

    override fun setUp() {
        super.setUp()
        fixtures.addApplication()
    }

    protected fun addProperties(vararg lines: String) {
        myFixture.addFileToProject("application.properties", lines.joinToString("\n"))
        ModificationTrackerManager.getInstance(project).invalidateAll()
    }

    protected fun activeBeans(): Set<String> {
        ModificationTrackerManager.getInstance(project).invalidateAll()
        return projectBeanKeys(SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module))
    }

    protected fun excludedBeans(): Set<String> {
        ModificationTrackerManager.getInstance(project).invalidateAll()
        return projectBeanKeys(SpringSearchServiceFacade.getInstance(project).getExcludedBeansClasses(module))
    }

    protected fun beanClass(qualifiedName: String): PsiClass = myFixture.findClass(qualifiedName)

    protected fun beanMethod(classQualifiedName: String, methodName: String): PsiMethod =
        beanClass(classQualifiedName).findMethodsByName(methodName, false).single()

    protected fun assertAnnotatedBy(member: PsiMember, annotationFqn: String) {
        val annotationClass = JavaPsiFacade.getInstance(project)
            .findClass(annotationFqn, GlobalSearchScope.allScope(project))
        assertNotNull("Precondition: $annotationFqn must come from the Spring fixture library", annotationClass)
        assertTrue(
            "Precondition: ${member.name} must carry a resolved $annotationFqn",
            member.modifierList?.hasAnnotation(annotationFqn) == true
        )
    }

    protected fun assertProfileValueUnresolved(member: PsiClass) {
        val annotation = member.toUElementOfType<UClass>()!!.uAnnotations
            .single { it.qualifiedName == SpringCoreClasses.PROFILE }
        val value = annotation.findDeclaredAttributeValue("value")!!
        val element = if (value is UCallExpression) value.valueArguments.single() else value
        assertNull("Precondition: the profile constant must not resolve", element.evaluate())
    }

    protected fun assertPropertyDefined(key: String) {
        assertTrue("Precondition: '$key' must be loaded, got ${definedKeys()}", PropertyUtil.toCommonPropertyForm(key) in definedKeys())
    }

    protected fun assertPropertyUndefined(key: String) {
        assertFalse("Precondition: '$key' must be absent, got ${definedKeys()}", PropertyUtil.toCommonPropertyForm(key) in definedKeys())
    }

    private fun definedKeys(): Set<String> {
        ModificationTrackerManager.getInstance(project).invalidateAll()
        return DefinedConfigurationPropertiesSearch.getInstance(project).getPropertiesCommonKeyMap(module).keys
    }

    private fun projectBeanKeys(beans: Collection<PsiBean>): Set<String> =
        beans.mapNotNullTo(sortedSetOf()) { bean ->
            val member = bean.psiMember
            val key = if (member is PsiMethod) {
                "${member.containingClass?.qualifiedName}#${member.name}"
            } else {
                bean.psiClass.qualifiedName
            }
            key?.takeIf { it.startsWith("com.app.") }
        }

    companion object {
        const val APPLICATION = "com.app.Application"
        const val CONDITIONAL_ON_PROPERTY = "org.springframework.boot.autoconfigure.condition.ConditionalOnProperty"
        const val CONDITIONAL_ON_BEAN = "org.springframework.boot.autoconfigure.condition.ConditionalOnBean"
        const val CONDITIONAL = "org.springframework.context.annotation.Conditional"
    }
}
