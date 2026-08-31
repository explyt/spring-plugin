/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.service

import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.module.ModuleUtilCore

class SpringWebStaticBeansTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springTest_6_0_7,
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springBootTestAutoConfigure_3_1_1
    )

    fun testWebStaticBeansAreDiscovered() {
        myFixture.addClass("package com.app; public class Application { }")
        val virtualFile = myFixture.findClass("com.app.Application").containingFile.virtualFile
        val module = ModuleUtilCore.findModuleForFile(virtualFile, project)!!

        val discovered = SpringSearchService.getInstance(project).getStaticBeans(module)
            .mapNotNullTo(mutableSetOf()) { it.psiClass.qualifiedName }

        // SpringWebAdditionalBeansDiscoverer is registered under the `com.explyt.spring.core`
        // extension namespace that declares the EP; registering it under `com.explyt.spring.web`
        // silently contributes nothing.
        assertTrue(
            "web static beans missing, discovered: $discovered",
            discovered.contains("org.springframework.web.context.WebApplicationContext")
        )

        // Contributed by test auto-configuration (@AutoConfigureMockMvc / @WebMvcTest), which is not
        // part of the regular auto-configuration model.
        assertTrue(
            "MockMvc missing, discovered: $discovered",
            discovered.contains("org.springframework.test.web.servlet.MockMvc")
        )
        assertTrue(
            "WebTestClient missing, discovered: $discovered",
            discovered.contains("org.springframework.test.web.reactive.server.WebTestClient")
        )
    }
}
