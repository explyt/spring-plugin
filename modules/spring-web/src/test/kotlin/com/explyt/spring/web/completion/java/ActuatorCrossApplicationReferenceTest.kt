/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.completion.java

import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.TestUtil.findTypedReferenceAt
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.references.ExplytControllerMethodReference
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModuleOrderEntry

import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression

class ActuatorCrossApplicationReferenceTest : ExplytMultiModuleTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springTest_6_0_7,
        TestLibrary.springBootAutoConfigure_4_1_0,
        TestLibrary.springBootActuatorAutoConfigure_4_1_0,
        TestLibrary.springBootHealth_4_1_0
    )

    fun testDifferentApplicationsKeepConcreteAndCatchAllTargetsAmbiguous() {
        addApplication(module, "app.a")
        addEndpoint(module, "app.a", "CatchAllStatus", "catchAll", "@Selector(match = Selector.Match.ALL_REMAINING) String path")
        val appB = addDependencyModule("app-b")
        ModuleRootModificationUtil.updateModel(module) { model ->
            model.orderEntries.filterIsInstance<ModuleOrderEntry>()
                .filter { it.module == appB }
                .forEach(model::removeOrderEntry)
        }
        addApplication(appB, "app.b")
        addEndpoint(appB, "app.b", "ConcreteStatus", "concrete", "")
        addFileToModule(module, "application.properties", "management.endpoints.web.exposure.include=status\n")
        addFileToModule(appB, "application.properties", "management.endpoints.web.exposure.include=status\n")
        val testModule = addDependentModule("app-test")
        ModuleRootModificationUtil.addDependency(testModule, appB, DependencyScope.COMPILE, false)
        val request = addTestSourceFileToModule(
            testModule,
            "app/test/StatusTest.java",
            "package app.test;\n\n" +
                "import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;\n\n" +
                "class StatusTest {\n    void request() { get(\"/actuator/status\"); }\n}"
        )
        myFixture.configureFromExistingVirtualFile(request.virtualFile)
        myFixture.editor.caretModel.moveToOffset(request.text.indexOf("status") + 2)
        val getCall = generateSequence(myFixture.file.findElementAt(myFixture.caretOffset)) { it.parent }
            .filterIsInstance<PsiMethodCallExpression>()
            .firstOrNull()
        assertEquals("precondition: MockMvc get resolves", "org.springframework.test.web.servlet.request.MockMvcRequestBuilders", getCall?.resolveMethod()?.containingClass?.qualifiedName)
        val reference = myFixture.file.findTypedReferenceAt<ExplytControllerMethodReference>(myFixture.caretOffset)
        assertNotNull("precondition: URL reference type", reference)

        val aEndpoints = endpoints(module)
        val bEndpoints = endpoints(appB)
        val aOperation = aEndpoints.single { (it.psiElement as? PsiMethod)?.name == "catchAll" }
        val bOperation = bEndpoints.single { (it.psiElement as? PsiMethod)?.name == "concrete" }
        assertEquals("precondition: app A catch-all path", "/actuator/status/{*path}", aOperation.path)
        assertEquals("precondition: app B concrete path", "/actuator/status", bOperation.path)
        assertNotNull("precondition: application identities differ", aOperation.application)
        assertNotNull("precondition: application identities differ", bOperation.application)
        assertFalse(aOperation.application!!.isEquivalentTo(bOperation.application))

        val targets = reference!!.multiResolve(false).mapNotNull { it.element as? PsiMethod }
        assertEquals(setOf("catchAll", "concrete"), targets.map { it.name }.toSet())
        assertNull(reference.resolve())
    }

    fun testConcreteOperationWinsInsideOneApplication() {
        addApplication(module, "app.single")
        addEndpoint(module, "app.single", "Status", "catchAll", "@Selector(match = Selector.Match.ALL_REMAINING) String path")
        addEndpoint(module, "app.single", "Status", "concrete", "")
        addFileToModule(module, "application.properties", "management.endpoints.web.exposure.include=status\n")
        val request = addTestSourceFileToModule(module, "StatusTest.java", "import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;\nclass StatusTest {\n    void request() { get(\"/actuator/status\"); }\n}")
        myFixture.configureFromExistingVirtualFile(request.virtualFile)
        myFixture.editor.caretModel.moveToOffset(request.text.indexOf("status") + 2)
        val reference = myFixture.file.findTypedReferenceAt<ExplytControllerMethodReference>(myFixture.caretOffset)
        assertNotNull(reference)
        assertEquals("concrete", (reference!!.resolve() as? PsiMethod)?.name)
    }

    private fun endpoints(module: com.intellij.openapi.module.Module) =
        SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module, listOf(EndpointType.ACTUATOR))
            .filter { it.path.startsWith("/actuator/status") }

    private fun addApplication(target: com.intellij.openapi.module.Module, packageName: String) {
        addFileToModule(target, "${packageName.replace('.', '/')}/Application.java", "package $packageName; import org.springframework.boot.autoconfigure.SpringBootApplication; @SpringBootApplication public class Application {}")
    }

    private fun addEndpoint(target: com.intellij.openapi.module.Module, packageName: String, className: String, methodName: String, parameter: String) {
        addFileToModule(target, "${packageName.replace('.', '/')}/$className.java", "package $packageName; import org.springframework.boot.actuate.endpoint.annotation.*; @Endpoint(id=\"status\") public class $className { @ReadOperation public String $methodName($parameter) { return \"status\"; } }")
    }
}
