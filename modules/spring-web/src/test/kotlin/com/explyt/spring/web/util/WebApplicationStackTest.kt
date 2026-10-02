/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.JavaPsiFacade

/**
 * Which stack a module's application runs on, decided the way Spring Boot's `WebApplicationType.deduce()` does: one
 * class per classpath, because a light fixture fixes its libraries per test class.
 */
abstract class WebApplicationStackTestCase : ExplytJavaLightTestCase() {

    protected fun assertDispatchers(servlet: Boolean, reactive: Boolean) {
        val facade = JavaPsiFacade.getInstance(project)
        val scope = module.moduleWithLibrariesScope
        assertEquals("DispatcherServlet on the classpath", servlet, facade.findClass(DISPATCHER_SERVLET, scope) != null)
        assertEquals("DispatcherHandler on the classpath", reactive, facade.findClass(DISPATCHER_HANDLER, scope) != null)
    }

    private companion object {
        const val DISPATCHER_SERVLET = "org.springframework.web.servlet.DispatcherServlet"
        const val DISPATCHER_HANDLER = "org.springframework.web.reactive.DispatcherHandler"
    }
}

class WebApplicationStackServletTest : WebApplicationStackTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWebMvc_6_0_7)

    fun testServletMvcAloneIsAServletApplication() {
        assertDispatchers(servlet = true, reactive = false)

        assertEquals(WebApplicationStack.SERVLET, WebApplicationStack.of(module))
    }
}

class WebApplicationStackReactiveTest : WebApplicationStackTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springReactiveWeb_3_1_1)

    fun testWebFluxAloneIsAReactiveApplication() {
        assertDispatchers(servlet = false, reactive = true)

        assertEquals(WebApplicationStack.REACTIVE, WebApplicationStack.of(module))
    }
}

/** WebFlux next to servlet MVC is usually there for `WebClient` only; Spring Boot still starts a servlet application. */
class WebApplicationStackMixedTest : WebApplicationStackTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary.springWebMvc_6_0_7, TestLibrary.springReactiveWeb_3_1_1)

    fun testServletMvcWinsOverWebFlux() {
        assertDispatchers(servlet = true, reactive = true)

        assertEquals(WebApplicationStack.SERVLET, WebApplicationStack.of(module))
    }
}

class WebApplicationStackNoWebTest : WebApplicationStackTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testNoDispatcherMeansNoWebApplication() {
        assertDispatchers(servlet = false, reactive = false)

        assertNull(WebApplicationStack.of(module))
    }
}
