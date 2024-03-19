package com.esprito.spring.core.service.beans.discoverer

import com.esprito.base.LibraryClassCache
import com.esprito.spring.core.service.PsiBean
import com.intellij.openapi.extensions.ProjectExtensionPointName
import com.intellij.openapi.module.Module

abstract class AdditionalBeansDiscoverer {

    open fun accepts(module: Module): Boolean = true

    abstract fun discoverBeans(module: Module): Collection<PsiBean>

    fun getStaticBean(module: Module, className: String, beanName: String): PsiBean? {
        val psiClass = LibraryClassCache.searchForLibraryClass(module, className) ?: return null

        return PsiBean(
            name = beanName,
            psiClass = psiClass,
            psiMember = psiClass
        )

    }

    open fun getExtraComponents(module: Module): Collection<PsiBean> = emptyList()

    companion object {
        val EP_NAME = ProjectExtensionPointName<AdditionalBeansDiscoverer>(
            "com.esprito.spring.core.additionalBeansDiscoverer"
        )
    }

}