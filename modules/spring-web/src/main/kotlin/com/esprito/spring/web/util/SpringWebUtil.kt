package com.esprito.spring.web.util

import com.esprito.base.LibraryClassCache
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.web.SpringWebClasses
import com.esprito.util.EspritoPsiUtil.inClassAnnotatedBy
import com.esprito.util.EspritoPsiUtil.isNonStatic
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod

object SpringWebUtil {
    fun isSpringWebProject(project: Project): Boolean {
        return LibraryClassCache.searchForLibraryClass(
            project,
            SpringWebClasses.WEB_INITIALIZER
        ) != null
    }

}