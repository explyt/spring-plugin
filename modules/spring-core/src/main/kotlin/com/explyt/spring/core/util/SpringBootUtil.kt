/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util

import com.explyt.spring.core.util.SpringCoreUtil.SPRING_BOOT_MAVEN
import com.intellij.java.library.JavaLibraryUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.ArrayUtil
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.text.VersionComparatorUtil
import org.jetbrains.kotlin.idea.base.util.module


private const val SPRING_BOOT_STARTER_SUFFIX = "-starter"
private const val SPRING_BOOT_MAIN_STARTER = "spring-boot-starter"
private const val SPRING_BOOT_KAFKA = "spring-kafka"
private const val SPRING_BOOT_3_VERSION = "3.0"
private const val SPRING_BOOT_3_4_VERSION = "3.4"
private const val SPRING_BOOT_4_VERSION = "4.0"

object SpringBootUtil {

    @Suppress("UnstableApiUsage")
    fun getSpringBootVersion(module: Module): SpringBootVersion? {
        if (module.isDisposed || module.project.isDefault) return null

        return CachedValuesManager.getManager(module.project).getCachedValue(
            module
        ) {
            val libraryVersion = JavaLibraryUtil.getLibraryVersion(module, SPRING_BOOT_MAVEN)

            val detected =
                if (libraryVersion == null) null else ContainerUtil.find(
                    ArrayUtil.reverseArray(
                        SpringBootVersion.entries.toTypedArray()
                    )
                ) { version: SpringBootVersion ->
                    VersionComparatorUtil.compare(
                        version.version,
                        libraryVersion
                    ) <= 0
                }
            CachedValueProvider.Result.create(
                detected,
                ProjectRootManager.getInstance(module.project)
            )
        }
    }

    /**
     * Returns `true` when the detected Spring Boot version is 3.0 or later.
     * Many Spring Boot 3.0 migration rules (Jakarta EE namespace, configuration-property renames, etc.)
     * only apply to projects running on Spring Boot 3+.
     * Since Spring Boot 3.0 `@ConstructorBinding` is no longer needed at the type level and a single-constructor
     * `@ConfigurationProperties` class is bound through its constructor automatically.
     */
    fun isAtLeastSpringBoot3(psiElement: PsiElement): Boolean {
        val version = psiElement.module?.let { getSpringBootVersion(it) } ?: return false
        return version >= SpringBootVersion.VERSION_3_0_0
    }

    /**
     * Returns `true` when the detected Spring Boot version is 3.4 or later.
     * Spring Boot 3.4 upgrades to Spring Framework 6.2, which is where the bean-override annotations
     * (`@MockitoBean`, `@MockitoSpyBean`, `@TestBean`) appear and where their Spring Boot predecessors are
     * deprecated for removal, so 3.4 opens the window in which such a migration can already be applied.
     */
    @RequiresReadLock
    fun isAtLeastSpringBoot34(psiElement: PsiElement): Boolean {
        val version = getSpringBootVersion(psiElement)
        if (version.isEmpty()) return false
        return VersionComparatorUtil.compare(version, SPRING_BOOT_3_4_VERSION) >= 0
    }

    /**
     * Returns `true` when the detected Spring Boot version is 4.0 or later.
     */
    fun isAtLeastSpringBoot4(psiElement: PsiElement): Boolean {
        val version = psiElement.module?.let { getSpringBootVersion(it) } ?: return false
        return version >= SpringBootVersion.VERSION_4_0_0
    }

    enum class SpringBootVersion(val version: String) {
        ANY("1.0.0"),

        VERSION_3_0_0("3.0.0"),
        VERSION_4_0_0("4.0.0");
    }

}