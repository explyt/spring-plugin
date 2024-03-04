package com.esprito.spring.core.util

import com.esprito.spring.core.util.SpringCoreUtil.SPRING_BOOT_MAVEN
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