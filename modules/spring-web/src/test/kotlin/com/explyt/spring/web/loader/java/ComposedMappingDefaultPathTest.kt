/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.java

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope

class ComposedMappingDefaultPathTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWebMvc_6_0_7)

    fun testAliasDefaultPathOfComposedMappingIsServedOnPlainUse() {
        addController("@ReportMapping")

        val endpoint = reportEndpoint()

        assertEquals("/reports", endpoint.path)
        assertEquals(listOf("GET"), endpoint.requestMethods)
    }

    fun testPathSetAtUseSiteOverridesTheAliasDefault() {
        addController("@ReportMapping(path = \"/custom\")")

        val endpoint = reportEndpoint()

        assertEquals("/custom", endpoint.path)
        assertEquals(listOf("GET"), endpoint.requestMethods)
    }

    private fun addController(usage: String) {
        myFixture.addFileToProject(
            "com/example/ReportController.java", """
            package com.example;

            import org.springframework.core.annotation.AliasFor;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RequestMethod;
            import org.springframework.web.bind.annotation.RestController;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Target(ElementType.METHOD)
            @Retention(RetentionPolicy.RUNTIME)
            @RequestMapping(method = RequestMethod.GET)
            @interface ReportMapping {
                @AliasFor(annotation = RequestMapping.class, attribute = "path")
                String[] path() default "/reports";
            }

            @RestController
            public class ReportController {
                $usage
                public String report() {
                    return "report";
                }
            }
            """.trimIndent()
        )
    }

    private fun reportEndpoint(): EndpointElement {
        val mapping = JavaPsiFacade.getInstance(project)
            .findClass("com.example.ReportMapping", GlobalSearchScope.projectScope(project))
        assertNotNull("Precondition: the composed mapping is in the fixture", mapping)
        assertEquals(
            "Precondition: the composed mapping is meta-annotated with @RequestMapping",
            SpringWebClasses.REQUEST_MAPPING,
            mapping!!.getAnnotation(SpringWebClasses.REQUEST_MAPPING)?.resolveAnnotationType()?.qualifiedName
        )
        return SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)
            .filter { it.type == EndpointType.SPRING_MVC && it.containingClass?.name == "ReportController" }
            .single { (it.psiElement as PsiMethod).name == "report" }
    }
}
