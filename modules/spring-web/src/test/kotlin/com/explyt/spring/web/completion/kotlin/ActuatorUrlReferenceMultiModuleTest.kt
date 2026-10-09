/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.completion.kotlin

import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.TestUtil.findTypedReferenceAt
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.references.ExplytControllerMethodReference
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression

class ActuatorUrlReferenceMultiModuleTest : ExplytMultiModuleTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.kotlin_1_9_22,
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springTest_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springBootAutoConfigure_4_1_0,
        TestLibrary.springBootActuatorAutoConfigure_4_1_0
    )

    fun testDependentModuleWithoutApplicationKeepsActuatorUrlUnresolved() {
        val testModule = addDependentModule("app-test")
        val request = addTestSourceFileToModule(
            testModule,
            "com/example/apptest/CustomEndpointTest.java",
            "package com.example.apptest;\n" +
                "import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;\n" +
                "class CustomEndpointTest { void request() { get(\"/actuator/custom\"); } }"
        )
        myFixture.configureFromExistingVirtualFile(request.virtualFile)
        myFixture.editor.caretModel.moveToOffset(request.text.indexOf("custom") + 2)
        val reference = myFixture.file.findTypedReferenceAt<ExplytControllerMethodReference>(myFixture.caretOffset)
        assertNotNull("precondition: MockMvc URL reference exists", reference)
        assertNull(reference!!.resolve())
    }

    fun testServingApplicationBasePathIsUsedFromDependentModule() {
        addFileToModule(module, "Application.kt", "import org.springframework.boot.autoconfigure.SpringBootApplication\n@SpringBootApplication class Application")
        addFileToModule(module, "CustomEndpoint.kt", "import org.springframework.boot.actuate.endpoint.annotation.Endpoint\nimport org.springframework.boot.actuate.endpoint.annotation.ReadOperation\n@Endpoint(id = \"custom\") class CustomEndpoint { @ReadOperation fun read(): String = \"custom\" }")
        addFileToModule(module, "application.properties", "management.endpoints.web.base-path=/manage")
        val testModule = addDependentModule("app-test")
        val request = addTestSourceFileToModule(testModule, "CustomEndpointTest.java", "import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get; class CustomEndpointTest { void request() { get(\"/manage/custom\"); } }")
        myFixture.configureFromExistingVirtualFile(request.virtualFile)
        myFixture.editor.caretModel.moveToOffset(request.text.indexOf("custom") + 2)
        val reference = myFixture.file.findTypedReferenceAt<ExplytControllerMethodReference>(myFixture.caretOffset)
        assertNotNull("precondition: base-path URL reference exists", reference)
        assertEquals("read", (reference!!.resolve() as? PsiMethod)?.name)
    }

    fun testMockMvcFromTestModuleResolvesApplicationEndpoint() {
        addFileToModule(
            module,
            "Application.kt",
            "import org.springframework.boot.autoconfigure.SpringBootApplication\n@SpringBootApplication class Application"
        )
        addFileToModule(
            module,
            "CustomEndpoint.kt",
            "import org.springframework.boot.actuate.endpoint.annotation.Endpoint\n" +
                "import org.springframework.boot.actuate.endpoint.annotation.ReadOperation\n" +
                "@Endpoint(id = \"custom\") class CustomEndpoint {\n" +
                " @ReadOperation fun read(): String = \"custom\"\n}"
        )
        val appEndpoints = SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)
            .filter { it.type == EndpointType.ACTUATOR && it.path == "/actuator/custom" }
        assertEquals(1, appEndpoints.size)
        assertEquals("read", (appEndpoints.single().psiElement as PsiMethod).name)

        val testModule = addDependentModule("app-test")
        val request = addTestSourceFileToModule(
            testModule,
            "com/example/apptest/CustomEndpointTest.java",
            "package com.example.apptest;\n\n" +
                "import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;\n\n" +
                "class CustomEndpointTest {\n" +
                "    void request() { get(\"/actuator/custom\"); }\n" +
                "}"
        )
        myFixture.configureFromExistingVirtualFile(request.virtualFile)
        myFixture.editor.caretModel.moveToOffset(request.text.indexOf("custom") + 2)
        val getCall = generateSequence(myFixture.file.findElementAt(myFixture.caretOffset)) { it.parent }
            .filterIsInstance<PsiMethodCallExpression>()
            .firstOrNull()
        assertNotNull("precondition: MockMvcRequestBuilders.get call is present", getCall)
        val resolvedGet = getCall!!.resolveMethod()
        assertEquals(
            "precondition: get resolves to MockMvcRequestBuilders.get",
            "org.springframework.test.web.servlet.request.MockMvcRequestBuilders",
            resolvedGet?.containingClass?.qualifiedName
        )

        val reference = myFixture.file.findTypedReferenceAt<ExplytControllerMethodReference>(myFixture.caretOffset)
        assertNotNull("precondition: MockMvc URL has an Actuator endpoint reference", reference)
        assertEquals("read", (reference!!.resolve() as? PsiMethod)?.name)
        assertEquals(
            1,
            SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints()
                .count { it.type == EndpointType.ACTUATOR && it.path == "/actuator/custom" }
        )
    }
}
