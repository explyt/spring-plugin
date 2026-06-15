/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.java

import com.explyt.spring.core.properties.providers.ConfigurationPropertyKeyReference
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference
import org.intellij.lang.annotations.Language

class RenameConfigurationPropertyReferenceTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + "properties/rename"

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1
    )

    fun testRenameFromYaml() {
        myFixture.configureByFile(
            "LssConfigurationProperties.java",
        )
        myFixture.addFileToProject(
            "application-config.properties",
            "lss.lss-plan-configuration.exact=false"
        )
        myFixture.configureByText(
            "application.yaml",
            """
lss:
  lss-plan-configuration:
    exa<caret>ct: true
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
        myFixture.renameElement(resolveResult.element!!, "setExactNew")

        myFixture.checkResult(
            """
lss:
  lss-plan-configuration:
    exact-new: true
""".trimIndent()
        )

        myFixture.checkResult("LssConfigurationProperties.java", LssConfigurationProperties_after, true)
        myFixture.checkResult("application-config.properties", "lss.lss-plan-configuration.exact-new=false", true)
    }

    fun testRenameFromProperties() {
        myFixture.configureByFile(
            "LssConfigurationProperties.java",
        )
        myFixture.addFileToProject(
            "application-config.yaml",
            """
lss:
  lss-plan-configuration:
    exact: true
""".trimIndent()
        )
        myFixture.configureByText(
            "application-config.properties",
            "lss.lss-plan-configuration.exa<caret>ct=false"
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
        myFixture.renameElement(resolveResult.element!!, "setExactNew")

        myFixture.checkResult(
            """
lss.lss-plan-configuration.exact-new=false
""".trimIndent()
        )

        myFixture.checkResult("LssConfigurationProperties.java", LssConfigurationProperties_after, true)
        myFixture.checkResult("application-config.yaml", """
lss:
  lss-plan-configuration:
    exact-new: true
        """.trimIndent(), true)
    }

    fun testRenameFromMethod() {
        myFixture.addFileToProject(
            "application-config.properties",
            "lss.lss-plan-configuration.exact=false"
        )
        myFixture.addFileToProject(
            "application-config.yaml",
            """
lss:
  lss-plan-configuration:
    exact: true
""".trimIndent()
        )
        myFixture.configureByFile("LssConfigurationProperties.java")
        val element = myFixture.findElementByText("setExact", PsiElement::class.java)

        assertNotNull(element)
        myFixture.renameElement(element!!, "setExactNew")

        myFixture.checkResult(LssConfigurationProperties_after, true)
        myFixture.checkResult(
            "application-config.yaml", """
lss:
  lss-plan-configuration:
    exact-new: true
        """.trimIndent(), true
        )
        myFixture.checkResult("application-config.properties", "lss.lss-plan-configuration.exact-new=false", true)
    }

}

@Language("JAVA")
private val LssConfigurationProperties_after = """           
            package com;

            import org.springframework.boot.context.properties.ConfigurationProperties;

            @ConfigurationProperties(prefix = "lss")
            public class LssConfigurationProperties {
                private LssPlanConfiguration lssPlanConfiguration = new LssPlanConfiguration();

                public LssPlanConfiguration getLssPlanConfiguration() {
                    return lssPlanConfiguration;
                }

                public void setLssPlanConfiguration(LssPlanConfiguration lssPlanConfiguration) {
                    this.lssPlanConfiguration = lssPlanConfiguration;
                }

                static class LssPlanConfiguration {
                    private Boolean isExact = false;

                    public Boolean getExact() {
                        return isExact;
                    }

                    public void setExactNew(Boolean exact) {
                        isExact = exact;
                    }
                }
            }
        """.trimIndent()