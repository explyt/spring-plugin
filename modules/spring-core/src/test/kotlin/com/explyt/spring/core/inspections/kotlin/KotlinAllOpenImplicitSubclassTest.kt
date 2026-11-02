/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInspection.inheritance.ImplicitSubclassInspection
import com.intellij.facet.FacetManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.application.PathManager
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCommonCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.facet.KotlinFacetType
import org.jetbrains.kotlin.idea.facet.getInstance

class KotlinAllOpenImplicitSubclassTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springTx_6_0_7
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(ImplicitSubclassInspection())
    }

    override fun tearDown() {
        try {
            removeKotlinFacet()
        } catch (e: Throwable) {
            addSuppressedException(e)
        } finally {
            super.tearDown()
        }
    }

    fun testSpringPresetOpensTransactionalService() {
        applyAllOpen("preset=spring")

        assertNoImplicitSubclassProblems(TRANSACTIONAL_SERVICE)
    }

    fun testSpringPresetWithoutCompilerPluginJarOpensTransactionalService() {
        applyAllOpen("preset=spring", withPluginJar = false)

        assertNoImplicitSubclassProblems(TRANSACTIONAL_SERVICE)
    }

    fun testExplicitAnnotationOpensTransactionalService() {
        applyAllOpen("annotation=org.springframework.stereotype.Component")

        assertNoImplicitSubclassProblems(TRANSACTIONAL_SERVICE)
    }

    fun testAnnotatedSupertypeOpensSubclass() {
        applyAllOpen("annotation=org.springframework.stereotype.Component")

        assertNoImplicitSubclassProblems(
            """
            package com.example

            import org.springframework.stereotype.Component
            import org.springframework.transaction.annotation.Transactional

            @Component
            abstract class BaseService

            class VoucherService : BaseService() {
                @Transactional
                fun redeem() {}
            }
            """.trimIndent()
        )
    }

    fun testJpaOnlyAllOpenStillReportsTransactionalService() {
        applyAllOpen("annotation=jakarta.persistence.Entity")

        assertImplicitSubclassProblems(TRANSACTIONAL_SERVICE)
    }

    fun testWithoutAllOpenReportsTransactionalService() {
        assertImplicitSubclassProblems(TRANSACTIONAL_SERVICE)
    }

    fun testExplicitlyFinalClassIsStillReported() {
        applyAllOpen("preset=spring")

        assertImplicitSubclassProblems(
            """
            package com.example

            import org.springframework.stereotype.Service
            import org.springframework.transaction.annotation.Transactional

            @Service
            final class VoucherService {
                @Transactional
                fun redeem() {}
            }
            """.trimIndent()
        )
    }

    fun testSpringPresetKeepsReportingExplicitlyFinalTransactionalMethod() {
        applyAllOpen("preset=spring")
        myFixture.configureByText("VoucherService.kt", FINAL_TRANSACTIONAL_METHOD_SERVICE)

        assertContainsElements(implicitSubclassProblems(), "Methods annotated with '@Transactional' must be overridable")
    }

    fun testSpringPresetWithoutCompilerPluginJarKeepsReportingExplicitlyFinalTransactionalMethod() {
        applyAllOpen("preset=spring", withPluginJar = false)
        myFixture.configureByText("VoucherService.kt", FINAL_TRANSACTIONAL_METHOD_SERVICE)

        assertContainsElements(implicitSubclassProblems(), "Methods annotated with '@Transactional' must be overridable")
    }

    fun testMethodMessageNamesTheAnnotation() {
        myFixture.configureByText("VoucherService.kt", TRANSACTIONAL_SERVICE)

        val messages = implicitSubclassProblems()

        assertContainsElements(messages, "Methods annotated with '@Transactional' must be overridable")
    }

    private fun applyAllOpen(option: String, withPluginJar: Boolean = true) {
        val module = myFixture.module
        val facetManager = FacetManager.getInstance(module)
        val facet = facetManager.createFacet(KotlinFacetType.INSTANCE, KotlinFacetType.NAME, null)
        facet.configuration.settings.apply {
            useProjectSettings = false
            compilerArguments = K2JVMCompilerArguments().apply {
                pluginOptions = arrayOf("plugin:org.jetbrains.kotlin.allopen:$option")
                if (withPluginJar) pluginClasspaths = arrayOf(allOpenCompilerPluginJar())
            }
        }
        runWriteAction {
            facetManager.createModifiableModel().apply {
                addFacet(facet)
                commit()
            }
        }
        assertContainsElements(
            KotlinCommonCompilerArgumentsHolder.getInstance(module).pluginOptions.orEmpty().toList(),
            "plugin:org.jetbrains.kotlin.allopen:$option"
        )
    }

    private fun removeKotlinFacet() {
        val facet = KotlinFacet.get(myFixture.module) ?: return
        val facetManager = FacetManager.getInstance(myFixture.module)
        runWriteAction {
            facetManager.createModifiableModel().apply {
                removeFacet(facet)
                commit()
            }
        }
    }

    private fun allOpenCompilerPluginJar(): String =
        PathManager.getJarForClass(Class.forName("org.jetbrains.kotlin.allopen.AllOpenComponentRegistrar"))!!.toString()

    private fun assertNoImplicitSubclassProblems(text: String) {
        myFixture.configureByText("VoucherService.kt", text)
        assertEmpty(implicitSubclassProblems())
    }

    private fun assertImplicitSubclassProblems(text: String) {
        myFixture.configureByText("VoucherService.kt", text)
        assertNotEmpty(implicitSubclassProblems())
    }

    private fun implicitSubclassProblems(): List<String> =
        myFixture.doHighlighting()
            .filter { it.inspectionToolId == IMPLICIT_SUBCLASS_ID }
            .mapNotNull { it.description }

    companion object {
        private const val IMPLICIT_SUBCLASS_ID = "ImplicitSubclassInspection"

        private val FINAL_TRANSACTIONAL_METHOD_SERVICE = """
            package com.example

            import org.springframework.stereotype.Service
            import org.springframework.transaction.annotation.Transactional

            @Service
            class VoucherService {
                @Transactional
                final fun redeem() {}
            }
        """.trimIndent()

        private val TRANSACTIONAL_SERVICE = """
            package com.example

            import org.springframework.stereotype.Service
            import org.springframework.transaction.annotation.Transactional

            @Service
            class VoucherService {
                @Transactional
                fun redeem() {}
            }
        """.trimIndent()
    }
}
