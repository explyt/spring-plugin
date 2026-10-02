/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.kotlin

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.properties.references.ActuatorEndpointKeys
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.loader.SpringWebEndpointsLoader
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope

/**
 * The actuator jar alone declares `InfoEndpoint` and its siblings, but registers none of them: Boot creates them from
 * the Actuator auto-configuration. A classpath without it, such as a library module depending on the actuator API,
 * serves no built-in endpoint.
 */
class ActuatorBuiltInEndpointsWithoutAutoConfigurationTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary("org.springframework.boot:spring-boot-actuator:4.1.0"))

    fun testBuiltInEndpointsAreNotListedWithoutTheAutoConfiguration() {
        val scope = GlobalSearchScope.allScope(project)
        val facade = JavaPsiFacade.getInstance(project)
        assertNotNull(
            "precondition: InfoEndpoint is on the classpath",
            facade.findClass("org.springframework.boot.actuate.info.InfoEndpoint", scope)
        )
        assertNull(
            "precondition: the Actuator auto-configuration is not",
            facade.findClass(SpringCoreClasses.ACTUATOR_ENDPOINT_AUTO_CONFIGURATION, scope)
        )

        assertEquals(emptyMap<String, Any>(), ActuatorEndpointKeys.libraryEndpointsById(module))
        val paths = SpringWebEndpointsLoader.EP_NAME.getExtensions(project).asSequence()
            .filter { it.getType() == EndpointType.ACTUATOR }
            .filter { it.isApplicable(module) }
            .flatMap { it.searchEndpoints(module) }
            .map { it.path }
            .toList()
        assertEquals(emptyList<String>(), paths)
    }
}
