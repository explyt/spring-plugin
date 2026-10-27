/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.editor.openapi.OpenApiUtils
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.ExtensionTestUtil
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue

class OpenApiPreviewEditorGutterTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWebMvc_6_0_7)

    fun testPreviewEditorProviderIsRegisteredWhenJcefIsPresent() {
        assertTrue(
            "spring-web-jcef.xml registers the OpenAPI preview editor provider",
            isPreviewEditorRegistered()
        )
    }

    fun testJavaControllerMethodHasRunInSwaggerGutterWithPreviewEditor() {
        assertTrue(isPreviewEditorRegistered())
        assertTrue(EndpointRunLineMarkerProvider().getInfo(javaControllerMethodName()).hasRunInSwagger())
    }

    fun testKotlinControllerMethodHasRunInSwaggerGutterWithPreviewEditor() {
        assertTrue(isPreviewEditorRegistered())
        assertTrue(EndpointRunLineMarkerProvider().getInfo(kotlinControllerMethodName()).hasRunInSwagger())
    }

    fun testJaxRsResourceMethodHasRunInSwaggerGutterWithPreviewEditor() {
        assertTrue(isPreviewEditorRegistered())
        assertTrue(JaxRsRunLineMarkerProvider().getInfo(jaxRsResourceMethodName()).hasRunInSwagger())
    }

    fun testYamlOperationHasOpenInUiGutterWithPreviewEditor() {
        assertTrue(isPreviewEditorRegistered())
        assertTrue(YamlRunEndpointLineMarkerProvider().getInfo(yamlOperationKey()).hasOpenInUi())
    }

    fun testJsonOperationHasOpenInUiGutterWithPreviewEditor() {
        assertTrue(isPreviewEditorRegistered())
        assertTrue(JsonRunEndpointLineMarkerProvider().getInfo(jsonOperationKey()).hasOpenInUi())
    }

    fun testJavaControllerMethodHasNoRunInSwaggerGutterWithoutPreviewEditor() {
        val methodName = javaControllerMethodName()
        assertTrue(
            "Precondition: the gutter is shown while the preview editor is registered",
            EndpointRunLineMarkerProvider().getInfo(methodName).hasRunInSwagger()
        )
        maskPreviewEditorProvider()

        assertFalse(
            "Run in Swagger must be hidden without the OpenAPI preview editor",
            EndpointRunLineMarkerProvider().getInfo(methodName).hasRunInSwagger()
        )
    }

    fun testKotlinControllerMethodHasNoRunInSwaggerGutterWithoutPreviewEditor() {
        val methodName = kotlinControllerMethodName()
        assertTrue(
            "Precondition: the gutter is shown while the preview editor is registered",
            EndpointRunLineMarkerProvider().getInfo(methodName).hasRunInSwagger()
        )
        maskPreviewEditorProvider()

        assertFalse(
            "Run in Swagger must be hidden without the OpenAPI preview editor",
            EndpointRunLineMarkerProvider().getInfo(methodName).hasRunInSwagger()
        )
    }

    fun testKotlinControllerClassHasNoRunInSwaggerGutterWithoutPreviewEditor() {
        val className = kotlinControllerClassName()
        assertTrue(
            "Precondition: the gutter is shown while the preview editor is registered",
            ControllerRunLineMarkerProvider().getInfo(className).hasRunInSwagger()
        )
        maskPreviewEditorProvider()

        assertFalse(
            "Run in Swagger must be hidden without the OpenAPI preview editor",
            ControllerRunLineMarkerProvider().getInfo(className).hasRunInSwagger()
        )
    }

    fun testJaxRsResourceMethodHasNoRunInSwaggerGutterWithoutPreviewEditor() {
        val methodName = jaxRsResourceMethodName()
        assertTrue(
            "Precondition: the gutter is shown while the preview editor is registered",
            JaxRsRunLineMarkerProvider().getInfo(methodName).hasRunInSwagger()
        )
        maskPreviewEditorProvider()

        assertFalse(
            "Run in Swagger must be hidden without the OpenAPI preview editor",
            JaxRsRunLineMarkerProvider().getInfo(methodName).hasRunInSwagger()
        )
    }

    fun testYamlOperationHasNoOpenInUiGutterWithoutPreviewEditor() {
        val operationKey = yamlOperationKey()
        assertTrue(
            "Precondition: the gutter is shown while the preview editor is registered",
            YamlRunEndpointLineMarkerProvider().getInfo(operationKey).hasOpenInUi()
        )
        maskPreviewEditorProvider()

        assertFalse(
            "Open in UI must be hidden without the OpenAPI preview editor",
            YamlRunEndpointLineMarkerProvider().getInfo(operationKey).hasOpenInUi()
        )
    }

    fun testJsonOperationHasNoOpenInUiGutterWithoutPreviewEditor() {
        val operationKey = jsonOperationKey()
        assertTrue(
            "Precondition: the gutter is shown while the preview editor is registered",
            JsonRunEndpointLineMarkerProvider().getInfo(operationKey).hasOpenInUi()
        )
        maskPreviewEditorProvider()

        assertFalse(
            "Open in UI must be hidden without the OpenAPI preview editor",
            JsonRunEndpointLineMarkerProvider().getInfo(operationKey).hasOpenInUi()
        )
    }

    private fun isPreviewEditorRegistered(): Boolean =
        FileEditorProvider.EP_FILE_EDITOR_PROVIDER.extensionList
            .any { it.editorTypeId == OpenApiUtils.OPENAPI_EDITOR_TYPE_ID }

    private fun maskPreviewEditorProvider() {
        assertTrue("Precondition: the preview editor provider is registered before masking", isPreviewEditorRegistered())
        val withoutPreviewEditor = FileEditorProvider.EP_FILE_EDITOR_PROVIDER.extensionList
            .filter { it.editorTypeId != OpenApiUtils.OPENAPI_EDITOR_TYPE_ID }
        ExtensionTestUtil.maskExtensions(FileEditorProvider.EP_FILE_EDITOR_PROVIDER, withoutPreviewEditor, testRootDisposable)
        assertFalse("Precondition: the preview editor provider is masked", isPreviewEditorRegistered())
        assertTrue("Precondition: other editor providers stay registered", withoutPreviewEditor.isNotEmpty())
    }

    private fun RunLineMarkerContributor.Info?.hasRunInSwagger(): Boolean =
        this?.actions.orEmpty().any { it is RunInSwaggerAction }

    private fun RunLineMarkerContributor.Info?.hasOpenInUi(): Boolean =
        this?.actions.orEmpty().any { it.templateText == "Open in UI" }

    private fun javaControllerMethodName(): PsiElement {
        myFixture.addFileToProject(
            "com/example/PetController.java", """
            package com.example;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class PetController {
                @GetMapping("/pets")
                public String pets() { return "pets"; }
            }
            """.trimIndent()
        )
        return myFixture.findClass("com.example.PetController")
            .findMethodsByName("pets", false).single().nameIdentifier!!
    }

    private fun kotlinControllerMethodName(): PsiElement {
        addKotlinController()
        val method = myFixture.findClass("com.example.OwnerController").findMethodsByName("owners", false).single()
        return (method.navigationElement as KtNamedFunction).nameIdentifier!!
    }

    private fun kotlinControllerClassName(): PsiElement {
        addKotlinController()
        val controller = myFixture.findClass("com.example.OwnerController")
        return (controller.navigationElement as org.jetbrains.kotlin.psi.KtClass).nameIdentifier!!
    }

    private fun addKotlinController() {
        myFixture.addFileToProject(
            "com/example/OwnerController.kt", """
            package com.example

            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RestController

            @RestController
            class OwnerController {
                @GetMapping("/owners")
                fun owners(): String = "owners"
            }
            """.trimIndent()
        )
    }

    private fun jaxRsResourceMethodName(): PsiElement {
        jaxRsAnnotation(
            "HttpMethod",
            "@Target(ElementType.ANNOTATION_TYPE) @Retention(RetentionPolicy.RUNTIME) public @interface HttpMethod { String value(); }"
        )
        jaxRsAnnotation(
            "Path",
            "@Target({ElementType.TYPE, ElementType.METHOD}) @Retention(RetentionPolicy.RUNTIME) public @interface Path { String value(); }"
        )
        jaxRsAnnotation(
            "GET",
            "@Target(ElementType.METHOD) @Retention(RetentionPolicy.RUNTIME) @HttpMethod(\"GET\") public @interface GET {}"
        )
        myFixture.addFileToProject(
            "com/example/VetResource.java", """
            package com.example;

            import jakarta.ws.rs.GET;
            import jakarta.ws.rs.Path;

            @Path("/api")
            public class VetResource {
                @GET
                @Path("/vets")
                public String vets() { return ""; }
            }
            """.trimIndent()
        )
        return myFixture.findClass("com.example.VetResource")
            .findMethodsByName("vets", false).single().nameIdentifier!!
    }

    private fun jaxRsAnnotation(name: String, declaration: String) {
        myFixture.addFileToProject(
            "jakarta/ws/rs/$name.java",
            "package jakarta.ws.rs;\n\nimport java.lang.annotation.*;\n\n$declaration\n"
        )
    }

    private fun yamlOperationKey(): PsiElement {
        val file = myFixture.addFileToProject(
            "openapi.yaml", """
            openapi: "3.0.3"
            info:
              title: "Pets"
              version: "1.0.0"
            paths:
              /pets:
                get:
                  tags:
                    - pets
                  operationId: listPets
            """.trimIndent()
        ) as YAMLFile
        return PsiTreeUtil.findChildrenOfType(file, YAMLKeyValue::class.java)
            .single { it.keyText == "get" }.key!!
    }

    private fun jsonOperationKey(): PsiElement {
        val file = myFixture.addFileToProject(
            "openapi.json", """
            {
              "openapi": "3.0.3",
              "info": { "title": "Pets", "version": "1.0.0" },
              "paths": {
                "/pets": {
                  "get": {
                    "tags": ["pets"],
                    "operationId": "listPets"
                  }
                }
              }
            }
            """.trimIndent()
        ) as JsonFile
        return PsiTreeUtil.findChildrenOfType(file, JsonProperty::class.java)
            .single { it.name == "get" }.nameElement.firstChild
    }
}
