/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core.reference.java

import com.explyt.spring.core.properties.providers.ConfigKeyPsiElement
import com.explyt.spring.core.properties.providers.ConfigurationPropertyKeyReference
import com.explyt.spring.core.properties.references.PropertiesKeyMapValueReference
import com.explyt.spring.core.properties.references.ValueHintReference
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.json.psi.impl.JsonPropertyImpl
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReference

class PropertiesReferenceTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + "reference/properties"

    override val libraries: Array<TestLibrary> =
        arrayOf(
            TestLibrary.springBoot_3_1_1,
            TestLibrary.springContext_6_0_7,
            TestLibrary.springCloud_4_1_3,
            TestLibrary.resilience4j_2_2_0
        )

    fun testRefValueByHintsProvidersClassReference() {
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText(
            "application.properties",
            "main.event-listener=org.springframework.boot.context.logging.Log<caret>gingApplicationListener"
        )
        val ref = file.findReferenceAt(myFixture.caretOffset) as? ValueHintReference

        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolveResult = multiResolve[0]
        val nameClass = (resolveResult.element as? PsiClass)?.name
        assertEquals(nameClass, "LoggingApplicationListener")
    }

    fun testRefValueSpringBeanReference() {
        myFixture.copyFileToProject("FooBeanComponent.java")
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText(
            "application.properties",
            "main.foo-bean-component=fooBean<caret>Component"
        )
        val ref = file.findReferenceAt(myFixture.caretOffset) as? ValueHintReference

        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolveResult = multiResolve[0]
        val nameClass = (resolveResult.element as? PsiClass)?.name
        assertEquals(nameClass, "FooBeanComponent")
    }

    fun testRefKeyInnerClass() {
        myFixture.copyFileToProject("LssConfigurationProperties.java")
        myFixture.configureByText(
            "application.properties",
            "lss.lss-plan-<caret>configuration"
        )
        val ref = (file.findReferenceAt(myFixture.caretOffset) as? PsiMultiReference)
            ?.references?.asSequence()
            ?.mapNotNull {
                it as? ConfigurationPropertyKeyReference
            }?.firstOrNull()

        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolveResult = multiResolve[0]
        val nameClass = (resolveResult.element as? ConfigKeyPsiElement)?.name
        assertEquals(nameClass, "setLssPlanConfiguration")
    }

    fun testRefKeyMapValue() {
        myFixture.configureByText(
            "application.properties",
            "resilience4j.ratelimiter.instances.test.limit-for<caret>-period=1"
        )

        val refs = (file.findReferenceAt(myFixture.caretOffset) as? PsiMultiReference)
            ?.references?.asSequence()
            ?.mapNotNull { it as? PropertiesKeyMapValueReference }
            ?.toList()

        assertNotNull(refs)
        assertNotEmpty(refs!!)
        val multiResolve = refs.map { it.multiResolve(true) }.firstOrNull { it.isNotEmpty() }
        assertNotNull(multiResolve)
        assertEquals(1, multiResolve!!.size)
        val name = (multiResolve[0].element as? ConfigKeyPsiElement)?.name
        assertEquals(name, "setLimitForPeriod")
    }

    fun testRefKeyMapValueInnerClass() {
        myFixture.configureByText(
            "application.properties",
            "spring.cloud.openfeign.client.config.test.micrometer.en<caret>abled"
        )

        val ref = (file.findReferenceAt(myFixture.caretOffset) as? PsiMultiReference)
            ?.references?.asSequence()
            ?.mapNotNull {
                it as? ConfigurationPropertyKeyReference
            }?.firstOrNull()

        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val name = (multiResolve[0].element as? ConfigKeyPsiElement)?.name
        assertEquals(name, "setEnabled")
    }

    fun testRefValueResource() {
        myFixture.copyFileToProject("MainFooProperties.java")
        myFixture.configureByText(
            "application-default.properties",
            "main.foo-bean-component=fooBeanComponent"
        )
        myFixture.configureByText(
            "application.properties",
            "main.local.code-resource=classpath:application-de<caret>fault.properties"
        )

        val ref = (file.findReferenceAt(myFixture.caretOffset) as? PsiMultiReference)
            ?.references?.asSequence()
            ?.mapNotNull {
                it as? FileReference
            }?.firstOrNull()

        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val name = (multiResolve[0].element as? PropertiesFileImpl)?.name
        assertEquals(name, "application-default.properties")
    }

    fun testRefKeyRelaxedBindingKebabCase() {
        myFixture.copyFileToProject("MainFooProperties.java")
        myFixture.configureByText(
            "application.properties",
            "main.local.event-<caret>listener"
        )
        val ref = (file.findReferenceAt(myFixture.caretOffset) as? PsiMultiReference)
            ?.references?.asSequence()
            ?.mapNotNull {
                it as? ConfigurationPropertyKeyReference
            }?.firstOrNull()

        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolveResult = multiResolve[0]
        val name = (resolveResult.element as? ConfigKeyPsiElement)?.name
        assertEquals(name, "setEventListener")
    }

    fun testRefKeyRelaxedBindingUnderscore() {
        myFixture.copyFileToProject("MainFooProperties.java")
        myFixture.configureByText(
            "application.properties",
            "main.local.event_<caret>listener"
        )

        val ref = (file.findReferenceAt(myFixture.caretOffset) as? PsiMultiReference)
            ?.references?.asSequence()
            ?.mapNotNull {
                it as? ConfigurationPropertyKeyReference
            }?.firstOrNull()

        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolveResult = multiResolve[0]
        val name = (resolveResult.element as? ConfigKeyPsiElement)?.name
        assertEquals(name, "setEventListener")
    }

    fun testRefKeyRelaxedBindingCamelCase() {
        myFixture.copyFileToProject("MainFooProperties.java")
        myFixture.configureByText(
            "application.properties",
            "main.local.event<caret>Listener"
        )
        val ref = (file.findReferenceAt(myFixture.caretOffset) as? PsiMultiReference)
            ?.references?.asSequence()
            ?.mapNotNull {
                it as? ConfigurationPropertyKeyReference
            }?.firstOrNull()

        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolveResult = multiResolve[0]
        val name = (resolveResult.element as? ConfigKeyPsiElement)?.name
        assertEquals(name, "setEventListener")
    }

    fun testRefKeyRelaxedBindingUpperCase() {
        myFixture.copyFileToProject("MainFooProperties.java")
        myFixture.configureByText(
            "application.properties",
            "main.local.EVENT_<caret>LISTENER"
        )
        val ref = (file.findReferenceAt(myFixture.caretOffset) as? PsiMultiReference)
            ?.references?.asSequence()
            ?.mapNotNull {
                it as? ConfigurationPropertyKeyReference
            }?.firstOrNull()

        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolveResult = multiResolve[0]
        val name = (resolveResult.element as? ConfigKeyPsiElement)?.name
        assertEquals(name, "setEventListener")
    }

    fun testRefKeyMapValueRelaxedBindingKebabCase() {
        myFixture.configureByText(
            "application.properties",
            "spring.cloud.openfeign.client.config.test.connect-<caret>timeout"
        )
        val refs = (file.findReferenceAt(myFixture.caretOffset) as? PsiMultiReference)
            ?.references?.asSequence()
            ?.mapNotNull { it as? PropertiesKeyMapValueReference }
            ?.toList()

        assertNotNull(refs)
        assertNotEmpty(refs!!)
        val multiResolve = refs.map { it.multiResolve(true) }.firstOrNull { it.isNotEmpty() }
        assertNotNull(multiResolve)
        assertEquals(1, multiResolve!!.size)
        val name = (multiResolve[0].element as? ConfigKeyPsiElement)?.name
        assertEquals(name, "setConnectTimeout")
    }

    fun testRefKeyMapValueRelaxedBindingUnderscore() {
        myFixture.configureByText(
            "application.properties",
            "spring.cloud.openfeign.client.config.test.connect_<caret>timeout"
        )

        val refs = (file.findReferenceAt(myFixture.caretOffset) as? PsiMultiReference)
            ?.references?.asSequence()
            ?.mapNotNull { it as? PropertiesKeyMapValueReference }
            ?.toList()

        assertNotNull(refs)
        assertNotEmpty(refs!!)
        val multiResolve = refs.map { it.multiResolve(true) }.firstOrNull { it.isNotEmpty() }
        assertNotNull(multiResolve)
        assertEquals(1, multiResolve!!.size)
        val name = (multiResolve[0].element as? ConfigKeyPsiElement)?.name
        assertEquals(name, "setConnectTimeout")
    }

    fun testRefKeyMapValueRelaxedBindingCamelCase() {
        myFixture.configureByText(
            "application.properties",
            "spring.cloud.openfeign.client.config.test.connect<caret>Timeout"
        )
        val refs = (file.findReferenceAt(myFixture.caretOffset) as? PsiMultiReference)
            ?.references?.asSequence()
            ?.mapNotNull { it as? PropertiesKeyMapValueReference }
            ?.toList()

        assertNotNull(refs)
        assertNotEmpty(refs!!)
        val multiResolve = refs.map { it.multiResolve(true) }.firstOrNull { it.isNotEmpty() }
        assertNotNull(multiResolve)
        assertEquals(1, multiResolve!!.size)
        val name = (multiResolve[0].element as? ConfigKeyPsiElement)?.name
        assertEquals(name, "setConnectTimeout")
    }

    fun testRefKeyMapValueRelaxedBindingUpperCase() {
        myFixture.configureByText(
            "application.properties",
            "spring.cloud.openfeign.client.config.test.CONNECT_<caret>TIMEOUT"
        )
        val refs = (file.findReferenceAt(myFixture.caretOffset) as? PsiMultiReference)
            ?.references?.asSequence()
            ?.mapNotNull { it as? PropertiesKeyMapValueReference }
            ?.toList()

        assertNotNull(refs)
        assertNotEmpty(refs!!)
        val multiResolve = refs.map { it.multiResolve(true) }.firstOrNull { it.isNotEmpty() }
        assertNotNull(multiResolve)
        assertEquals(1, multiResolve!!.size)
        val name = (multiResolve[0].element as? ConfigKeyPsiElement)?.name
        assertEquals(name, "setConnectTimeout")
    }

    fun testLoggingLevel() {
        myFixture.configureByText(
            "application.properties",
            """
logging.le<caret>vel          
            """.trimIndent()
        )
        val ref = (file.findReferenceAt(myFixture.caretOffset) as? ConfigurationPropertyKeyReference)
        assertNotNull(ref)

    }

    fun testLoggingLevelJavaClass() {
        myFixture.configureByText(
            "application.properties",
            """
logging.level.org.hiber<caret>nate.SQL=debug          
            """.trimIndent()
        )
        val ref = (file.findReferenceAt(myFixture.caretOffset) as? JavaClassReference)
        assertNotNull(ref)
    }

    fun testHandleAsValues() {
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.copyFileToProject("WeekEnum.java")
        myFixture.configureByText(
            "application.properties",
            """
main.enum-value-additional=TUE<caret>SDAY
            """.trimIndent()
        )
        val ref = (file.findReferenceAt(myFixture.caretOffset) as? ValueHintReference)
        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolveResult = multiResolve[0]
        val name = (resolveResult.element as? PsiEnumConstant)?.name
        assertEquals(name, "TUESDAY")
    }

    fun testHintsValues() {
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText(
            "application.properties",
            """
main.name=cre<caret>ate
            """.trimIndent()
        )
        val ref = (file.findReferenceAt(myFixture.caretOffset) as? ValueHintReference)
        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolveResult = multiResolve[0]
        val name = (resolveResult.element as? JsonPropertyImpl)?.name
        assertEquals(name, "value")
    }

    fun testLoggingLevelValue() {
        myFixture.configureByText(
            "application.properties",
            """
logging.level.sql=in<caret>fo
            """.trimIndent()
        )
        val ref = (file.findReferenceAt(myFixture.caretOffset) as? ValueHintReference)
        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolveResult = multiResolve[0]
        val name = (resolveResult.element as? JsonPropertyImpl)?.name
        assertEquals(name, "value")
    }

    fun testLoggingLevelJavaClassValue() {
        myFixture.configureByText(
            "application.properties",
            """
logging.level.org.hibernate.SQL=deb<caret>ug
""".trimIndent()
        )
        val ref = (file.findReferenceAt(myFixture.caretOffset) as? ValueHintReference)
        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolveResult = multiResolve[0]
        val name = (resolveResult.element as? JsonPropertyImpl)?.name
        assertEquals(name, "value")
    }
}