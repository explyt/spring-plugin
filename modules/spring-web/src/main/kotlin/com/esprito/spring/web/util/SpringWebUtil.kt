package com.esprito.spring.web.util

import com.esprito.base.LibraryClassCache
import com.esprito.spring.web.SpringWebClasses
import com.intellij.openapi.project.Project

object SpringWebUtil {
    fun isSpringWebProject(project: Project): Boolean {
        return LibraryClassCache.searchForLibraryClass(
            project,
            SpringWebClasses.WEB_INITIALIZER
        ) != null
    }

}