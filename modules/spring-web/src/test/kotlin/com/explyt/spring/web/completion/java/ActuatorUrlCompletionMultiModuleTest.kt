/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.completion.java

import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.TestUtil.findTypedReferenceAt
import com.explyt.spring.web.references.ExplytControllerMethodReference
import com.intellij.codeInsight.lookup.LookupElement

class ActuatorUrlCompletionMultiModuleTest : ExplytMultiModuleTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springTest_6_0_7,
        TestLibrary.springBootAutoConfigure_4_1_0,
        TestLibrary.springBootActuatorAutoConfigure_4_1_0
    )

    fun testActuatorCompletionFromApplicationModuleIsAvailableInTestModule() {
        addFileToModule(module, "Application.kt", "import org.springframework.boot.autoconfigure.SpringBootApplication\n@SpringBootApplication class Application")
        addFileToModule(module, "CustomEndpoint.kt", "import org.springframework.boot.actuate.endpoint.annotation.Endpoint\nimport org.springframework.boot.actuate.endpoint.annotation.ReadOperation\n@Endpoint(id = \"custom\") class CustomEndpoint { @ReadOperation fun read(): String = \"custom\" }")
        addFileToModule(module, "application.properties", "management.endpoints.web.exposure.include=custom\n")
        val testModule = addDependentModule("app-test")
        val request = addTestSourceFileToModule(
            testModule,
            "com/example/apptest/CustomEndpointTest.java",
            "package com.example.apptest;\nimport static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;\n" +
                "class CustomEndpointTest { void request() { get(\"/actuator/custom\"); } }"
        )
        myFixture.configureFromExistingVirtualFile(request.virtualFile)
        myFixture.editor.caretModel.moveToOffset(request.text.indexOf("custom") + 2)
        val reference = myFixture.file.findTypedReferenceAt<ExplytControllerMethodReference>(myFixture.caretOffset)
        assertNotNull("precondition: URL reference exists", reference)
        val lookups = reference!!.variants.mapNotNull { it as? LookupElement }
        assertTrue("custom Actuator endpoint is offered", lookups.any { it.lookupString == "/actuator/custom" || it.lookupString == "custom" })
    }
}
