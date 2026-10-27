/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.JavaPsiFacade

/** A reactive application serves under `spring.webflux.base-path`, and the servlet keys mean nothing to it. */
class ApplicationBasePathReactiveTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springReactiveWeb_3_1_1)

    fun testWebFluxBasePathIsTheOnlyPrefix() {
        val facade = JavaPsiFacade.getInstance(project)
        val scope = module.moduleWithLibrariesScope
        assertNull(
            "The fixture must be a reactive application",
            facade.findClass("org.springframework.web.servlet.DispatcherServlet", scope)
        )
        assertNotNull(facade.findClass("org.springframework.web.reactive.DispatcherHandler", scope))
        myFixture.addFileToProject(
            "application.yml",
            "spring:\n  webflux:\n    base-path: /reactive\nserver:\n  servlet:\n    context-path: /ignored\n"
        )

        assertEquals("/reactive", ApplicationBasePath.of(module))
    }
}
