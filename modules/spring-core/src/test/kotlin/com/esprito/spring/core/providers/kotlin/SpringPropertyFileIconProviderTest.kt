package com.explyt.spring.core.providers.kotlin

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.providers.SpringPropertyFileIconProvider
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiFile
import org.jetbrains.yaml.psi.YAMLFile

class SpringPropertyFileIconProviderTest : ExplytKotlinLightTestCase() {

    private lateinit var iconProvider: SpringPropertyFileIconProvider

    override fun setUp() {
        super.setUp()
        iconProvider = SpringPropertyFileIconProvider()
    }

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springTest_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1
    )

    fun testReturnIconInPropertyYaml() {
        val yamlFile = myFixture.configureByText("application-test.yaml", "").virtualFile
        val psiFile = myFixture.psiManager.findFile(yamlFile) as YAMLFile

        val icon = iconProvider.getIcon(psiFile, 0)

        assertNotNull(icon)
        assertEquals(icon, SpringIcons.SpringSetting)
    }

    fun testNotReturnIconInPropertyYaml() {
        val yamlFile = myFixture.configureByText("other-application.yaml", "").virtualFile
        val psiFile = myFixture.psiManager.findFile(yamlFile) as YAMLFile
        val icon = iconProvider.getIcon(psiFile, 0)

        assertNull(icon)
    }

    fun testReturnIconInPropertyProperty() {
        val file = myFixture.configureByText("application-test.properties", "").virtualFile
        val propertiesFile = myFixture.psiManager.findFile(file)

        val icon = iconProvider.getIcon(propertiesFile as PsiFile, 0)

        assertNotNull(icon)
        assertEquals(icon, SpringIcons.SpringSetting)
    }

    fun testNotReturnIconInPropertyProperty() {
        val file = myFixture.configureByText("other-application.properties", "").virtualFile
        val propertiesFile = myFixture.psiManager.findFile(file)
        val icon = iconProvider.getIcon(propertiesFile as PsiFile, 0)

        assertNull(icon)
    }
}