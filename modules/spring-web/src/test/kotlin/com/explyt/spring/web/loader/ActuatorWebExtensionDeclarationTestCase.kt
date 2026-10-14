/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader

import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.test.ExplytBaseLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope

abstract class ActuatorWebExtensionDeclarationTestCase : ExplytBaseLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootActuatorAutoConfigure_4_1_0,
        TestLibrary.springBootHealth_4_1_0,
    )

    protected open val conventionOverridesMetaValues: Boolean = false

    protected abstract fun addApplicationWithEndpointsXAndY()
    protected abstract fun addDirectExtension()
    protected abstract fun addComposedExtensionWithFixedEndpoint()
    protected abstract fun addAliasExtensionUsingTheAliasDefault()
    protected abstract fun addAliasExtensionSettingTheAliasAtUseSite()
    protected abstract fun addRootAliasExtensionUsingTheAliasDefault()
    protected abstract fun addUnaliasedSameNamedAttributeExtension()
    protected abstract fun addExtensionInheritingADirectAnnotation()
    protected abstract fun addExtensionInheritingAComposedAnnotation()

    override fun setUp() {
        super.setUp()
        addApplicationWithEndpointsXAndY()
        assertNotNull(
            "precondition: $ENDPOINT_WEB_EXTENSION is on the classpath",
            JavaPsiFacade.getInstance(project).findClass(ENDPOINT_WEB_EXTENSION, GlobalSearchScope.allScope(project))
        )
        assertEquals("precondition: x serves its own read", listOf(X_OWN_READ), operationsAt(X_PATH))
        assertEquals("precondition: y serves its own read", listOf(Y_OWN_READ), operationsAt(Y_PATH))
    }

    fun testDirectAnnotationExtendsItsEndpoint() {
        addDirectExtension()

        assertExtendsX("DirectExtension", annotatedBy = "DirectExtension")
    }

    fun testComposedAnnotationWithAFixedEndpointExtendsIt() {
        addComposedExtensionWithFixedEndpoint()

        assertExtendsX("ComposedExtension", annotatedBy = "FixedExt")
    }

    fun testAliasDefaultOverridesTheMetaEndpoint() {
        addAliasExtensionUsingTheAliasDefault()

        assertExtendsX("AliasDefaultExtension", annotatedBy = "AliasExt")
    }

    fun testAliasSetAtUseSiteOverridesTheAliasDefaultAndTheMetaEndpoint() {
        addAliasExtensionSettingTheAliasAtUseSite()

        assertExtendsX("AliasUseSiteExtension", annotatedBy = "AliasExtDefaultY")
    }

    fun testAliasToTheRootExtensionAttributeOverridesTheMetaEndpoint() {
        addRootAliasExtensionUsingTheAliasDefault()

        assertExtendsX("RootAliasExtension", annotatedBy = "RootAliasExt")
    }

    fun testUnaliasedSameNamedAttributeFollowsTheConventionOfTheSpringMajor() {
        assertSpringCoreMajorVersion(if (conventionOverridesMetaValues) 6 else 7)
        addUnaliasedSameNamedAttributeExtension()

        if (conventionOverridesMetaValues) {
            assertExtendsX("ConventionExtension", annotatedBy = "ConventionExt")
        } else {
            assertCarriesWebExtension("ConventionExt")
            assertEquals("x keeps its own read", listOf(X_OWN_READ), operationsAt(X_PATH))
            assertEquals("ConventionExtension extends y", listOf(readOf("ConventionExtension")), operationsAt(Y_PATH))
        }
    }

    fun testDirectAnnotationOnTheSuperclassExtendsItsEndpoint() {
        addExtensionInheritingADirectAnnotation()

        assertExtendsX("InheritedDirectExtension", annotatedBy = "AbstractDirectExtension")
    }

    fun testComposedAnnotationOnTheSuperclassExtendsItsEndpoint() {
        addExtensionInheritingAComposedAnnotation()

        assertExtendsX("InheritedComposedExtension", annotatedBy = "FixedExt")
    }

    private fun assertExtendsX(extension: String, annotatedBy: String) {
        assertCarriesWebExtension(annotatedBy)
        assertEquals("$extension extends x", listOf(readOf(extension)), operationsAt(X_PATH))
        assertEquals("y keeps its own read", listOf(Y_OWN_READ), operationsAt(Y_PATH))
    }

    private fun assertCarriesWebExtension(simpleName: String) {
        val carrier = JavaPsiFacade.getInstance(project)
            .findClass("$PACKAGE.$simpleName", GlobalSearchScope.projectScope(project))
        assertNotNull("precondition: $simpleName is in the project", carrier)
        assertNotNull(
            "precondition: $simpleName carries a resolved @EndpointWebExtension",
            carrier!!.getAnnotation(ENDPOINT_WEB_EXTENSION)
        )
    }

    private fun assertSpringCoreMajorVersion(major: Int) {
        val springVersion = JavaPsiFacade.getInstance(project)
            .findClass(SPRING_VERSION, module.getModuleWithDependenciesAndLibrariesScope(false))
        assertNotNull("precondition: spring-core is on the module classpath", springVersion)
        val jarName = springVersion!!.containingFile.virtualFile.path.substringBefore("!/").substringAfterLast('/')
        assertTrue("precondition: spring-core jar $jarName is version $major.x", jarName.startsWith("spring-core-$major."))
    }

    private fun operationsAt(path: String): List<Triple<List<String>, String?, String?>> {
        ModificationTrackerManager.getInstance(project).invalidateAll()
        return SpringWebEndpointsLoader.EP_NAME.getExtensions(project).asSequence()
            .filter { it.getType() == EndpointType.ACTUATOR }
            .filter { it.isApplicable(module) }
            .flatMap { it.searchEndpoints(module) }
            .filter { it.path == path }
            .map {
                val method = it.psiElement as? PsiMethod
                Triple(it.requestMethods, method?.containingClass?.qualifiedName, method?.name)
            }
            .toList()
    }

    protected companion object {
        const val PACKAGE = "com.app"
        const val X_PATH = "/actuator/x"
        const val Y_PATH = "/actuator/y"
        const val ENDPOINT_WEB_EXTENSION = "org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension"
        const val SPRING_VERSION = "org.springframework.core.SpringVersion"

        private fun readOf(simpleName: String) = Triple(listOf("GET"), "$PACKAGE.$simpleName", "read")

        val X_OWN_READ = readOf("XEndpoint")
        val Y_OWN_READ = readOf("YEndpoint")
    }
}
