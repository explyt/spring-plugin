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

package com.explyt.spring.data.inspection.java

import com.explyt.spring.data.inspection.SpringDataEnableInspection
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.util.registry.Registry

class SpringDataEnableInspectionTest : ExplytJavaLightTestCase() {
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
        Registry.get("explyt.spring.root.runConfiguration").setValue(false)
    }

    override fun tearDown() {
        super.tearDown()
        Registry.get("explyt.spring.root.runConfiguration").resetToDefault()
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
