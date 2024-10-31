package com.esprito.spring.data.inspection.kotlin

import com.esprito.spring.data.inspection.SpringDataEnableInspection
import com.esprito.spring.test.ExplytKotlinLightTestCase
import com.esprito.spring.test.TestLibrary
import com.intellij.openapi.util.registry.Registry

class SpringDataEnableInspectionTest : ExplytKotlinLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1, TestLibrary.springContext_6_0_7, TestLibrary.springDataJpa_3_1_0
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringDataEnableInspection::class.java)
        myFixture.addClass(
            """
            public class Entity {
            		public Integer id;
            		public String name;
                    public String city;
            }
        """.trimIndent()
        )
        Registry.get("esprito.spring.root.runConfiguration").setValue(false)
    }

    override fun tearDown() {
        super.tearDown()
        Registry.get("esprito.spring.root.runConfiguration").resetToDefault()
    }


    fun testEnabled() {
        myFixture.configureByText(
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun findByNameAndCity(): Entity            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }

    fun testEnabledWithProperty() {
        myFixture.addFileToProject("application.properties", "spring.data.jpa.repositories.enabled=true")
        myFixture.configureByText(
            "TestRepository.kt",
            """            
            interface TestRepository : org.springframework.data.repository.Repository<Entity, Int> {
            	fun findByNameAndCity(): Entity            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }

    fun testDisabledWithProperty() {
        myFixture.addFileToProject("application.properties", "spring.data.jpa.repositories.enabled=false")
        myFixture.configureByText(
            "TestRepository.kt",
            """            
            interface <warning>TestRepository</warning> : org.springframework.data.repository.Repository<Entity, Int> {
            	fun findByNameAndCity(): Entity            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.kt")
    }

    fun testDifferentPackage() {
        val file = myFixture.copyDirectoryToProject("inspection/repoPackageTest", "")
        val appRepoFile = file.findChild("com")?.findChild("app")?.findChild("AppRepo.kt") ?: throw Exception()
        myFixture.configureFromExistingVirtualFile(appRepoFile)
        myFixture.testHighlighting(true, false, true, appRepoFile)

        val outerRepoFile = file.findChild("com")?.findChild("outer")?.findChild("OuterRepo.kt") ?: throw Exception()
        myFixture.configureFromExistingVirtualFile(outerRepoFile)
        myFixture.testHighlighting(true, false, true, outerRepoFile)
    }
}
