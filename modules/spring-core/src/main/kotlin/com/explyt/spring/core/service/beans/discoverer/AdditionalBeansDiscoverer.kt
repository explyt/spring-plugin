package com.explyt.spring.core.service.beans.discoverer

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.RootDataHolder
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

    open fun additionalFilterBeans(module: Module, bean: PsiBean, rootDataHolder: RootDataHolder): Boolean {
        if (rootDataHolder.isEmpty()) return true
        val packages = rootDataHolder.getPackages(module)
        if (packages.isEmpty()) return false
        val qualifiedName = bean.psiClass.qualifiedName ?: return false
        return packages.any { qualifiedName.startsWith(it) }
    }

    companion object {
        val EP_NAME = ProjectExtensionPointName<AdditionalBeansDiscoverer>(
            "com.explyt.spring.core.additionalBeansDiscoverer"
        )
    }

}