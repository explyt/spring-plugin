package com.esprito.spring.test

import com.intellij.jarRepository.JarRepositoryManager
import com.intellij.jarRepository.RemoteRepositoryDescription
import com.intellij.jarRepository.RepositoryLibraryType
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryProperties

data class TestLibrary(val mavenCoordinates: String, val includeTransitiveDependencies: Boolean = true) {
    companion object {
        val springBoot_3_1_1: TestLibrary = TestLibrary("org.springframework.boot:spring-boot:3.1.1")
        val springBootAutoConfigure_3_1_1: TestLibrary = TestLibrary("org.springframework.boot:spring-boot-autoconfigure:3.1.1")
        val springContext_6_0_7 = TestLibrary("org.springframework:spring-context:6.0.7")
        val springDataJpa_3_1_0 = TestLibrary("org.springframework.data:spring-data-jpa:3.1.0", true)
        val jakarta_persistence_3_1_0 = TestLibrary("jakarta.persistence:jakarta.persistence-api:3.1.0", true)
        val javax_persistence_2_2 = TestLibrary("javax.persistence:javax.persistence-api:2.2", true)
    }
}

/**
 * This was copied from `com.intellij.testFramework.fixtures.MavenDependencyUtil#addFromMaven`, and modified to remove the call to
 * `getRemoteRepositoryDescriptions()` which only works when the `intellij-community` repo is available, and the `idea.home.path`
 * environment variable points to it.
 *
 * For this to work a "mock jdk" must still be present in `<idea.home.path>/java/mockJDK-1.7/jre/lib/rt.jar`.
 *
 * See https://jetbrains-platform.slack.com/archives/CPL5291JP/p1664105522154139 and https://youtrack.jetbrains.com/issue/IJSDK-321
 */
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
