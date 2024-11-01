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

package com.explyt.spring.core.util

import com.explyt.spring.core.util.SpringCoreUtil.SPRING_BOOT_MAVEN
import com.intellij.java.library.JavaLibraryUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.ArrayUtil
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.text.VersionComparatorUtil

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

    enum class SpringBootVersion(val version: String) {
        ANY("1.0.0"),

        VERSION_3_0_0("3.0.0");
    }

}