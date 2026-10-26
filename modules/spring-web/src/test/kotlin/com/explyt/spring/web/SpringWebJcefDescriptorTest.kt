/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.nio.charset.StandardCharsets
import javax.xml.parsers.DocumentBuilderFactory

class SpringWebJcefDescriptorTest {

    @Test
    fun testJcefDependencyIsOptionalAndOpenApiProviderIsMovedToConfig() {
        val descriptor = descriptor("META-INF/spring-web-plugin.xml")
        val dependencies = descriptor.getElementsByTagName("depends").asElements()
        val jcef = dependencies.single { it.textContent.trim() == "com.intellij.modules.jcef" }

        assertEquals("true", jcef.getAttribute("optional"))
        assertEquals("spring-web-jcef.xml", jcef.getAttribute("config-file"))
        assertEquals(0, descriptor.getElementsByTagName("fileEditorProvider").asElements()
            .count { it.getAttribute("implementation") == OPEN_API_PROVIDER })

        val config = descriptor("META-INF/spring-web-jcef.xml")
        assertEquals(1, config.getElementsByTagName("fileEditorProvider").asElements()
            .count { it.getAttribute("implementation") == OPEN_API_PROVIDER })
    }

    @Test
    fun testMainDescriptorImplementationsDoNotReferenceJcef() {
        val descriptor = descriptor("META-INF/spring-web-plugin.xml")
        val implementationNames = descriptor.allElements().flatMap { element ->
            IMPLEMENTATION_ATTRIBUTES.mapNotNull { attribute -> element.getAttribute(attribute).takeIf(String::isNotEmpty) }
        }
        assertTrue("precondition: main descriptor has no registered implementations", implementationNames.isNotEmpty())
        implementationNames.forEach { className ->
            val bytes = classBytes(className)
            assertNotNull("compiled class is absent: $className", bytes)
            assertFalse("main descriptor class references JCEF: $className", bytes!!.containsJcefMarker())
        }
    }

    @Test
    fun testBytecodeGuardDetectsKnownJcefClass() {
        val bytes = classBytes("com.explyt.spring.web.editor.openapi.OpenApiCefBrowser")
        assertNotNull("precondition: known JCEF class is compiled", bytes)
        assertTrue("negative control must contain the JCEF marker", bytes!!.containsJcefMarker())
    }

    private fun descriptor(resource: String) = resourceAsStream(resource).use { stream ->
        assertNotNull("precondition: descriptor resource is present: $resource", stream)
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream)
    }

    private fun resourceAsStream(resource: String) = javaClass.classLoader.getResourceAsStream(resource)

    private fun classBytes(className: String): ByteArray? =
        resourceAsStream("${className.replace('.', '/')}.class")?.use { it.readBytes() }

    private fun ByteArray.containsJcefMarker() =
        toString(StandardCharsets.ISO_8859_1).contains("com/intellij/ui/jcef/")

    private fun org.w3c.dom.NodeList.asElements() = (0 until length).map { item(it) as Element }

    private fun org.w3c.dom.Document.allElements(): List<Element> {
        val elements = getElementsByTagName("*")
        return (0 until elements.length).map { elements.item(it) as Element }
    }

    private companion object {
        const val OPEN_API_PROVIDER = "com.explyt.spring.web.editor.openapi.OpenApiUIEditorProvider"
        val IMPLEMENTATION_ATTRIBUTES = listOf("implementation", "implementationClass", "serviceImplementation", "class")
    }
}
