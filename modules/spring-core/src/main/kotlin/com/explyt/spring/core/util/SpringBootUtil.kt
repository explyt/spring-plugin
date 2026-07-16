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