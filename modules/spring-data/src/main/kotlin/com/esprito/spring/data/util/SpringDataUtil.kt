package com.esprito.spring.data.util

import com.esprito.base.LibraryClassCache
import com.esprito.spring.data.SpringDataClasses
import com.intellij.openapi.module.Module


object SpringDataUtil {
    fun isSpringDataRestProject(module: Module): Boolean {
        return LibraryClassCache.searchForLibraryClass(module, SpringDataClasses.SPRING_DATA_REST_RESOURCE) != null
    }

    fun isSpringDataProject(module: Module): Boolean {
        return LibraryClassCache.searchForLibraryClass(module, SpringDataClasses.SPRING_RESOURCE) != null
    }

}