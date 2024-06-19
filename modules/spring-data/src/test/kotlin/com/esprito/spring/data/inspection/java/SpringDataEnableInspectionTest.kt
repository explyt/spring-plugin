package com.esprito.spring.data.inspection.java

import com.esprito.spring.data.inspection.SpringDataEnableInspection
import com.esprito.spring.test.EspritoJavaLightTestCase
import com.esprito.spring.test.TestLibrary
import com.intellij.openapi.util.registry.Registry

class SpringDataEnableInspectionTest : EspritoJavaLightTestCase() {
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
            "TestRepository.java",
            """            
            public interface TestRepository extends org.springframework.data.repository.Repository<Entity, Integer> {
            	Entity findByNameAndCity();            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.java")
    }

    fun testEnabledWithProperty() {
        myFixture.addFileToProject("application.properties", "spring.data.jpa.repositories.enabled=true")
        myFixture.configureByText(
            "TestRepository.java",
            """            
            public interface TestRepository extends org.springframework.data.repository.Repository<Entity, Integer> {
            	Entity findByNameAndCity();            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.java")
    }

    fun testDisabledWithProperty() {
        myFixture.addFileToProject("application.properties", "spring.data.jpa.repositories.enabled=false")
        myFixture.configureByText(
            "TestRepository.java",
            """            
            public interface <warning>TestRepository</warning> extends org.springframework.data.repository.Repository<Entity, Integer> {
            	Entity findByNameAndCity();            	
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestRepository.java")
    }

    /*  Access to tree elements not allowed for '/src/com/app/Application.java'.
        Try using stub-based PSI API to avoid expensive AST loading for files that aren't already opened in the editor.
        Consult this method's javadoc for more details.

        fun testDifferentPackage() {
            val file = myFixture.copyDirectoryToProject("inspection/repoPackageTest", "")
            val appRepoFile = file.findChild("com")?.findChild("app")?.findChild("AppRepo.java") ?: throw Exception()
            myFixture.configureFromExistingVirtualFile(appRepoFile)
            myFixture.testHighlighting(true, false, true, appRepoFile)

            val outerRepoFile = file.findChild("com")?.findChild("outer")?.findChild("OuterRepo.java") ?: throw Exception()
            myFixture.configureFromExistingVirtualFile(outerRepoFile)
            myFixture.testHighlighting(true, false, true, outerRepoFile)
        }*/
}
