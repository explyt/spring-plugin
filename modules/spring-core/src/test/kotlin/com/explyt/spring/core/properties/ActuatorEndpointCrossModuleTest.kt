/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties

import com.explyt.spring.core.inspections.SpringPropertiesInspection
import com.explyt.spring.core.inspections.SpringYamlInspection
import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.addFromMaven
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PsiTestUtil
import org.intellij.lang.annotations.Language

/**
 * Regression for issue #382.
 *
 * The standard layout of a shared Actuator endpoint is a library module (a company starter)
 * depended on by the application module owning `application.yaml`. Endpoint discovery searched
 * only the configuration module's own content, so `management.endpoint.<id>.*` of such an
 * endpoint was reported as `Cannot resolve key property`.
 *
 * This needs a multi-module fixture: with a single module the endpoint is always in scope and the
 * bug is invisible.
 */
class ActuatorEndpointCrossModuleTest : ExplytMultiModuleTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootActuatorAutoConfigure_4_1_0)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringPropertiesInspection::class.java, SpringYamlInspection::class.java)
    }

    fun testDependencyModuleEndpointKeysAreNotReported() {
        val library = addDependencyModule("library")
        addFileToModule(library, "com/example/library/PublishersEndpoint.kt", KOTLIN_ENDPOINT)

        myFixture.configureByText(
            "application.yaml",
            """
            management:
              endpoint:
                publishers:
                  access: unrestricted
                  enabled: true
                  cache:
                    time-to-live: 10s
            """.trimIndent()
        )

        assertEquals("expected no unresolved key, got: ${unresolvedKeys()}", emptyList<String>(), unresolvedKeys())
    }

    fun testDependencyModuleJavaEndpointKeysAreNotReported() {
        val library = addDependencyModule("library")
        addFileToModule(library, "com/example/library/PublishersEndpoint.java", JAVA_ENDPOINT)

        myFixture.configureByText("application.properties", "management.endpoint.publishers.access=unrestricted")

        assertEquals("expected no unresolved key, got: ${unresolvedKeys()}", emptyList<String>(), unresolvedKeys())
    }

    fun testIdSegmentNavigatesToDependencyModuleEndpointClass() {
        val library = addDependencyModule("library")
        addFileToModule(library, "com/example/library/PublishersEndpoint.kt", KOTLIN_ENDPOINT)
        myFixture.configureByText("application.properties", "management.endpoint.publishers.access=unrestricted")

        val targets = targetsAt("publishers")

        assertEquals("the id names one endpoint, got: ${describe(targets)}", 1, targets.size)
        assertEquals("PublishersEndpoint", (targets.single() as? PsiClass)?.name)
    }

    /**
     * Guards the guard: an endpoint in a module the configuration module does NOT depend on stays
     * undiscovered, proving the tests above exercise the dependency edge rather than a project-wide
     * search.
     */
    fun testEndpointInUnrelatedModuleIsNotDiscovered() {
        val unrelated = addUnrelatedModule("unrelated")
        addFileToModule(unrelated, "com/example/unrelated/PublishersEndpoint.kt", KOTLIN_ENDPOINT)

        myFixture.configureByText("application.properties", "management.endpoint.publishers.access=unrestricted")

        assertEquals("an endpoint outside the dependency graph must not resolve", 1, unresolvedKeys().size)
    }

    fun testUnknownTailOfDependencyModuleEndpointIsStillReported() {
        val library = addDependencyModule("library")
        addFileToModule(library, "com/example/library/PublishersEndpoint.kt", KOTLIN_ENDPOINT)

        myFixture.configureByText("application.properties", "management.endpoint.publishers.unknown=1")

        assertEquals("a tail Spring does not resolve stays reported", 1, unresolvedKeys().size)
    }

    private fun addUnrelatedModule(name: String): Module {
        val sourceRoot = myFixture.tempDirFixture.findOrCreateDir("$name/src")
        val unrelated = PsiTestUtil.addModule(project, JavaModuleType.getModuleType(), name, sourceRoot)
        ModuleRootModificationUtil.setSdkInherited(unrelated)
        ModuleRootModificationUtil.updateModel(unrelated) { model ->
            libraries.forEach { addFromMaven(model, it.mavenCoordinates, it.includeTransitiveDependencies) }
        }
        IndexingTestUtil.waitUntilIndexesAreReady(project)
        return unrelated
    }

    private fun unresolvedKeys(): List<String> = myFixture.doHighlighting().asSequence()
        .mapNotNull { it.description }
        .filter { it.contains("Cannot resolve key property") }
        .toList()

    private fun targetsAt(segment: String): List<PsiElement> {
        val reference = myFixture.file.findReferenceAt(
            myFixture.file.text.indexOf(segment) + segment.length - 1
        ) ?: return emptyList()
        return when (reference) {
            is PsiPolyVariantReference -> reference.multiResolve(false).mapNotNull { it.element }
            else -> listOfNotNull(reference.resolve())
        }
    }

    private fun describe(targets: List<PsiElement>) =
        targets.map { "${it::class.java.simpleName}[${it.text?.take(40)}]" }

    private companion object {
        @Language("kotlin")
        val KOTLIN_ENDPOINT = """
            package com.example.library

            import org.springframework.boot.actuate.endpoint.Access
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
            import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint

            @WebEndpoint(id = "publishers", defaultAccess = Access.NONE)
            internal class PublishersEndpoint {
                @ReadOperation
                fun status() = "ok"
            }
        """.trimIndent()

        @Language("java")
        val JAVA_ENDPOINT = """
            package com.example.library;

            import org.springframework.boot.actuate.endpoint.Access;
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
            import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;

            @WebEndpoint(id = "publishers", defaultAccess = Access.NONE)
            public class PublishersEndpoint {
                @ReadOperation
                public String status() { return "ok"; }
            }
        """.trimIndent()
    }
}
