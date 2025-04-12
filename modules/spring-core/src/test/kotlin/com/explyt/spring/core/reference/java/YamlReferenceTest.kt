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
import com.explyt.spring.core.properties.references.ValueHintReference
import com.explyt.spring.core.properties.references.YamlKeyMapValueReference
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.json.psi.impl.JsonPropertyImpl
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReference

class YamlReferenceTest : ExplytJavaLightTestCase() {
    override fun getTestDataPath(): String = super.getTestDataPath() + "reference/properties"

    override val libraries: Array<TestLibrary> =
        arrayOf(
            TestLibrary.springBoot_3_1_1,
            TestLibrary.springContext_6_0_7,
            TestLibrary.springCloud_4_1_3,
            TestLibrary.resilience4j_2_2_0,
        )

    fun testRefValueByHintsProvidersClassReference() {
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.configureByText(
            "application.yaml",
            """
main:
  event-listener: org.springframework.boot.context.logging.Log<caret>gingApplicationListener                
            """.trimIndent()
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
            "application.yaml",
            """
main:
  foo-bean-component: fooBean<caret>Component                
            """.trimIndent()
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
            "application.yaml",
            """
lss:
  lss-plan-<caret>configuration:                
            """.trimIndent()
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

    fun testRefKeyInnerClassField() {
        myFixture.copyFileToProject("LssConfigurationProperties.java")
        myFixture.configureByText(
            "application.yaml",
            """
lss:
  lss-plan-configuration:
    exa<caret>ct:
            """.trimIndent()
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
        assertEquals(name, "setExact")
    }

    fun testRefKeyInnerClassFieldBoolean() {
        myFixture.copyFileToProject("LssConfigurationProperties.java")
        myFixture.configureByText(
            "application.yaml",
            """
lss:
  lss-plan-configuration:
    is-exa<caret>ct:
            """.trimIndent()
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
        assertEquals(name, "setExact")
    }

    fun testRefKeyMap() {
        myFixture.configureByText(
            "application.yaml",
            """
resilience4j:
  ratelimiter:
    inst<caret>ances:
      lssRateLimiterCreateTask:
        limit-for-period:
                    """.trimIndent()
        )

        val ref = file.findReferenceAt(myFixture.caretOffset) as? YamlKeyMapValueReference
        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val name = (multiResolve[0].element as? ConfigKeyPsiElement)?.name
        assertEquals(name, "instances")
    }

    fun testRefKeyMapKey() {
        myFixture.configureByText(
            "application.yaml",
            """
resilience4j:
  ratelimiter:
    instances:
      lssRate<caret>LimiterCreateTask:
        limit-for-period:
                    """.trimIndent()
        )

        val ref = file.findReferenceAt(myFixture.caretOffset) as? YamlKeyMapValueReference
        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val name = (multiResolve[0].element as? ConfigKeyPsiElement)?.name
        assertEquals("instances", name)
    }

    fun testRefKeyMapValue() {
        myFixture.configureByText(
            "application.yaml",
            """
resilience4j:
  ratelimiter:
    instances:
      lssRateLimiterCreateTask:
        limit-<caret>for-period:
                    """.trimIndent()
        )

        val ref = (file.findReferenceAt(myFixture.caretOffset) as? PsiMultiReference)
            ?.references?.asSequence()
            ?.mapNotNull {
                it as? YamlKeyMapValueReference
            }?.firstOrNull()

        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val name = (multiResolve[0].element as? ConfigKeyPsiElement)?.name
        assertEquals(name, "setLimitForPeriod")
    }

    fun testRefKeyMapValueInnerClass() {
        myFixture.configureByText(
            "application.yaml",
            """
spring:
  cloud:
    openfeign:
      client:
        config:
          test:
            micrometer:
              ena<caret>bled: true
        """.trimIndent()
        )

        val ref = file.findReferenceAt(myFixture.caretOffset) as? YamlKeyMapValueReference
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
            "application.yaml",
            """
main:
  local:
    code-resource: classpath:application-de<caret>fault.properties                
            """.trimIndent()
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
            "application.yaml",
            """
main:
  local:
    event-<caret>listener:                
            """.trimIndent()
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
            "application.yaml",
            """
main:
  local:
    event_<caret>listener:                
            """.trimIndent()
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
            "application.yaml",
            """
main:
  local:
    event<caret>Listener:                
            """.trimIndent()
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
            "application.yaml",
            """
main:
  local:
    EVENT_<caret>LISTENER:                
            """.trimIndent()
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
            "application.yaml",
            """
spring:
  cloud:
    openfeign:
      client:
        config:
          test:
            connect-<caret>timeout:
            """.trimIndent()
        )
        val ref = (file.findReferenceAt(myFixture.caretOffset) as? PsiMultiReference)
            ?.references?.asSequence()
            ?.mapNotNull {
                it as? YamlKeyMapValueReference
            }?.firstOrNull()

        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolveResult = multiResolve[0]
        val name = (resolveResult.element as? ConfigKeyPsiElement)?.name
        assertEquals(name, "setConnectTimeout")
    }

    fun testRefKeyMapValueRelaxedBindingUnderscore() {
        myFixture.configureByText(
            "application.yaml",
            """
spring:
  cloud:
    openfeign:
      client:
        config:
          test:
            connect_<caret>timeout:            
            """.trimIndent()
        )
        val ref = (file.findReferenceAt(myFixture.caretOffset) as? PsiMultiReference)
            ?.references?.asSequence()
            ?.mapNotNull {
                it as? YamlKeyMapValueReference
            }?.firstOrNull()

        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolveResult = multiResolve[0]
        val name = (resolveResult.element as? ConfigKeyPsiElement)?.name
        assertEquals(name, "setConnectTimeout")
    }

    fun testRefKeyMapValueRelaxedBindingCamelCase() {
        myFixture.configureByText(
            "application.yaml",
            """
spring:
  cloud:
    openfeign:
      client:
        config:
          test:
            connect<caret>Timeout:           
            """.trimIndent()
        )
        val ref = (file.findReferenceAt(myFixture.caretOffset) as? PsiMultiReference)
            ?.references?.asSequence()
            ?.mapNotNull {
                it as? YamlKeyMapValueReference
            }?.firstOrNull()

        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolveResult = multiResolve[0]
        val name = (resolveResult.element as? ConfigKeyPsiElement)?.name
        assertEquals(name, "setConnectTimeout")
    }

    fun testRefKeyMapValueRelaxedBindingUpperCase() {
        myFixture.configureByText(
            "application.yaml",
            """
spring:
  cloud:
    openfeign:
      client:
        config:
          test:
            CONNECT_<caret>TIMEOUT:            
            """.trimIndent()
        )
        val ref = (file.findReferenceAt(myFixture.caretOffset) as? PsiMultiReference)
            ?.references?.asSequence()
            ?.mapNotNull {
                it as? YamlKeyMapValueReference
            }?.firstOrNull()

        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolveResult = multiResolve[0]
        val name = (resolveResult.element as? ConfigKeyPsiElement)?.name
        assertEquals(name, "setConnectTimeout")
    }

    fun testLoggingLevel() {
        myFixture.configureByText(
            "application.yaml",
            """
logging:
  le<caret>vel:
    org.hibernate.SQL: debug          
            """.trimIndent()
        )
        val ref = (file.findReferenceAt(myFixture.caretOffset) as? ConfigurationPropertyKeyReference)
        assertNotNull(ref)

    }

    fun testLoggingLevelJavaClass() {
        myFixture.configureByText(
            "application.yaml",
            """
logging:
  level:
    org.hiber<caret>nate.SQL: debug          
            """.trimIndent()
        )
        val ref = (file.findReferenceAt(myFixture.caretOffset) as? JavaClassReference)
        assertNotNull(ref)
    }

    fun testHandleAsValues() {
        myFixture.copyFileToProject("META-INF/additional-spring-configuration-metadata.json")
        myFixture.copyFileToProject("WeekEnum.java")
        myFixture.configureByText(
            "application.yaml",
            """
main:
  enum-value-additional: TUE<caret>SDAY
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
            "application.yaml",
            """
main:
  name: cre<caret>ate
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
            "application.yaml",
            """
logging:
    level:
        sql: in<caret>fo
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
            "application.yaml",
            """
logging:
  level:
    org.hibernate.SQL: deb<caret>ug
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