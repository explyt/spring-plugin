/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.java

import com.explyt.spring.core.service.FactoryTouchDecoy
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.RootDataHolder
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.service.beans.discoverer.AdditionalBeansDiscoverer
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope

class NonArrayLookupArrayScanTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testNonArrayLookupDoesNotBuildBeansForUnrelatedFactories() {
        val decoy = configureDecoy()
        assertEquals(listOf(psiClass("Bar")), lookUpBar())

        decoy.arm()
        assertEquals(listOf(psiClass("Bar")), lookUpBar())

        assertEquals("A Bar lookup must not read unrelated factory annotations", emptyList<String>(), decoy.touches())
    }

    fun testArrayScanDoesNotBuildBeansForNonArrayFactories() {
        val decoy = configureDecoy()
        decoy.arm()

        val arrayBeans = SpringSearchService.getInstance(project).searchArrayComponentPsiClassesByBeanMethods(module)

        assertEquals(emptyList<String>(), arrayBeans.map { it.name })
        assertEquals("The array scan must not read non-array factory annotations", emptyList<String>(), decoy.touches())
    }

    fun testDecoyRecordsAnnotationReadsOnceArmed() {
        val decoy = configureDecoy()
        decoy.arm()
        decoy.factory.annotations
        assertEquals("annotations", decoy.touches().first().substringBefore(' '))
    }

    private fun configureDecoy(): FactoryTouchDecoy {
        myFixture.addFileToProject(
            "com/explyt/demo/Holder.java",
            """
            package com.explyt.demo;
            import org.springframework.context.annotation.Bean;
            import org.springframework.stereotype.Component;
            class Foo {}
            @Component
            class Bar {}
            public class Holder {
                @Bean
                public Foo foo() { return new Foo(); }
            }
            """.trimIndent()
        )
        val decoy = FactoryTouchDecoy(psiClass("Holder"), "foo")
        registerConfiguration(decoy)
        assertTrue(
            "Precondition: the decoy factory reaches the module factory map",
            SpringSearchServiceFacade.getInstance(project).getComponentBeanFactoryMethods(module).keys.contains(decoy.factory)
        )
        return decoy
    }

    private fun registerConfiguration(decoy: PsiClass) {
        AdditionalBeansDiscoverer.EP_NAME.getPoint(project).registerExtension(object : AdditionalBeansDiscoverer() {
            override fun discoverBeans(module: Module): Collection<PsiBean> = emptyList()
            override fun getExtraComponents(module: Module): Collection<PsiBean> = listOf(PsiBean("holder", decoy))
            override fun additionalFilterBeans(module: Module, bean: PsiBean, rootDataHolder: RootDataHolder) = true
        }, testRootDisposable)
        ModificationTrackerManager.getInstance(project).invalidateAll()
    }

    private fun lookUpBar() = SpringSearchServiceFacade.getInstance(project).findActiveBeanDeclarations(
        module, "bar", JavaLanguage.INSTANCE, JavaPsiFacade.getElementFactory(project).createType(psiClass("Bar"))
    )

    private fun psiClass(name: String) = JavaPsiFacade.getInstance(project)
        .findClass("com.explyt.demo.$name", GlobalSearchScope.projectScope(project)) ?: error("No PSI for $name")
}
