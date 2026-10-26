/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.initializr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.nio.charset.StandardCharsets
import javax.xml.parsers.DocumentBuilderFactory

class SpringInitializrJcefDescriptorTest {

    @Test
    fun testJcefDependencyIsOptionalAndModuleTypeIsMovedToConfig() {
        val descriptor = descriptor("META-INF/spring-initializr-plugin.xml")
        val jcef = descriptor.getElementsByTagName("depends").asElements()
            .single { it.textContent.trim() == "com.intellij.modules.jcef" }

        assertEquals("true", jcef.getAttribute("optional"))
        assertEquals("spring-initializr-jcef.xml", jcef.getAttribute("config-file"))
        assertEquals(0, descriptor.getElementsByTagName("moduleType").asElements()
            .count { it.getAttribute("id") == MODULE_TYPE })

        val config = descriptor("META-INF/spring-initializr-jcef.xml")
        val moduleTypes = config.getElementsByTagName("moduleType").asElements()
        assertEquals(1, moduleTypes.count { it.getAttribute("id") == MODULE_TYPE })
        assertEquals(INITIALIZR_MODULE_TYPE, moduleTypes.single { it.getAttribute("id") == MODULE_TYPE }
            .getAttribute("implementationClass"))
    }

    @Test
    fun testMainDescriptorImplementationsDoNotReferenceJcef() {
        val descriptor = descriptor("META-INF/spring-initializr-plugin.xml")
        val implementationNames = descriptor.allElements().flatMap { element ->
            IMPLEMENTATION_ATTRIBUTES.mapNotNull { attribute -> element.getAttribute(attribute).takeIf(String::isNotEmpty) }
        }
        assertTrue("precondition: main descriptor has registered implementations", implementationNames.isNotEmpty())
        implementationNames.forEach { className ->
            val bytes = classBytes(className)
            assertNotNull("compiled class is absent: $className", bytes)
            assertFalse("main descriptor class references JCEF: $className", bytes!!.containsJcefMarker())
        }
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
        const val MODULE_TYPE = "EXPLYT_SPRING_INITIALIZR"
        const val INITIALIZR_MODULE_TYPE = "com.explyt.spring.initializr.ExplytSpringInitializrModuleType"
        val IMPLEMENTATION_ATTRIBUTES = listOf("implementation", "implementationClass", "serviceImplementation", "class")
    }
}
