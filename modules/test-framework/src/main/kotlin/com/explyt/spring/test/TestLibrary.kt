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

package com.explyt.spring.test

import com.intellij.jarRepository.JarRepositoryManager
import com.intellij.jarRepository.RemoteRepositoryDescription
import com.intellij.jarRepository.RepositoryLibraryType
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryProperties

data class TestLibrary(val mavenCoordinates: String, val includeTransitiveDependencies: Boolean = true) {
    companion object {
        val springContext_6_0_7 = TestLibrary("org.springframework:spring-context:6.0.7")
        val springTx_6_0_7 = TestLibrary("org.springframework:spring-tx:6.0.7")
        val springTest_6_0_7 = TestLibrary("org.springframework:spring-test:6.0.7")
        val springWeb_6_0_7 = TestLibrary("org.springframework:spring-web:6.0.7", true)
        val springReactiveWeb_3_1_1 = TestLibrary("org.springframework.boot:spring-boot-starter-webflux:3.1.1", true)
        val springCloud_4_1_3 = TestLibrary("org.springframework.cloud:spring-cloud-starter-openfeign:4.1.3", true)

        val springBoot_3_1_1: TestLibrary = TestLibrary("org.springframework.boot:spring-boot:3.1.1")
        val springBootAutoConfigure_3_1_1: TestLibrary = TestLibrary("org.springframework.boot:spring-boot-autoconfigure:3.1.1")
        val springBootTestAutoConfigure_3_1_1: TestLibrary =
            TestLibrary("org.springframework.boot:spring-boot-test-autoconfigure:3.1.1")
        val springDataJpa_3_1_0 = TestLibrary("org.springframework.data:spring-data-jpa:3.1.0", true)
        val springDataJpa_3_4_0 = TestLibrary("org.springframework.data:spring-data-jpa:3.4.0", true)
        val springGraphQl_1_0_4 = TestLibrary("org.springframework.graphql:spring-graphql:1.0.4", true)
        val jakarta_annotation_2_1_1 = TestLibrary("jakarta.annotation:jakarta.annotation-api:2.1.1", true)
        val jakarta_inject_2_0_1 = TestLibrary("jakarta.inject:jakarta.inject-api:2.0.1", true)
        val jakarta_persistence_3_1_0 = TestLibrary("jakarta.persistence:jakarta.persistence-api:3.1.0", true)
        val javax_annotation_1_3_2 = TestLibrary("javax.annotation:javax.annotation-api:1.3.2", true)
        val javax_inject_1 = TestLibrary("javax.inject:javax.inject:1", true)
        val javax_persistence_2_2 = TestLibrary("javax.persistence:javax.persistence-api:2.2", true)

        val springSecurityTest_6_0_7 = TestLibrary("org.springframework.security:spring-security-test:6.0.7", true)

        val kotlin_1_9_22 = TestLibrary("org.jetbrains.kotlin:kotlin-stdlib:1.9.22", true)
        val kotlin_coroutines_1_7_1 = TestLibrary("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1", true)

        val springAop_6_0_7 = TestLibrary("org.springframework:spring-aop:6.0.7", true)
        val aspectjWeaver_1_9_7 = TestLibrary("org.aspectj:aspectjweaver:1.9.7", true)

        val slf4j_2_0_7 = TestLibrary("org.slf4j:slf4j-api:2.0.7", true)

        val resilience4j_2_2_0 = TestLibrary("io.github.resilience4j:resilience4j-spring-boot3:2.2.0", true)
        val hibernate_5_6_15 = TestLibrary("org.hibernate:hibernate-entitymanager:5.6.15.Final", true)
        val springJdbc_6_2_5 = TestLibrary("org.springframework:spring-jdbc:6.2.5", true)
    }
}

fun addFromMaven(
    model: ModifiableRootModel,
    mavenCoordinates: String,
    includeTransitiveDependencies: Boolean = true,
    dependencyScope: DependencyScope = DependencyScope.COMPILE,
) {
    val remoteRepositoryDescriptions = listOf(RemoteRepositoryDescription.MAVEN_CENTRAL)
    val libraryProperties = RepositoryLibraryProperties(mavenCoordinates, includeTransitiveDependencies)
    val roots = JarRepositoryManager.loadDependenciesModal(
        model.project,
        libraryProperties,
        false,
        false,
        null,
        remoteRepositoryDescriptions
    )
    val tableModel = model.moduleLibraryTable.modifiableModel
    val library = tableModel.createLibrary(mavenCoordinates, RepositoryLibraryType.REPOSITORY_LIBRARY_KIND)
    val libraryModel = library.modifiableModel
    check(!roots.isEmpty()) { String.format("No roots for '%s'", mavenCoordinates) }
    for (root in roots) {
        libraryModel.addRoot(root.file, root.type)
    }
    (libraryModel as LibraryEx.ModifiableModelEx).properties = libraryProperties
    val libraryOrderEntry = model.findLibraryOrderEntry(library)
        ?: throw IllegalStateException("Unable to find registered library $mavenCoordinates")
    libraryOrderEntry.scope = dependencyScope
    libraryModel.commit()
    tableModel.commit()
}
